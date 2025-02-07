package zone.glueck.elevator.cars;

import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import zone.glueck.elevator.events.RiderCueEvent;
import zone.glueck.elevator.events.ServiceRequestEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class QueueCheckingDelayableCar implements Car {

    protected final ThreadPoolTaskScheduler taskScheduler;

    protected final Supplier<ServiceRequestEvent> serviceRequestSupplier;

    protected final Supplier<Duration> perFloorDurationDelay;

    protected final Consumer<RiderCueEvent> riderCueEventConsumer;

    protected volatile State state = State.AVAILABLE;

    protected volatile int currentFloor = 0;

    public QueueCheckingDelayableCar(ThreadPoolTaskScheduler taskScheduler, Supplier<ServiceRequestEvent> serviceRequestSupplier, Supplier<Duration> perFloorDurationDelay, Consumer<RiderCueEvent> riderCueEventConsumer) {
        this.taskScheduler = taskScheduler;
        this.serviceRequestSupplier = serviceRequestSupplier;
        this.perFloorDurationDelay = perFloorDurationDelay;
        this.riderCueEventConsumer = riderCueEventConsumer;
    }

    protected enum State {
        AVAILABLE, MOVING, WAITING
    }

    protected void startMoving(int nextFloor) {
        final var numberOfFloors = Math.abs(nextFloor - currentFloor);
        currentFloor = nextFloor;
        changeState(State.MOVING);
        taskScheduler.schedule(
                this::arrived,
                Instant.now().plus(perFloorDurationDelay.get().multipliedBy(numberOfFloors))
        );
    }

    protected abstract void arrived();

    protected void cueRider(ServiceRequestEvent serviceRequestEvent) {
        changeState(State.WAITING);
        riderCueEventConsumer.accept(new RiderCueEvent(serviceRequestEvent, getCarId()));
    }

    protected void changeState(State state) {
        this.state = state;
        if (state == State.AVAILABLE) {
            final ServiceRequestEvent serviceRequestEvent = serviceRequestSupplier.get();
            if (serviceRequestEvent != null) {
                if (!processServiceRequest(serviceRequestEvent)) {
                    throw new IllegalStateException("should be able to process, available");
                }
            }
        }
    }
}
