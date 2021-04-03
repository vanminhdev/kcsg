/*
 * Copyright © 2021 Copyright (c) 2021 Yoyodyne, Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sina.impl.models;

public class ConfigRWModel {
    private int configR;
    private int configW;

    public ConfigRWModel(int configR, int configW) {
        this.configR = configR;
        this.configW = configW;
    }

    public int getR() {
        return configR;
    }

    public int getW() {
        return configW;
    }

    public void setW(int wnum) {
        this.configW = wnum;
    }

    public void setR(int rnum) {
        this.configR = rnum;
    }
}
