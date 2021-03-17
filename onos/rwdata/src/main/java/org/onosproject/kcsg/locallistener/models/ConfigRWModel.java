package org.onosproject.kcsg.locallistener.models;

public class ConfigRWModel {
    private int r;
    private int w;

    public ConfigRWModel(int r, int w) {
        this.r = r;
        this.w = w;
    }

    public int getR() {
        return r;
    }

    public int getW() {
        return w;
    }

    public void setW(int w) {
        this.w = w;
    }

    public void setR(int r) {
        this.r = r;
    }
}
