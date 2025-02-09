package zone.glueck.elevator.service;

import org.springframework.lang.NonNull;
import zone.glueck.elevator.events.CarStateEvent;

public interface CarStateListener {

    void handleCarState(@NonNull CarStateEvent carStateEvent);

}
