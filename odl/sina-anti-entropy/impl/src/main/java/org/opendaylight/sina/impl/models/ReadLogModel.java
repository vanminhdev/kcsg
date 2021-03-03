/*
 * Copyright © 2021 Copyright (c) 2021 Yoyodyne, Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sina.impl.models;

import java.util.Map;
import java.util.Set;

public class ReadLogModel {
    private Map<String, NotificationModel> notificationList;
    private Set<String> readIds;

    public ReadLogModel(Map<String,NotificationModel> notificationList, Set<String> ids) {
        this.notificationList = notificationList;
        this.readIds = ids;
    }

    public Map<String,NotificationModel> getNotificationList() {
        return this.notificationList;
    }

    public void setNotificationList(Map<String,NotificationModel> notificationList) {
        this.notificationList = notificationList;
    }

    public Set<String> getReadIds() {
        return this.readIds;
    }

    public void setReadIds(Set<String> ids) {
        this.readIds = ids;
    }

}
