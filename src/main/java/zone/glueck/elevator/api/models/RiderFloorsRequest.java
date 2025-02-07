package zone.glueck.elevator.api.models;

import zone.glueck.elevator.events.ServiceRequestEvent;

import java.util.Set;

public class RiderFloorsRequest {

    private RiderServiceRequest riderServiceRequest;

    private Set<Integer> requestedFloors;

    public RiderServiceRequest getRiderServiceRequest() {
        return riderServiceRequest;
    }

    public void setRiderServiceRequest(RiderServiceRequest riderServiceRequest) {
        this.riderServiceRequest = riderServiceRequest;
    }

    public Set<Integer> getRequestedFloors() {
        return requestedFloors;
    }

    public void setRequestedFloors(Set<Integer> requestedFloors) {
        this.requestedFloors = requestedFloors;
    }
}
