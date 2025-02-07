package zone.glueck.elevator.service;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.List;

@Configuration
@EnableAsync
@ConfigurationProperties(prefix = "elevator")
public class ElevatorServiceConfiguration {

    @Min(0)
    private int numberOfFloors;

    @NotEmpty
    private List<String> elevators;

    public int getNumberOfFloors() {
        return numberOfFloors;
    }

    public void setNumberOfFloors(int numberOfFloors) {
        this.numberOfFloors = numberOfFloors;
    }

    public List<String> getElevators() {
        return elevators;
    }

    public void setElevators(List<String> elevators) {
        this.elevators = elevators;
    }
}
