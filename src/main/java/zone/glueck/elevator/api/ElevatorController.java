package zone.glueck.elevator.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import zone.glueck.elevator.api.models.Configuration;
import zone.glueck.elevator.api.models.RiderCue;
import zone.glueck.elevator.api.models.RiderFloorsRequest;
import zone.glueck.elevator.api.models.RiderServiceRequest;
import zone.glueck.elevator.events.FloorsRequestEvent;
import zone.glueck.elevator.events.RiderCueEvent;
import zone.glueck.elevator.events.ServiceRequestEvent;
import zone.glueck.elevator.service.ElevatorService;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class ElevatorController {

    private static final Logger log = LoggerFactory.getLogger(ElevatorController.class);

    private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    private final ElevatorService elevatorService;

    public ElevatorController(ElevatorService elevatorService) {
        this.elevatorService = elevatorService;
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

    @GetMapping("/service/{serviceId}/events")
    public SseEmitter registerServiceListener(@PathVariable("serviceId") UUID id) {
        final var emitter = new SseEmitter();
        emitters.put(id, emitter);
        elevatorService.addRiderCueListener(riderCueEvent -> {
            if (id.equals(riderCueEvent.serviceRequestEvent().id())) {
                final var riderCue = toModel(riderCueEvent);

                final var existingEmitter = emitters.remove(id);
                if (existingEmitter == null) {
                    log.error("failed to find emitter for: {}", id);
                    return false;
                }

                try {
                    existingEmitter.send(riderCue, MediaType.APPLICATION_JSON);
                    existingEmitter.complete();
                } catch (IOException ex) {
                    log.error("failed to send cue event: {}", riderCueEvent, ex);
                }
                return true;
            }
            return false;
        });
        return emitter;
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
        riderCue.setCarName("");
        return riderCue;
    }

}
