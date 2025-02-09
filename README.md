## Elevator Simulator

A simple **and not safety approved** elevator simulator with an interactive UI.

### Dependencies

- Java 23
- Maven
- Browser

### Getting Started

1. `mvn spring-boot:run`
2. Open Browser
3. Navigate to [localhost](http://localhost:8080)

### Configuration

Specify the number of floors and the elevator types (currently only standard and express) in the `application.yaml`:

```yaml
elevator:
  number-of-floors: 4
  elevators:
    - StandardCar
    - StandardCar
    - StandardCar
    - ExpressCar
```

### Design/Architecture

This overly complex code but _simple_ simulator was a fun challenge to make the service orchestration purely event
driven and executable on a single thread. A REST API exists for rider inputs, but the service includes a registration 
endpoint for Server Sent Events to receive both service requested cues for the riders and elevator car state updates.

The operation should be similar to real-world elevators.

1. Rider on a floor requests service (ServiceRequestEvent)
2. Elevator Service selects an acceptable elevator car and dispatches to the floor.
3. Car reaches the floor and Cues the Rider for the destination floors.
4. Car goes on it's merry way.

ServiceRequestEvent's that cannot be scheduled to a specific car go into a queue that will automatically be checked once
a car becomes available.

sequenceDiagram
Web UI->>Controller: What is the elevator configuration?
Controller->>Web UI: Number of floors and elevators
Web UI->>Controller: Register for Rider and Car Events
Web UI->>Controller: Service Requested to floor two
Controller->>Web UI: Service Request Information
Note right of Controller: Asynchronously hand of service information and API returns immediately
Controller->>Elevator Service: Car Requested to floor two
Elevator Service-->Controller: Car State Update: Car Moving
Controller-->Web UI: Car Moving
Elevator Service-->Controller: Car Arrived, Need Rider Input
Controller-->Web UI: Cue Rider Input

### Notes

- This was a fun challenge and could be solved a number of ways. I wanted to see if I could incorporate temporal delays for realism while still processing on a single thread.
- There are little, to no safety/realism checks. I'm sure you can request a floor that doesn't exist.

### Assumptions

1. Floor requests are only honored when the car responds to the origination floor of a service request. If a rider happens to jump on without pushing a request button, they do not get to select a floor.
2. The `StandardCar` will attempt to honor additional service requests as long as they match the direction of the initial service request and the origination floor has not been passed by.
3. The `ExpressCar` honors only one service request at a time and will travel to all of the floor requests before accepting a new service request.
4. The cars will wait *forever* for a user to put in a floor request, probably a bad idea.

### Future Features

- Round Robin or Nearest Elevator Car Selection - currently the list is traversed from first to last, so inevitably the first car gets more requests.
- Testing
- Better Status Reporting
- App Based Service Requests (set a time and floor for an elevator to be available!)
