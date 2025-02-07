package zone.glueck.elevator.api.models;

import zone.glueck.elevator.utils.Direction;

import java.util.UUID;

public class RiderServiceRequest {

    private UUID id;

    private Direction direction;

    private int originationFloor;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public int getOriginationFloor() {
        return originationFloor;
    }

    public void setOriginationFloor(int originationFloor) {
        this.originationFloor = originationFloor;
    }
}
