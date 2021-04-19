package org.onosproject.routing.handledata.models;

import java.util.ArrayList;

public class CusTopo {
    private ArrayList<CusDevice> devices;
    private ArrayList<CusHost> hosts;
    private ArrayList<CusLink> links;
    public ArrayList<CusDevice> getDevices() {
        return devices;
    }
    public ArrayList<CusLink> getLinks() {
        return links;
    }
    public void setLinks(ArrayList<CusLink> links) {
        this.links = links;
    }
    public ArrayList<CusHost> getHosts() {
        return hosts;
    }
    public void setHosts(ArrayList<CusHost> hosts) {
        this.hosts = hosts;
    }
    public void setDevices(ArrayList<CusDevice> devices) {
        this.devices = devices;
    }
}
