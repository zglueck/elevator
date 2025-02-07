package zone.glueck.elevator.service;

import org.springframework.lang.NonNull;
import zone.glueck.elevator.events.RiderCueEvent;

public interface RiderCueListener {

    boolean handleRiderCue(@NonNull RiderCueEvent riderCueEvent);

}
