package zone.glueck.elevator.cars;

import org.assertj.core.data.TemporalUnitWithinOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import zone.glueck.elevator.events.CarStateEvent;
import zone.glueck.elevator.events.FloorsRequestEvent;
import zone.glueck.elevator.events.ServiceRequestEvent;
import zone.glueck.elevator.utils.Direction;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static zone.glueck.elevator.cars.EventPublisherCar.State.AVAILABLE;
import static zone.glueck.elevator.cars.EventPublisherCar.State.MOVING;

@ExtendWith(MockitoExtension.class)
class EventPublisherCarTest {

    /**
     * NoOp concrete implementation so we can validate the behavior associated with this abstract class.
     */
    private static class TestEventPublisherCar extends EventPublisherCar {

        private int processRequests;

        public TestEventPublisherCar(ThreadPoolTaskScheduler taskScheduler) {
            super(taskScheduler);
        }

        @Override
        protected void arrived() {

        }

        @Override
        public String getCarId() {
            return "";
        }

        @Override
        public boolean processServiceRequest(ServiceRequestEvent serviceRequestEvent) {
            processRequests++;
            return true;
        }

        @Override
        public boolean processFloorsRequest(FloorsRequestEvent floorsRequestEvent) {
            return false;
        }
    }

    @Mock
    private ThreadPoolTaskScheduler taskScheduler;

    @Mock
    private Supplier<ServiceRequestEvent> serviceRequestEventSupplier;

    @Mock
    private Consumer<CarStateEvent> carStateEventConsumer;

    private EventPublisherCar car;

    @BeforeEach
    void setUp() {
        car = new TestEventPublisherCar(taskScheduler);
        car.setServiceRequestSupplier(serviceRequestEventSupplier);
        car.setCarStateEventConsumer(carStateEventConsumer);
        car.currentFloor = 1;
    }

    @Test
    void testMoveTo() {
        car.moveTo(5);

        assertThat(car.currentFloor).isEqualTo(5);
        assertThat(car.state).isEqualTo(MOVING);

        final var delayCapture = ArgumentCaptor.forClass(Instant.class);
        verify(taskScheduler).schedule(any(), delayCapture.capture());

        final var delay = delayCapture.getValue();
        assertThat(delay).isNotNull();
        assertThat(delay).isCloseTo(Instant.now().plus(Duration.ofSeconds(12L)), new TemporalUnitWithinOffset(1L, ChronoUnit.SECONDS));
    }

    @Test
    void testChangeStateAndRetrieveNextJob() {
        final var serviceRequestEvent = new ServiceRequestEvent(UUID.randomUUID(), Direction.ASCENDING, 3);
        when(serviceRequestEventSupplier.get()).thenReturn(serviceRequestEvent);

        car.changeState(AVAILABLE);

        assertThat(((TestEventPublisherCar) car).processRequests).isEqualTo(1);

        final var carStateCaptor = ArgumentCaptor.forClass(CarStateEvent.class);
        verify(carStateEventConsumer).accept(carStateCaptor.capture());
        final var carState = carStateCaptor.getValue();
        assertThat(carState.status()).isEqualTo(AVAILABLE.name());
    }
}