package zone.glueck.elevator.service;

import org.springframework.lang.NonNull;
import zone.glueck.elevator.events.FloorsRequestEvent;
import zone.glueck.elevator.events.RiderCueEvent;
import zone.glueck.elevator.events.ServiceRequestEvent;

import java.util.List;

public interface ElevatorService {

    int getNumberOfFloors();

    List<String> getElevatorNames();

    void processServiceRequest(@NonNull ServiceRequestEvent serviceRequestEvent);

    void processFloorsRequest(@NonNull FloorsRequestEvent floorsRequestEvent);

    void addRiderCueListener(@NonNull RiderCueListener riderCueListener);

    void processRiderCue(@NonNull RiderCueEvent riderCueEvent);

    ElevatorServiceConfiguration getConfiguration();

}
