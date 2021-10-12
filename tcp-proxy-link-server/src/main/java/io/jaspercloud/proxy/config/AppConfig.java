package io.jaspercloud.proxy.config;

import io.jaspercloud.proxy.support.agent.server.AgentManager;
import io.jaspercloud.proxy.support.agent.server.AgentProcessHandler;
import io.jaspercloud.proxy.support.agent.server.AgentServer;
import io.jaspercloud.proxy.support.proxy.ProxyProcessHandler;
import io.jaspercloud.proxy.support.proxy.ProxyServer;
import io.jaspercloud.proxy.support.socks5.Socks5CommandRequestHandler;
import io.jaspercloud.proxy.support.socks5.Socks5PasswordAuthRequestHandler;
import io.jaspercloud.proxy.support.socks5.Socks5Server;
import io.jaspercloud.proxy.support.tunnel.TunnelManager;
import io.jaspercloud.proxy.support.tunnel.TunnelProcessHandler;
import io.jaspercloud.proxy.support.tunnel.TunnelServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public AgentServer agentServer(@Value("${agent.port}") int port) {
        return new AgentServer(port);
    }

    @Bean
    public AgentProcessHandler agentProcessHandler() {
        return new AgentProcessHandler();
    }

    @Bean
    public TunnelServer tunnelServer(@Value("${tunnel.port}") int port) {
        return new TunnelServer(port);
    }

    @Bean
    public TunnelProcessHandler tunnelProcessHandler() {
        return new TunnelProcessHandler();
    }

    @Bean
    public ProxyProcessHandler proxyProcessHandler() {
        return new ProxyProcessHandler(false);
    }

    @Bean
    public ProxyServer proxyServer(ProxyProcessHandler proxyProcessHandler, @Value("${proxy.port}") int port) {
        return new ProxyServer(proxyProcessHandler, port, false);
    }

    @Bean
    public ProxyProcessHandler haProxyProcessHandler() {
        return new ProxyProcessHandler(true);
    }

    @Bean
    public ProxyServer haProxyServer(ProxyProcessHandler haProxyProcessHandler, @Value("${proxy.haproxy.port}") int port) {
        return new ProxyServer(haProxyProcessHandler, port, true);
    }

    @Bean
    public Socks5Server socks5Server(@Value("${socks5.port}") int port) {
        return new Socks5Server(port);
    }

    @Bean
    public Socks5PasswordAuthRequestHandler socks5PasswordAuthRequestHandler() {
        return new Socks5PasswordAuthRequestHandler();
    }

    @Bean
    public Socks5CommandRequestHandler socks5CommandRequestHandler() {
        return new Socks5CommandRequestHandler();
    }

    @Bean
    public AgentManager agentManager() {
        return new AgentManager();
    }

    @Bean
    public TunnelManager tunnelManager() {
        return new TunnelManager();
    }
}
