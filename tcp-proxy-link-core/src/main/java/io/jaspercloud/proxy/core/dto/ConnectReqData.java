package io.jaspercloud.proxy.core.dto;

public class ConnectReqData extends Data {

    public interface ProxyType {

        int Simple = 1;
        int Socks5 = 2;
    }

    private Integer proxyType;
    private String sessionId;
    private String remoteHost;
    private int remotePort;
    private String tunnelHost;
    private int tunnelPort;

    public ConnectReqData() {
        super(Type.ConnectReq);
    }

    public void setProxyType(Integer proxyType) {
        this.proxyType = proxyType;
    }

    public Integer getProxyType() {
        return proxyType;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setRemoteHost(String remoteHost) {
        this.remoteHost = remoteHost;
    }

    public String getRemoteHost() {
        return remoteHost;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public void setRemotePort(int remotePort) {
        this.remotePort = remotePort;
    }

    public String getTunnelHost() {
        return tunnelHost;
    }

    public void setTunnelHost(String tunnelHost) {
        this.tunnelHost = tunnelHost;
    }

    public int getTunnelPort() {
        return tunnelPort;
    }

    public void setTunnelPort(int tunnelPort) {
        this.tunnelPort = tunnelPort;
    }
}
