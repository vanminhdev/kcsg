/*
 * Copyright © 2021 Copyright (c) 2021 Yoyodyne, Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sina.impl.handledata.models;

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
