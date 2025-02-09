package zone.glueck.elevator.events;

import org.springframework.lang.NonNull;

public record CarStateEvent(@NonNull String carName, @NonNull String status, int currentFloor) {
}
