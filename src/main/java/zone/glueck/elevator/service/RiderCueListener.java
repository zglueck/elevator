package zone.glueck.elevator.service;

import org.springframework.lang.NonNull;
import zone.glueck.elevator.events.RiderCueEvent;

public interface RiderCueListener {

    void handleRiderCue(@NonNull RiderCueEvent riderCueEvent);

}
