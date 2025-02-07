package zone.glueck.elevator.events;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.lang.NonNull;

import java.util.Set;

/**
 * A representation of the final state of the elevator button within the elevator.
 * @param serviceRequestEvent the {@link ServiceRequestEvent} that summoned the elevator for this floor request to be made
 * @param requestedFloors the floors that were selected
 */
public record FloorsRequestEvent(@NonNull ServiceRequestEvent serviceRequestEvent, @NotEmpty Set<Integer> requestedFloors) {}
