package org.onosproject.kcsg.locallistener.models;

public class ResultReadModel {
    private boolean isSuccess;
    private int length;

    public boolean isSuccess() {
        return isSuccess;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public void setSuccess(boolean isSuccess) {
        this.isSuccess = isSuccess;
    }
}
