package zone.glueck.elevator.cars;

import org.springframework.lang.Nullable;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import zone.glueck.elevator.events.RiderCueEvent;
import zone.glueck.elevator.events.ServiceRequestEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class QueueCheckingDelayableCar implements Car {

    protected static final Duration PER_FLOOR_MOVE_DURATION = Duration.ofSeconds(3L);

    protected final ThreadPoolTaskScheduler taskScheduler;

    @Nullable
    protected Supplier<ServiceRequestEvent> serviceRequestSupplier;

    @Nullable
    protected Consumer<RiderCueEvent> riderCueEventConsumer;

    protected State state = State.AVAILABLE;

    protected int currentFloor = 0;

    public QueueCheckingDelayableCar(ThreadPoolTaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }

    public enum State {
        AVAILABLE, MOVING, WAITING
    }

    /**
     * This method is invoked when the car has arrived at the next destination (as indicated by {@link #currentFloor}.
     */
    protected abstract void arrived();

    /**
     * "Moves" the elevator by scheduling an arrival and modifying the state and current floor
     * @param nextFloor
     */
    protected void moveTo(int nextFloor) {
        final var numberOfFloors = Math.abs(nextFloor - currentFloor);
        currentFloor = nextFloor;
        changeState(State.MOVING);
        taskScheduler.schedule(
                this::arrived,
                Instant.now().plus(PER_FLOOR_MOVE_DURATION.multipliedBy(numberOfFloors))
        );
    }

    /**
     * Invoke when the car should be in a state of waiting for a rider to provide the requested floors. Changes the
     * state and publishes a {@link RiderCueEvent} if a consumer is configured.
     * @param serviceRequestEvent
     */
    protected void cueRider(ServiceRequestEvent serviceRequestEvent) {
        changeState(State.WAITING);
        if (riderCueEventConsumer != null) {
            riderCueEventConsumer.accept(new RiderCueEvent(serviceRequestEvent, getCarId()));
        }
    }

    /**
     * Changes the elevator state and checks for certain state change conditions. In the event the elevator transitions
     * to {@link State#AVAILABLE}, will check if there is queued service request, if a request supplier is configured.
     * @param state
     */
    protected void changeState(State state) {
        this.state = state;
        if (state == State.AVAILABLE && serviceRequestSupplier != null) {
            final ServiceRequestEvent serviceRequestEvent = serviceRequestSupplier.get();
            if (serviceRequestEvent != null) {
                if (!processServiceRequest(serviceRequestEvent)) {
                    throw new IllegalStateException("should be able to process, available");
                }
            }
        }
    }

    @Nullable
    public Supplier<ServiceRequestEvent> getServiceRequestSupplier() {
        return serviceRequestSupplier;
    }

    public void setServiceRequestSupplier(@Nullable Supplier<ServiceRequestEvent> serviceRequestSupplier) {
        this.serviceRequestSupplier = serviceRequestSupplier;
    }

    @Nullable
    public Consumer<RiderCueEvent> getRiderCueEventConsumer() {
        return riderCueEventConsumer;
    }

    public void setRiderCueEventConsumer(@Nullable Consumer<RiderCueEvent> riderCueEventConsumer) {
        this.riderCueEventConsumer = riderCueEventConsumer;
    }

    public State getState() {
        return state;
    }

    public int getCurrentFloor() {
        return currentFloor;
    }
}
