package io.jaspercloud.proxy.config;

import io.jaspercloud.proxy.support.agent.server.AgentManager;
import io.jaspercloud.proxy.support.agent.server.AgentProcessHandler;
import io.jaspercloud.proxy.support.agent.server.AgentServer;
import io.jaspercloud.proxy.support.socks5.Socks5Control;
import io.jaspercloud.proxy.support.socks5.Socks5Server;
import io.jaspercloud.proxy.support.socks5.Socks5ServerHandler;
import io.jaspercloud.proxy.support.tunnel.TunnelManager;
import io.jaspercloud.proxy.support.tunnel.TunnelProcessHandler;
import io.jaspercloud.proxy.support.tunnel.TunnelServer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties(ProxyServerProperties.class)
@Configuration
public class AppConfig {

    @Configuration
    public static class AgentConfig {

        @Bean
        public AgentServer agentServer(ProxyServerProperties serverProperties) {
            return new AgentServer(serverProperties);
        }

        @Bean
        public AgentProcessHandler agentProcessHandler() {
            return new AgentProcessHandler();
        }

        @Bean
        public AgentManager agentManager() {
            return new AgentManager();
        }
    }

    @Configuration
    public static class TunnelConfig {

        @Bean
        public TunnelServer tunnelServer(ProxyServerProperties serverProperties) {
            return new TunnelServer(serverProperties);
        }

        @Bean
        public TunnelProcessHandler tunnelProcessHandler() {
            return new TunnelProcessHandler();
        }

        @Bean
        public TunnelManager tunnelManager() {
            return new TunnelManager();
        }
    }

    @Configuration
    public static class SocksConfig {

        @Bean
        public Socks5Server socks5Server(ProxyServerProperties serverProperties) {
            return new Socks5Server(serverProperties);
        }

        @Bean
        public Socks5ServerHandler socks5ServerHandler() {
            return new Socks5ServerHandler();
        }

        @Bean
        public Socks5Control socks5Control() {
            return new Socks5Control();
        }
    }
}
