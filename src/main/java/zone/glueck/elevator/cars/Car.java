package zone.glueck.elevator.cars;

import org.springframework.lang.NonNull;
import zone.glueck.elevator.events.FloorsRequestEvent;
import zone.glueck.elevator.events.ServiceRequestEvent;

/**
 * An elevator car that accepts service and floor requests.
 */
public interface Car {

    String getCarId();

    /**
     * A submission method for {@link ServiceRequestEvent}s to be processed by this car. Submitted service requests may
     * be processed OR rejected. The state is indicated by the return value.
     * @param serviceRequestEvent
     * @return {@code true} if the car is in a state to process the event
     */
    boolean processServiceRequest(@NonNull ServiceRequestEvent serviceRequestEvent);

    /**
     * A submission method for {@link FloorsRequestEvent}s to be processed by this car. The submitted
     * {@link FloorsRequestEvent#serviceRequestEvent()} ID must match this cars current {@link ServiceRequestEvent} or
     * else the request will be rejected and indicated by a {@code} false response.
     * @param floorsRequestEvent
     * @return {@code true} if the car may process the floors request
     */
    boolean processFloorsRequest(@NonNull FloorsRequestEvent floorsRequestEvent);

}
