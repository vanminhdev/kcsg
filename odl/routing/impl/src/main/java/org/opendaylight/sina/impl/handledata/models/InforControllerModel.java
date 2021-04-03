/*
 * Copyright © 2021 Copyright (c) 2021 Yoyodyne, Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sina.impl.handledata.models;

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