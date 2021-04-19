/*
 * Copyright © 2021 Copyright (c) 2021 Yoyodyne, Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sina.impl.handledata.models;

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
