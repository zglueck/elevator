package zone.glueck.elevator.api;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import zone.glueck.elevator.api.models.*;
import zone.glueck.elevator.events.CarStateEvent;
import zone.glueck.elevator.events.FloorsRequestEvent;
import zone.glueck.elevator.events.RiderCueEvent;
import zone.glueck.elevator.events.ServiceRequestEvent;
import zone.glueck.elevator.service.ElevatorService;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

@RestController
public class ElevatorController {

    private static final Logger log = LoggerFactory.getLogger(ElevatorController.class);

    private final ElevatorService elevatorService;

    private final Set<SseEmitter> sseEmitters;

    public ElevatorController(ElevatorService elevatorService) throws IOException {
        this.elevatorService = elevatorService;
        this.sseEmitters = new CopyOnWriteArraySet<>();

        initPushNotification();
    }

    private void initPushNotification() {
        elevatorService.addRiderCueListener(riderCueEvent -> {
            log.info("publishing rider cue event: {}", riderCueEvent);
            final var riderCue = toModel(riderCueEvent);
            sseEmitters.forEach(sseEmitter -> {
                try {
                    sseEmitter.send(riderCue, MediaType.APPLICATION_JSON);
                } catch (IOException ex) {
                    log.error("failed to send cue event: {}", riderCueEvent, ex);
                }
            });
        });
        elevatorService.addCarStateListener(carStateEvent -> {
            log.info("publishing elevator state event: {}", carStateEvent);
            final var carState = toModel(carStateEvent);
            sseEmitters.forEach(sseEmitter -> {
                try {
                    sseEmitter.send(carState, MediaType.APPLICATION_JSON);
                } catch (IOException ex) {
                    log.error("failed to send car event: {}", carStateEvent, ex);
                }
            });
        });
    }

    @PreDestroy
    public void destroy() {
        sseEmitters.forEach(ResponseBodyEmitter::complete);
    }

    @GetMapping("/configuration")
    public Configuration getConfiguration() {
        return new Configuration(
                elevatorService.getNumberOfFloors(),
                elevatorService.getElevatorNames()
        );
    }

    @PostMapping("/service")
    public RiderServiceRequest createServiceRequest(@RequestBody RiderServiceRequest riderServiceRequest) {
        riderServiceRequest.setId(UUID.randomUUID());

        log.info("thread: {}", Thread.currentThread().getName());

        final var serviceRequestEvent = toEvent(riderServiceRequest);
        elevatorService.processServiceRequest(serviceRequestEvent);

        return riderServiceRequest;
    }

    @PostMapping("/service/{serviceId}/floors")
    public RiderFloorsRequest createFloorsRequest(@PathVariable("serviceId") UUID id, @RequestBody RiderFloorsRequest riderFloorsRequest) {
        if (!id.equals(riderFloorsRequest.getRiderServiceRequest().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "service request id mismatch");
        }

        final var floorsRequestEvent = toEvent(riderFloorsRequest);
        elevatorService.processFloorsRequest(floorsRequestEvent);

        return riderFloorsRequest;
    }

    @GetMapping("/service/events")
    public SseEmitter registerServiceListener() {
        final var sseEmitter = new SseEmitter(-1L);
        sseEmitters.add(sseEmitter);
        return sseEmitter;
    }

    private ServiceRequestEvent toEvent(RiderServiceRequest riderServiceRequest) {
        return new ServiceRequestEvent(
                riderServiceRequest.getId(),
                riderServiceRequest.getDirection(),
                riderServiceRequest.getOriginationFloor()
        );
    }

    private FloorsRequestEvent toEvent(RiderFloorsRequest riderFloorsRequest) {
        final var serviceRequestEvent = toEvent(riderFloorsRequest.getRiderServiceRequest());
        return new FloorsRequestEvent(
                serviceRequestEvent,
                riderFloorsRequest.getRequestedFloors()
        );
    }

    private RiderServiceRequest toModel(ServiceRequestEvent serviceRequestEvent) {
        final var riderServiceRequest = new RiderServiceRequest();
        riderServiceRequest.setId(serviceRequestEvent.id());
        riderServiceRequest.setDirection(serviceRequestEvent.direction());
        riderServiceRequest.setOriginationFloor(serviceRequestEvent.originationFloor());
        return riderServiceRequest;
    }

    private RiderCue toModel(RiderCueEvent riderCueEvent) {
        final var riderCue = new RiderCue();
        riderCue.setServiceRequest(toModel(riderCueEvent.serviceRequestEvent()));
        riderCue.setCarName(riderCueEvent.carId());
        return riderCue;
    }

    private CarState toModel(CarStateEvent carStateEvent) {
        final var carState = new CarState();
        carState.setCarName(carStateEvent.carName());
        carState.setStatus(carStateEvent.status());
        carState.setCurrentFloor(carStateEvent.currentFloor());
        return carState;
    }

}
