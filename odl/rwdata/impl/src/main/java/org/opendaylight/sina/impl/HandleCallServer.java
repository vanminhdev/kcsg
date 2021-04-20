/*
 * Copyright © 2021 Copyright (c) 2021 Yoyodyne, Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sina.impl;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.opendaylight.sina.impl.models.ConfigRWModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HandleCallServer {
    private static String serverUrl = HandleVersion.getServerUrl();
    private static final Logger LOG = LoggerFactory.getLogger(HandleCallServer.class);
    private static final String MSG = "msg: {}";

    private HandleCallServer() {
    }

    public static ConfigRWModel getRWConfig() {
        try {
            HttpResponse<String> response = Unirest
                .get(serverUrl + "/api/config/get-configrw")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .asString();

            JSONObject resBody = new JSONObject(response.getBody());

            ConfigRWModel config = new ConfigRWModel(resBody.getInt("r"), resBody.getInt("w"));
            return config;
        } catch (UnirestException e) {
            LOG.error(MSG, e.getMessage());
        }
        return null;
    }

    public static int getVersionFromServer(String ip) {
        try {
            HttpResponse<String> response = Unirest
                .get(serverUrl + "/api/version/get-version?ip=" + ip)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .asString();

            JSONObject resBody = new JSONObject(response.getBody());
            return resBody.getInt("version");
        } catch (UnirestException e) {
            LOG.error(MSG, e.getMessage());
        }
        return 0;
    }

    public static JSONArray getVersionsFromServer() {
        try {
            HttpResponse<String> response = Unirest
                .get(serverUrl + "/api/version/get-versions")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .asString();

            JSONArray resBody = new JSONArray(response.getBody());
            return resBody;
        } catch (UnirestException e) {
            LOG.error(MSG, e.getMessage());
        }
        return null;
    }

    public static void updateVersion(JSONObject body) {
        try {
            Unirest
                .post(serverUrl + "/api/version/update-version")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .body(body)
                .asString();
        } catch (UnirestException e) {
            LOG.error(MSG, e.getMessage());
        }
    }

    public static void sendLogRead(JSONArray log) {
        try {
            Unirest
                .post(serverUrl + "/api/log/log-read")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .body(log)
                .asString();
        } catch (UnirestException e) {
            LOG.error(MSG, e.getMessage());
        }
    }

    public static void sendLogWrite(JSONArray log) {
        try {
            Unirest
                .post(serverUrl + "/api/log/log-write")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .body(log)
                .asString();
        } catch (UnirestException e) {
            LOG.error(MSG, e.getMessage());
        }
    }
}
