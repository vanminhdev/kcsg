/**
 * CusDevice.
 */
package org.onosproject.routing.handledata.models;

public class CusDevice {
    private String id;
    private String type;
    public String getId() {
        return id;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public void setId(String id) {
        this.id = id;
    }
    public CusDevice(String id, String type) {
        this.id = id;
        this.type = type;
    }
}
