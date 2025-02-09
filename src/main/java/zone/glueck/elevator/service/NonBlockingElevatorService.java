package zone.glueck.elevator.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import zone.glueck.elevator.cars.QueueCheckingDelayableCar;
import zone.glueck.elevator.configs.UserDefinedElevatorConfiguration;
import zone.glueck.elevator.cars.Car;
import zone.glueck.elevator.events.CarStateEvent;
import zone.glueck.elevator.events.FloorsRequestEvent;
import zone.glueck.elevator.events.RiderCueEvent;
import zone.glueck.elevator.events.ServiceRequestEvent;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class NonBlockingElevatorService implements ElevatorService {

    private static final Logger log = LoggerFactory.getLogger(NonBlockingElevatorService.class);

    private final UserDefinedElevatorConfiguration configuration;

    private final Collection<RiderCueListener> riderCueListeners = new CopyOnWriteArrayList<>();

    private final Collection<CarStateListener> carStateListeners = new CopyOnWriteArrayList<>();

    private final Queue<ServiceRequestEvent> pendingServiceRequests = new ConcurrentLinkedQueue<>();

    private final Collection<Car> cars = new ArrayList<>();

    public NonBlockingElevatorService(
            UserDefinedElevatorConfiguration configuration,
            Collection<Car> cars
    ) {
        this.configuration = configuration;
        this.cars.addAll(cars);

        this.cars.forEach(car -> {
            if (car instanceof QueueCheckingDelayableCar queuedCar) {
                queuedCar.setServiceRequestSupplier(pendingServiceRequests::poll);
                queuedCar.setRiderCueEventConsumer(this::processRiderCue);
                queuedCar.setCarStateEventConsumer(this::processCarState);
            }
        });
    }

    @Override
    public int getNumberOfFloors() {
        return configuration.getNumberOfFloors();
    }

    @Override
    public List<String> getElevatorNames() {
        return cars.stream().map(Car::getCarId).toList();
    }

    @Override
    @Async(value = "singleThreadedServiceScheduler")
    public void processServiceRequest(@NonNull ServiceRequestEvent serviceRequestEvent) {
        log.info("thread: {}", Thread.currentThread().getName());
        for (Car car : cars) {
            if (car.processServiceRequest(serviceRequestEvent)) {
                return;
            }
        }
        pendingServiceRequests.add(serviceRequestEvent);
    }

    @Override
    @Async(value = "singleThreadedServiceScheduler")
    public void processFloorsRequest(@NonNull FloorsRequestEvent floorsRequestEvent) {
        for (Car car : cars) {
            if (car.processFloorsRequest(floorsRequestEvent)) {
                return;
            }
        }
        throw new IllegalStateException("floors request without matching service request");
    }

    @Override
    public void addRiderCueListener(@NonNull RiderCueListener riderCueListener) {
        riderCueListeners.add(riderCueListener);
    }

    @Override
    public void addCarStateListener(CarStateListener carStateListener) {
        carStateListeners.add(carStateListener);
    }

    private void processRiderCue(@NonNull RiderCueEvent riderCueEvent) {
        riderCueListeners.forEach(listener -> listener.handleRiderCue(riderCueEvent));
    }

    private void processCarState(@NonNull CarStateEvent carStateEvent) {
        carStateListeners.forEach(listener -> listener.handleCarState(carStateEvent));
    }

    @Override
    public UserDefinedElevatorConfiguration getConfiguration() {
        return null;
    }
}
