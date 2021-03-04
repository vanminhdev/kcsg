package org.onosproject.kcsg.restapi.communicateapi.models;

/**
 * version model.
 */
public class VersionModel {
    private String ip;
    private int ver;

    public String getIp() {
        return ip;
    }

    public int getVer() {
        return ver;
    }

    public void setVer(int ver) {
        this.ver = ver;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}
