package io.jaspercloud.proxy.core.support.agent;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("proxy.agent")
public class AgentProperties {

    //connection
    private String serverHost;
    private Integer agentPort;
    private Integer tunnelPort;
    private Long heartTime;
    private Integer connectTimeout;
    //auth
    private String username;
    private String password;

    public String getServerHost() {
        return serverHost;
    }

    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }

    public Integer getAgentPort() {
        return agentPort;
    }

    public void setAgentPort(Integer agentPort) {
        this.agentPort = agentPort;
    }

    public Integer getTunnelPort() {
        return tunnelPort;
    }

    public void setTunnelPort(Integer tunnelPort) {
        this.tunnelPort = tunnelPort;
    }

    public Long getHeartTime() {
        return heartTime;
    }

    public void setHeartTime(Long heartTime) {
        this.heartTime = heartTime;
    }

    public Integer getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Integer connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
