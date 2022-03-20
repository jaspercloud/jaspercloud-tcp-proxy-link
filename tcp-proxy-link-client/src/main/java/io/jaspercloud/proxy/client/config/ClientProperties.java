package io.jaspercloud.proxy.client.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("proxy.client")
public class ClientProperties {

    //connection
    private String serverHost;
    private Integer socks5Port;
    private Integer connectTimeout;
    //auth
    private String username;
    private String password;
    //dst
    private String dstHost;
    private Integer dstPort;
    private Integer localPort;

    public String getServerHost() {
        return serverHost;
    }

    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }

    public Integer getSocks5Port() {
        return socks5Port;
    }

    public void setSocks5Port(Integer socks5Port) {
        this.socks5Port = socks5Port;
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

    public Integer getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Integer connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public String getDstHost() {
        return dstHost;
    }

    public void setDstHost(String dstHost) {
        this.dstHost = dstHost;
    }

    public Integer getDstPort() {
        return dstPort;
    }

    public void setDstPort(Integer dstPort) {
        this.dstPort = dstPort;
    }

    public Integer getLocalPort() {
        return localPort;
    }

    public void setLocalPort(Integer localPort) {
        this.localPort = localPort;
    }
}
