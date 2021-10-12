package io.jaspercloud.proxy.core.dto;

public class Data {

    public interface Type {

        int Heart = 1;
        int ConnectReq = 2;
        int ConnectResp = 3;
    }

    private int type;

    public Data(int type) {
        this.type = type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }
}
