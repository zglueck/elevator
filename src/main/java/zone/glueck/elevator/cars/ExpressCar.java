package zone.glueck.elevator.cars;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import zone.glueck.elevator.events.FloorsRequestEvent;
import zone.glueck.elevator.events.ServiceRequestEvent;
import zone.glueck.elevator.utils.Direction;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.TreeSet;

public class ExpressCar extends EventPublisherCar {

    private static final Logger log = LoggerFactory.getLogger(ExpressCar.class);

    private final String carId;

    private ServiceRequestEvent serviceRequestEvent;

    private TreeSet<Integer> stops;

    public ExpressCar(ThreadPoolTaskScheduler taskScheduler, String carId) {
        super(taskScheduler);
        this.carId = carId;
    }

    @Override
    protected void arrived() {
        log.info("car: {} arrived at floor: {}", getCarId(), currentFloor);
        stops.remove(currentFloor);
        if (currentFloor != serviceRequestEvent.originationFloor()) {
            // just letting people off
            stops.remove(currentFloor);

            if (stops.isEmpty()) {
                serviceRequestEvent = null;
                changeState(State.AVAILABLE);
                return;
            }

            changeState(State.WAITING);

            taskScheduler.schedule(
                    () -> moveTo(stops.first()),
                    Instant.now().plus(Duration.ofSeconds(3L))
            );
        } else {
            // need to wait for rider input
            cueRider(serviceRequestEvent);
        }
    }

    @Override
    public String getCarId() {
        return carId;
    }

    @Override
    public boolean processServiceRequest(@NonNull ServiceRequestEvent serviceRequestEvent) {
        if (this.serviceRequestEvent != null) {
            return false;
        }

        log.info("car: {} accepting: {}", getCarId(), serviceRequestEvent);
        final Comparator<Integer> comparator = serviceRequestEvent.direction() == Direction.ASCENDING ? Comparator.naturalOrder() : Comparator.reverseOrder();
        stops = new TreeSet<>(comparator);
        this.serviceRequestEvent = serviceRequestEvent;
        stops.add(serviceRequestEvent.originationFloor());
        moveTo(stops.first());
        return true;
    }

    @Override
    public boolean processFloorsRequest(@NonNull FloorsRequestEvent floorsRequestEvent) {
        if (
                this.serviceRequestEvent != null &&
                !this.serviceRequestEvent.id().equals(floorsRequestEvent.serviceRequestEvent().id())
        ) {
            return false;
        }

        stops.addAll(floorsRequestEvent.requestedFloors());
        moveTo(stops.first());
        return true;
    }
}
