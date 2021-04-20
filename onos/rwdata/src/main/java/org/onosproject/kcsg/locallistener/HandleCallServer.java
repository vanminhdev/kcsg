package org.onosproject.kcsg.locallistener;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.onosproject.kcsg.locallistener.models.ConfigRWModel;

public final class HandleCallServer {
    private static String serverUrl = HandleVersion.getServerUrl();

    private HandleCallServer() {}

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
            e.printStackTrace();
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
            e.printStackTrace();
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
            //LOG.error(MSG, e.getMessage());
        }
        return null;
    }

    public static void updateVersion(JSONObject body) {
        try {
            HttpResponse<String> response = Unirest
                .post(serverUrl + "/api/version/update-version")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .body(body)
                .asString();
        } catch (UnirestException e) {
            e.printStackTrace();
        }
    }

    public static void sendLogRead(JSONArray log) {
        try {
            HttpResponse<String> response = Unirest
                .post(serverUrl + "/api/log/log-read")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .body(log)
                .asString();
        } catch (UnirestException e) {
            e.printStackTrace();
        }
    }

    public static void sendLogWrite(JSONArray log) {
        try {
            HttpResponse<String> response = Unirest
                .post(serverUrl + "/api/log/log-write")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .body(log)
                .asString();
        } catch (UnirestException e) {
            e.printStackTrace();
        }
    }
}