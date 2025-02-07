package zone.glueck.elevator.events;

import jakarta.validation.constraints.Min;
import org.springframework.lang.NonNull;
import zone.glueck.elevator.utils.Direction;

import java.util.UUID;

/**
 * The representation of what happens when you push the elevator button.
 * @param id a unique id assigned to this request
 * @param direction the direction you wish to travel
 * @param originationFloor your current floor (cannot be less than 0)
 */
public record ServiceRequestEvent(@NonNull UUID id, @NonNull Direction direction, @Min(0) int originationFloor){}
