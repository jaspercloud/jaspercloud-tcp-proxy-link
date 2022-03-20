package io.jaspercloud.proxy.agent.config;

import io.jaspercloud.proxy.core.support.agent.AgentProperties;
import io.jaspercloud.proxy.core.support.agent.HostAgent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties(AgentProperties.class)
@Configuration
public class AppConfig {

    @Bean
    public HostAgent agentClient(AgentProperties agentProperties) {
        return new HostAgent(agentProperties);
    }
}
