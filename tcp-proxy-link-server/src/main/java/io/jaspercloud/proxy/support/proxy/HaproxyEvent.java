package io.jaspercloud.proxy.support.proxy;

public class HaproxyEvent {

    private HaProxyProtocolHandler.IpAddress ipAddress;

    public HaProxyProtocolHandler.IpAddress getIpAddress() {
        return ipAddress;
    }

    public HaproxyEvent(HaProxyProtocolHandler.IpAddress ipAddress) {
        this.ipAddress = ipAddress;
    }
}
