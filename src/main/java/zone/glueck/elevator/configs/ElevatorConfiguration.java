package zone.glueck.elevator.configs;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import zone.glueck.elevator.cars.Car;
import zone.glueck.elevator.cars.ExpressCar;
import zone.glueck.elevator.cars.StandardCar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@EnableAsync
public class ElevatorConfiguration {

    private final AtomicInteger counter = new AtomicInteger(1);

    @Bean
    public Collection<Car> cars(
            UserDefinedElevatorConfiguration configuration,
            @Qualifier("singleThreadedServiceScheduler") ThreadPoolTaskScheduler taskScheduler) {
        final List<Car> cars = new ArrayList<>();
        for (String elevatorType : configuration.getElevators()) {
            if ("StandardCar".equals(elevatorType)) {
                cars.add(new StandardCar(taskScheduler, "Car " + counter.getAndIncrement()));
            } else if ("ExpressCar".equals(elevatorType)) {
                cars.add(new ExpressCar(taskScheduler, "ECar " + counter.getAndIncrement()));
            }
        }
        return cars;
    }

    @Bean(name = "singleThreadedServiceScheduler")
    public ThreadPoolTaskScheduler singleThreadedServiceScheduler() {
        final var threadPool = new ThreadPoolTaskScheduler();
        threadPool.setPoolSize(1);
        threadPool.setThreadNamePrefix("ElevatorService");
        threadPool.initialize();
        return threadPool;
    }

}
