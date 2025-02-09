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
import java.util.Objects;
import java.util.TreeMap;

/**
 * A "Standard" Elevator car that accepts additional service requests as long as they are for the same direction and
 * haven't been passed by yet.
 */
public class StandardCar extends QueueCheckingDelayableCar {

    private static final Logger log = LoggerFactory.getLogger(StandardCar.class);

    private final String carId;

    private Direction direction;

    private TreeMap<Integer, StopReasonContainer> stops;

    public StandardCar(
            ThreadPoolTaskScheduler taskScheduler,
            String carId
    ) {
        super(taskScheduler);
        this.carId = carId;
    }

    @Override
    public String getCarId() {
        return carId;
    }

    @Override
    public boolean processServiceRequest(@NonNull ServiceRequestEvent serviceRequestEvent) {

        if (state == State.AVAILABLE) {
            log.info("car: {} accepting: {}", getCarId(), serviceRequestEvent);
            direction = serviceRequestEvent.direction();
            final Comparator<Integer> comparator = direction == Direction.ASCENDING ? Comparator.naturalOrder() : Comparator.reverseOrder();
            stops = new TreeMap<>(comparator);
            stops.put(serviceRequestEvent.originationFloor(), new StopReasonContainer(serviceRequestEvent));
            moveTo(stops.firstKey());
            return true;
        }

        if (state == State.MOVING || state == State.WAITING) {
            final var isOnTheWay = direction == Direction.DESCENDING ?
                    currentFloor > serviceRequestEvent.originationFloor() :
                    currentFloor < serviceRequestEvent.originationFloor();
            if (direction == serviceRequestEvent.direction() && isOnTheWay) {
                log.info("car: {} accepting: {} even though moving", getCarId(), serviceRequestEvent);
                stops.put(serviceRequestEvent.originationFloor(), new StopReasonContainer(serviceRequestEvent));
                return true;
            }
        }

        log.info("car: {} rejecting: {}", getCarId(), serviceRequestEvent);

        return false;
    }

    @Override
    public boolean processFloorsRequest(@NonNull FloorsRequestEvent floorsRequestEvent) {
        final var potentialServiceRequest = stops.values().stream()
                .map(StopReasonContainer::serviceRequestEvent)
                .filter(Objects::nonNull)
                .filter(sre -> sre.id().equals(floorsRequestEvent.serviceRequestEvent().id()))
                .findFirst();

        if (potentialServiceRequest.isEmpty()) {
            return false;
        }

        stops.remove(potentialServiceRequest.get().originationFloor());

        for (Integer requestedFloor : floorsRequestEvent.requestedFloors()) {
            final var isGoodFloorRequest = direction == Direction.DESCENDING ?
                    requestedFloor < currentFloor :
                    requestedFloor > currentFloor;
            if (isGoodFloorRequest && !stops.containsKey(requestedFloor)) {
                stops.put(requestedFloor, new StopReasonContainer(null));
            }
        }

        if (stops.isEmpty()) {
            changeState(State.AVAILABLE);
        } else {
            moveTo(stops.firstKey());
        }

        return true;
    }

    @Override
    protected void arrived() {
        log.info("car: {} arrived at floor: {}", getCarId(), currentFloor);
        final var currentReason = stops.get(currentFloor);
        if (currentReason.serviceRequestEvent() == null) {
            // just letting people off
            stops.remove(currentFloor);

            if (stops.isEmpty()) {
                changeState(State.AVAILABLE);
                return;
            }

            changeState(State.WAITING);

            taskScheduler.schedule(
                    () -> moveTo(stops.firstKey()),
                    Instant.now().plus(Duration.ofSeconds(3L))
            );
        } else {
            // need to wait for rider input
            cueRider(currentReason.serviceRequestEvent());
        }
    }

    /**
     * A container for holding whether a floor has an associated service request, or is just a destination.
     * @param serviceRequestEvent
     */
    private record StopReasonContainer(ServiceRequestEvent serviceRequestEvent) {

    }

}
