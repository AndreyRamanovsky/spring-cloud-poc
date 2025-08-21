package common.validation.playground;

import common.validation.playground.properties.TestCoreProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class PlaygroundApplication {

    @Autowired
    TestCoreProperties testCoreProperties;

    public static void main(String[] args) {
        SpringApplication.run(PlaygroundApplication.class, args);
    }

    @PostConstruct
    public void post() {
        log.error("Application started!!!!");
        log.error("testCoreProperties=[{}]", testCoreProperties);
    }
}
