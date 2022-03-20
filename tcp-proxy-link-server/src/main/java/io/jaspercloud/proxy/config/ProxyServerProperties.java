package io.jaspercloud.proxy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("proxy.server")
public class ProxyServerProperties {

    //agent
    private Integer agentPort;
    private Long agentCheckTime;
    private Long agentTimeout;
    //tunnel
    private Integer tunnelPort;
    //socks5
    private Integer socks5Port;
    private Integer socks5MaxRetry;

    public Integer getAgentPort() {
        return agentPort;
    }

    public void setAgentPort(Integer agentPort) {
        this.agentPort = agentPort;
    }

    public Long getAgentCheckTime() {
        return agentCheckTime;
    }

    public void setAgentCheckTime(Long agentCheckTime) {
        this.agentCheckTime = agentCheckTime;
    }

    public Long getAgentTimeout() {
        return agentTimeout;
    }

    public void setAgentTimeout(Long agentTimeout) {
        this.agentTimeout = agentTimeout;
    }

    public Integer getTunnelPort() {
        return tunnelPort;
    }

    public void setTunnelPort(Integer tunnelPort) {
        this.tunnelPort = tunnelPort;
    }

    public Integer getSocks5Port() {
        return socks5Port;
    }

    public void setSocks5Port(Integer socks5Port) {
        this.socks5Port = socks5Port;
    }

    public Integer getSocks5MaxRetry() {
        return socks5MaxRetry;
    }

    public void setSocks5MaxRetry(Integer socks5MaxRetry) {
        this.socks5MaxRetry = socks5MaxRetry;
    }
}
