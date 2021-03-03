/*
 * Copyright © 2021 Copyright (c) 2021 Yoyodyne, Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sina.impl.models;

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
