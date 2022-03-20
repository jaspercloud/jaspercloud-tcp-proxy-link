package io.jaspercloud.proxy.client.config;

import io.jaspercloud.proxy.client.support.LocalServer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties(ClientProperties.class)
@Configuration
public class AppConfig {

    @Bean
    public LocalServer localServer(ClientProperties clientProperties) {
        return new LocalServer(clientProperties);
    }
}
