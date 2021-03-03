/*
 * Copyright © 2021 Copyright (c) 2021 Yoyodyne, Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sina.impl.models;

public class NotificationModel {
    private String id;
    private String eventType;
    private String time;
    private String data;
    private String ip;
    private String status;

    public NotificationModel(String id, String eventType, String time, String data, String ip) {
        this.id = id;
        this.eventType = eventType;
        this.time = time;
        this.data = data;
        this.ip = ip;
        this.status = "0";
    }

    public NotificationModel(String id, String eventType, String time, String data, String ip, String status) {
        this.id = id;
        this.eventType = eventType;
        this.time = time;
        this.data = data;
        this.ip = ip;
        this.status = status;
    }


    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEventType() {
        return this.eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getTime() {
        return this.time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getData() {
        return this.data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getIp() {
        return this.ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

}
