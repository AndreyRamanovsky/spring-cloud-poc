package common.validation.playground.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties("test-properties")
public class TestCoreProperties {

    private String service;
    private String serviceExtension;
    private String environment;
    private String shared;
    private String game;

//
//    service: "Core service properties. Should remain the same all the time"
//    service-extension: "Core service properties. Should be overridden by the extension properties"
//    environment: "Core service properties. Determined by the env"
//    shared: "Core service properties. Should be overridden by the shared properties"
//    game: "Core service properties. Should be overridden by the application-game properties"
}
