package org.onosproject.kcsg.locallistener.models;

/**
 * Member model.
 */
public class InforControllerModel {
    private String kindController;
    private String ip;

    public String getKindController() {
        return kindController;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setKindController(String kindController) {
        this.kindController = kindController;
    }
}
