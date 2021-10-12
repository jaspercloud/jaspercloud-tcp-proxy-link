package io.jaspercloud.proxy.agent.config;

import io.jaspercloud.proxy.core.support.agent.client.AgentClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;

@Configuration
public class AppConfig {

    @Bean
    public AgentClient agentClient(@Value("${agent.host}") String host,
                                   @Value("${agent.port}") int port) {
        return new AgentClient(new InetSocketAddress(host, port));
    }
}
