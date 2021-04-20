/**
 * CusHost.
 */
package org.onosproject.routing.handledata.models;

public class CusHost {
    private int port;
    private String id;
    private String deviceId;
    public int getPort() {
        return port;
    }
    public String getDeviceId() {
        return deviceId;
    }
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public void setPort(int port) {
        this.port = port;
    }
    public CusHost(int port, String id, String deviceId) {
        this.port = port;
        this.id = id;
        this.deviceId = deviceId;
    }
}
