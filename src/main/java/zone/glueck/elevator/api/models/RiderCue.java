package zone.glueck.elevator.api.models;

public class RiderCue {

    private RiderServiceRequest serviceRequest;

    private String carName;

    public RiderServiceRequest getServiceRequest() {
        return serviceRequest;
    }

    public void setServiceRequest(RiderServiceRequest serviceRequest) {
        this.serviceRequest = serviceRequest;
    }

    public String getCarName() {
        return carName;
    }

    public void setCarName(String carName) {
        this.carName = carName;
    }
}
