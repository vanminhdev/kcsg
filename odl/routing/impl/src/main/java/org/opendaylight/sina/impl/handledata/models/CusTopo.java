/*
 * Copyright © 2021 Copyright (c) 2021 Yoyodyne, Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sina.impl.handledata.models;

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
