package io.jaspercloud.proxy.core.dto;

public class ConnectRespData extends Data {

    private Integer proxyType;
    private String sessionId;
    private int code;

    public void setProxyType(Integer proxyType) {
        this.proxyType = proxyType;
    }

    public Integer getProxyType() {
        return proxyType;
    }

    public ConnectRespData() {
        super(Type.ConnectResp);
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}
