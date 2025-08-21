package common.validation.configserverplaygroung.service;

import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.cloud.config.server.environment.JGitEnvironmentProperties;
import org.springframework.cloud.config.server.environment.JGitEnvironmentRepository;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CustomRoutingEnvironmentRepository implements EnvironmentRepository, Ordered {

    // Default extension Url
    @Value("${SPRING_CLOUD_CONFIG_SERVER_GIT_URI:https://github.com/AndreyRamanovsky/game-specific-properties}")
    private String extensionUri;

    @Value("${SPRING_CLOUD_CONFIG_SERVER_PHOENIX_CORE_GIT_URI:https://github.com/AndreyRamanovsky/core-properties}")
    private String phoenixCoreUri;

    @Value("${SPRING_CLOUD_CONFIG_SERVER_GIT_USERNAME:}")
    private String extensionGitUsername;

    @Value("${SPRING_CLOUD_CONFIG_SERVER_GIT_PASSWORD:}")
    private String extensionGitPassword;

    @Value("${SPRING_CLOUD_CONFIG_SERVER_PHOENIX_CORE_GIT_USERNAME:}")
    private String phoenixCoreGitUsername;

    @Value("${SPRING_CLOUD_CONFIG_SERVER_PHOENIX_CORE_GIT_PASSWORD:}")
    private String phoenixCoreGitPassword;

    private static final String[] CLOUD_CONFIG_PATHS = {
            "shared",
            "shared/environments",
            "shared/static",
            "services/platforms/*/{application}",
            "services/{application}",
            "{application}"
    };

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private static final Logger logger = LoggerFactory.getLogger(CustomRoutingEnvironmentRepository.class);

    private final EnvironmentRepository gameRepo;
    private final ConfigurableEnvironment environment;
    private final ObservationRegistry observationRegistry;

    public CustomRoutingEnvironmentRepository(
            ConfigurableEnvironment environment,
            ObservationRegistry observationRegistry) {
        this.environment = environment;
        this.observationRegistry = observationRegistry;
        this.gameRepo = createGameRepository();
    }

    @Override
    public Environment findOne(String application, String profile, String label) {
        logger.debug("___Test Repo___");
        logger.debug("app: {}", application);
        logger.debug("profiles: {}", profile);
        logger.debug("label: {}", label);
        try {
            if (label != null && !label.contains(":")) {
                // Phoenix family: use composite (extension + core)
                return gameRepo.findOne(application, profile, label);
            } else if (label != null && label.contains(":")) {
                // Custom branch format: "extension-branch:core-branch"
                String[] branches = label.split(":");
                // Validate format
                if (branches.length != 2) {
                    throw new IllegalArgumentException(
                            "Invalid label format. Expected 'extension-branch:core-branch', got: " + label);
                }

                String extensionBranch = branches[0].trim();
                String coreBranch = branches[1].trim();

                // Validate branch names are not empty
                if (extensionBranch.isEmpty() || coreBranch.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Branch names cannot be empty. Got: extension='" + extensionBranch +
                                    "', core='" + coreBranch + "'");
                }

                return createCustomComposite(application, profile, extensionBranch, coreBranch);

            } else {
                // Default: game repository only
                return gameRepo.findOne(application, profile, "main");
            }
        } catch (Exception e) {
            // Log error and return empty environment as fallback
            logger.error("Error finding environment for app={}, profile={}, label={}",
                    application, profile, label, e);
            return createCustomComposite(application, profile, "main", "main");
        }
    }


    private EnvironmentRepository createGameRepository() {
        JGitEnvironmentProperties properties = new JGitEnvironmentProperties();
        properties.setUri(extensionUri);
        properties.setSearchPaths(CLOUD_CONFIG_PATHS);
        properties.setDefaultLabel("main");
        properties.setCloneOnStart(true);

        return new JGitEnvironmentRepository(environment, properties, observationRegistry);
    }

    private EnvironmentRepository createCoreRepository() {
        JGitEnvironmentProperties properties = new JGitEnvironmentProperties();
        properties.setUri(phoenixCoreUri);
        properties.setSearchPaths(CLOUD_CONFIG_PATHS);
        properties.setDefaultLabel("main");
        properties.setCloneOnStart(true);

        return new JGitEnvironmentRepository(environment, properties, observationRegistry);
    }

    private Environment createCustomComposite(String app, String profile, String extBranch, String coreBranch) {
        try {
            // Create individual repository instances for specific branches
            EnvironmentRepository extensionRepo = createExtensionRepository(extBranch);
            EnvironmentRepository coreRepo = createCoreRepository(coreBranch);

            // Fetch environments from each repository
            Environment extensionEnv = extensionRepo.findOne(app, profile, extBranch);
            Environment coreEnv = coreRepo.findOne(app, profile, coreBranch);

            // Merge with extension having precedence
            return mergeEnvironments(extensionEnv, coreEnv, app, profile);

        } catch (Exception e) {
            logger.error("Failed to create custom composite for app={}, profile={}, extensionBranch={}, coreBranch={}",
                    app, profile, extBranch, coreBranch, e);

            // Fallback to core repository only
            return createCoreRepository().findOne(app, profile, coreBranch);
        }
    }

    private Environment mergeEnvironments(Environment extensionEnv, Environment coreEnv,
                                          String application, String profile) {
        // Prepare merged property sources in order (first = highest precedence)
        List<PropertySource> mergedSources = new ArrayList<>();

        // Extension properties first (highest precedence)
        if (extensionEnv.getPropertySources() != null) {
            mergedSources.addAll(extensionEnv.getPropertySources());
        }

        // Core properties second (lower precedence)
        if (coreEnv.getPropertySources() != null) {
            mergedSources.addAll(coreEnv.getPropertySources());
        }

        // Create merged environment with property sources
        Environment merged = new Environment(
                application,
                new String[]{profile},
                extensionEnv.getLabel(), // Use extension label as primary
                extensionEnv.getVersion(),
                extensionEnv.getState()
        );

        // Add property sources to the environment
        for (PropertySource propertySource : mergedSources) {
            merged.add(propertySource);
        }

        return merged;
    }

    private EnvironmentRepository createCoreRepository(String branch) {
        return createRepository(phoenixCoreUri, branch);
    }

    private EnvironmentRepository createExtensionRepository(String branch) {
        return createRepository(extensionUri, branch);
    }

    private EnvironmentRepository createRepository(String uri, String branch) {
        JGitEnvironmentProperties properties = new JGitEnvironmentProperties();
        properties.setUri(uri);
        properties.setSearchPaths(CLOUD_CONFIG_PATHS);
        properties.setDefaultLabel(branch);
        properties.setCloneOnStart(true);

        return new JGitEnvironmentRepository(environment, properties, observationRegistry);
    }
}