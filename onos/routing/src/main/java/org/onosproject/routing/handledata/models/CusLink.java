/**
 * CusLink.
 */
package org.onosproject.routing.handledata.models;

public class CusLink {
    private String idSrc;
    private int portSrc;
    private String idDst;
    private int portDst;
    public String getIdSrc() {
        return idSrc;
    }
    public int getPortDst() {
        return portDst;
    }
    public void setPortDst(int portDst) {
        this.portDst = portDst;
    }
    public String getIdDst() {
        return idDst;
    }
    public void setIdDst(String idDst) {
        this.idDst = idDst;
    }
    public int getPortSrc() {
        return portSrc;
    }
    public void setPortSrc(int portSrc) {
        this.portSrc = portSrc;
    }
    public void setIdSrc(String idSrc) {
        this.idSrc = idSrc;
    }
    public CusLink(String idSrc, int portSrc, String idDst, int portDst) {
        this.idSrc = idSrc;
        this.portSrc = portSrc;
        this.idDst = idDst;
        this.portDst = portDst;
    }
}
