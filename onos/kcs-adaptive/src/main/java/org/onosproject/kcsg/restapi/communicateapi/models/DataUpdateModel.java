package org.onosproject.kcsg.restapi.communicateapi.models;

/**
 * Model update file log from other SDN.
 */
public class DataUpdateModel {

    private String ip;
    private int version;
    private String data;

    public String getIp() {
        return ip;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}
