package common.validation.configserverplayground;

import common.validation.configserverplaygroung.service.CustomRoutingEnvironmentRepository;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.ConfigurableEnvironment;

@SpringBootApplication
@EnableConfigServer
public class ConfigServerPlaygroundApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerPlaygroungApplication.class, args);
    }

}
