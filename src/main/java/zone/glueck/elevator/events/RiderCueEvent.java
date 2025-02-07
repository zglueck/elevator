package zone.glueck.elevator.events;

import org.springframework.lang.NonNull;

public record RiderCueEvent(@NonNull ServiceRequestEvent serviceRequestEvent, @NonNull String carId) {
}
