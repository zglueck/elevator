package zone.glueck.elevator.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import zone.glueck.elevator.cars.Car;
import zone.glueck.elevator.cars.StandardCar;
import zone.glueck.elevator.events.FloorsRequestEvent;
import zone.glueck.elevator.events.RiderCueEvent;
import zone.glueck.elevator.events.ServiceRequestEvent;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Async
public class NonBlockingElevatorService implements ElevatorService {

    private static final Logger log = LoggerFactory.getLogger(NonBlockingElevatorService.class);

    private final ElevatorServiceConfiguration configuration;

    private final Collection<RiderCueListener> listeners = new CopyOnWriteArrayList<>();

    private final Queue<ServiceRequestEvent> pendingServiceRequests = new ConcurrentLinkedQueue<>();

    private final Collection<Car> cars = new ArrayList<>();

    @Autowired
    public NonBlockingElevatorService(ElevatorServiceConfiguration configuration) {
        this.configuration = configuration;

        final var threadPool = new ThreadPoolTaskScheduler();
        threadPool.setPoolSize(1);
        threadPool.setThreadNamePrefix("ElevatorService");
        threadPool.initialize();

        int counter = 1;

        for (String elevatorType : configuration.getElevators()) {
            if ("StandardCar".equals(elevatorType)) {
                cars.add(
                        new StandardCar(
                                threadPool,
                                pendingServiceRequests::poll,
                                () -> Duration.ofSeconds(3L),
                                this::processRiderCue,
                                "Car " + counter++
                        )
                );
            }
        }
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
        listeners.add(riderCueListener);
    }

    @Override
    public void processRiderCue(@NonNull RiderCueEvent riderCueEvent) {
        final var listenersToRemove = new HashSet<RiderCueListener>();
        listeners.forEach(listener -> {
            if (listener.handleRiderCue(riderCueEvent)) {
                listenersToRemove.add(listener);
            }
        });
        listeners.removeAll(listenersToRemove);
    }

    @Override
    public ElevatorServiceConfiguration getConfiguration() {
        return null;
    }
}
