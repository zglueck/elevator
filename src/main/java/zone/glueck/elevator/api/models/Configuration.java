package zone.glueck.elevator.api.models;

import java.util.List;

public class Configuration {

    private int totalFloors;

    private List<String> elevatorNames;

    public Configuration() {
    }

    public Configuration(int totalFloors, List<String> elevatorNames) {
        this.totalFloors = totalFloors;
        this.elevatorNames = elevatorNames;
    }

    public int getTotalFloors() {
        return totalFloors;
    }

    public void setTotalFloors(int totalFloors) {
        this.totalFloors = totalFloors;
    }

    public List<String> getElevatorNames() {
        return elevatorNames;
    }

    public void setElevatorNames(List<String> elevatorNames) {
        this.elevatorNames = elevatorNames;
    }
}
