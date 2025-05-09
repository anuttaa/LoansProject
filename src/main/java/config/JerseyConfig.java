package config;

import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJsonProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

public class JerseyConfig extends ResourceConfig {
    public JerseyConfig() {
        // Отключаем автоматическое сканирование Jackson
        property(ServerProperties.FEATURE_AUTO_DISCOVERY_DISABLE, true);

        packages("server/controller");

        register(JacksonJsonProvider.class);
    }
}
