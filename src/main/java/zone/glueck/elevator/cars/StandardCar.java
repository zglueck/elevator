package zone.glueck.elevator.cars;

import org.springframework.lang.NonNull;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import zone.glueck.elevator.events.FloorsRequestEvent;
import zone.glueck.elevator.events.RiderCueEvent;
import zone.glueck.elevator.events.ServiceRequestEvent;
import zone.glueck.elevator.utils.Direction;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class StandardCar extends QueueCheckingDelayableCar {

    private final String carId;

    private volatile Direction direction;

    private volatile TreeMap<Integer, StopReasonContainer> stops;

    public StandardCar(
            ThreadPoolTaskScheduler taskScheduler,
            Supplier<ServiceRequestEvent> serviceConsumer,
            Supplier<Duration> perFloorDurationDelay,
            Consumer<RiderCueEvent> riderCueEventConsumer,
            String carId
    ) {
        super(taskScheduler, serviceConsumer, perFloorDurationDelay, riderCueEventConsumer);
        this.carId = carId;
    }

    @Override
    public String getCarId() {
        return carId;
    }

    @Override
    public synchronized boolean processServiceRequest(@NonNull ServiceRequestEvent serviceRequestEvent) {

        if (state == State.AVAILABLE) {
            direction = serviceRequestEvent.direction();
            final Comparator<Integer> comparator = direction == Direction.ASCENDING ? Comparator.naturalOrder() : Comparator.reverseOrder();
            stops = new TreeMap<>(comparator);
            stops.put(serviceRequestEvent.originationFloor(), new StopReasonContainer(serviceRequestEvent));
            startMoving(stops.firstKey());
            return true;
        }

        if (state == State.MOVING || state == State.WAITING) {
            final var isOnTheWay = direction == Direction.DESCENDING ?
                    currentFloor >= serviceRequestEvent.originationFloor() :
                    currentFloor <= serviceRequestEvent.originationFloor();
            if (direction == serviceRequestEvent.direction() && isOnTheWay) {
                stops.put(serviceRequestEvent.originationFloor(), new StopReasonContainer(serviceRequestEvent));
                return true;
            }
        }

        return false;
    }

    @Override
    public synchronized boolean processFloorsRequest(@NonNull FloorsRequestEvent floorsRequestEvent) {
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

        startMoving(stops.firstKey());

        return true;
    }

    @Override
    protected synchronized void arrived() {
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
                    () -> startMoving(stops.firstKey()),
                    Instant.now().plus(Duration.ofSeconds(3L))
            );
        } else {
            // need to wait for rider input
            cueRider(currentReason.serviceRequestEvent());
        }
    }

    private record StopReasonContainer(ServiceRequestEvent serviceRequestEvent) {

    }

}
