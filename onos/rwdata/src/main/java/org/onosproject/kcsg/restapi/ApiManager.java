package org.onosproject.kcsg.restapi;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Collections;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.onosproject.core.CoreService;
import org.onosproject.kcsg.locallistener.HandleCallServer;
import org.onosproject.kcsg.locallistener.HandleVersion;
import org.onosproject.kcsg.locallistener.KcsgListenerManager;
import org.onosproject.kcsg.locallistener.models.ConfigRWModel;
import org.onosproject.kcsg.locallistener.models.InforControllerModel;
import org.onosproject.kcsg.restapi.communicateapi.KcsgCommunicateApiService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

/**
 * Component in charge of handling local topology events and applying
 * <p>
 * the mapping between internal changes and the exposed topologies.
 */
@Component(immediate = true, service = {KcsgCommunicateApiService.class})
public class ApiManager implements KcsgCommunicateApiService {
    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    public static final String PROVIDER_NAME = "org.onosproject.kcsg.api";

    @Activate
    public void activate() {
        coreService.registerApplication(PROVIDER_NAME);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    public JSONObject getDevices() {
        String url = "http://localhost:8181/onos/v1/devices";
        return requestGet(url);
    }

    public JSONObject getPorts() {
        String url = "http://localhost:8181/onos/v1/devices/ports";
        return requestGet(url);
    }

    public JSONObject getHosts() {
        String url = "http://localhost:8181/onos/v1/hosts";
        return requestGet(url);
    }

    public JSONObject getLinks() {
        String url = "http://localhost:8181/onos/v1/links";
        return requestGet(url);
    }

    private static JSONObject requestGet(String url) {
        try {
            Unirest.setTimeouts(0, 0);
            HttpResponse<String> response = Unirest.get(url).header("Accept", "application/json")
                    .header("Authorization", "Basic a2FyYWY6a2FyYWY=").asString();

            JSONObject object = new JSONObject(response);
            return new JSONObject(object.getString("body"));
        } catch (UnirestException e) {
            return null;
        }
    }

    private JSONObject readDataTestPing() {
        //doc version tu server truoc
        JSONArray verFromServer = HandleCallServer.getVersionsFromServer();
        log.info("version from server " + verFromServer);
        if (verFromServer == null) {
            return null;
        }

        //doc version tu r controller khac
        ConfigRWModel config = HandleCallServer.getRWConfig();
        ArrayList<InforControllerModel> controllers = HandleVersion.getRandomMembers(config.getR() - 1);

        JSONObject logDetail = new JSONObject();
        logDetail.put("targetIp", KcsgListenerManager.myIpAddress);
        logDetail.put("start", java.time.LocalDateTime.now());
        JSONArray allVersion = new JSONArray();
        for (InforControllerModel dstController : controllers) {
            JSONObject getVer = handleReadTestPing(dstController);
            if (getVer != null) {
                allVersion.put(getVer);
            } else {
                log.error("get ver test ping from " + dstController.getIp()
                    + " type: " + dstController.getKindController());
            }
        }
        //them ver cua controller nay
        allVersion.put(new JSONObject(HandleVersion.getVersions()));
        log.info("all version " + allVersion.toString());

        boolean checkAllSuccess = true;
        for (int i = 0; i < verFromServer.length(); i++) {
            boolean checkSuccess = false;
            String ip = verFromServer.getJSONObject(i).getString("ip");
            int version = verFromServer.getJSONObject(i).getInt("version");

            for (int j = 0; j < allVersion.length(); j++) {
                JSONObject currJson = allVersion.getJSONObject(j);
                try {
                    //neu version tu r controller khac >= version tu server thi la dung
                    JSONObject versionDetail = currJson.getJSONObject(ip);
                    if (versionDetail.getInt("version") >= version) {
                        checkSuccess = true;
                        break;
                    }
                } catch (JSONException e) {
                    log.error("error read version cua ip: " + ip);
                }
            }

            if (!checkSuccess) {
                checkAllSuccess = false;
                break;
            }
        }
        logDetail.put("end", java.time.LocalDateTime.now());
        logDetail.put("isVersionSuccess", checkAllSuccess);

        ArrayList<Long> listTStaleness = new ArrayList<>();
        ArrayList<Integer> listVStaleness = new ArrayList<>();
        //tinh v staleness va tinh t staleness
        for (int i = 0; i < verFromServer.length(); i++) {
            String ip = verFromServer.getJSONObject(i).getString("ip");
            int version = verFromServer.getJSONObject(i).getInt("version");

            long maxTime = 0;
            int minSubVer = 9999; //max hieu ver tu server va tu cac controller cho moi ip
            for (int j = 0; j < allVersion.length(); j++) {
                JSONObject currJson = allVersion.getJSONObject(j);
                try {
                    JSONObject versionDetail = currJson.getJSONObject(ip);
                    int subVer = version - versionDetail.getInt("version");
                    if (subVer < minSubVer && subVer >= 0) {
                        minSubVer = subVer;
                    }
                    long timeSet = versionDetail.getLong("timeSet");
                    if (timeSet > maxTime) {
                        maxTime = timeSet;
                    }
                } catch (JSONException e) {
                    log.error("error read version cua ip: " + ip);
                }
            }
            listVStaleness.add(minSubVer);
            listTStaleness.add(maxTime);
        }

        int max = 0;
        int min = 0;
        float avg = 0;
        if (listVStaleness.size() > 0) {
            max = Collections.max(listVStaleness);
            min = Collections.min(listVStaleness);
            int sum = 0;
            for (Integer mark : listVStaleness) {
                sum += mark;
            }
            avg = (float) sum / listVStaleness.size();
        }

        long timeRead = System.currentTimeMillis();
        long tstaleness = 0;
        if (listTStaleness.size() > 0) {
            long maxTimeSet = Collections.max(listTStaleness);
            tstaleness = timeRead - maxTimeSet;
        }

        logDetail.put("vStalenessMax", max);
        logDetail.put("vStalenessMin", min);
        logDetail.put("vStalenessAvg", avg);

        logDetail.put("tStaleness", tstaleness);

        log.info(logDetail.toString());
        return logDetail;
    }

    private JSONObject handleReadTestPing(InforControllerModel desCtrller) {
        switch (desCtrller.getKindController()) {
            case "ONOS": {
                try {
                    HttpResponse<String> response = Unirest
                        .get("http://" + desCtrller.getIp() + ":8181/onos/rwdata/communicate/get-versions")
                        .header("Authorization", "Basic a2FyYWY6a2FyYWY=")
                        .asString();

                    log.info("onos " + response.getBody());
                    if (response.getStatus() == 200) {
                        JSONObject resBody = new JSONObject(response.getBody());
                        log.info("get ver onos " + resBody.toString());
                        return resBody;
                    } else {
                        log.warn("read version in controller: " + desCtrller.getIp() + " with status code: "
                            + response.getStatus());
                    }
                } catch (UnirestException e) {
                    log.error(e.getMessage());
                }
                break;
            }
            case "Faucet": {
                try {
                    HttpResponse<String> response = Unirest
                        .get("http://" + desCtrller.getIp() + ":8080/faucet/sina/versions/get-versions")
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .header("Authorization", "Basic a2FyYWY6a2FyYWY=")
                        .asString();
                    if (response.getStatus() == 200) {
                        JSONObject resBody = new JSONObject(response.getBody());
                        log.info("get ver faucet " + resBody.toString());
                        return resBody;
                    } else {
                        log.warn("read version in controller: " + desCtrller.getIp() + " with status code: "
                            + response.getStatus());
                    }
                } catch (UnirestException e) {
                    log.error(e.getMessage());
                }
                break;
            }
            case "ODL": {
                try {
                    HttpResponse<String> response = Unirest
                        .post("http://" + desCtrller.getIp() + ":8181/restconf/operations/sina:getVersions")
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                        .asString();
                    if (response.getStatus() == 200) {
                        JSONObject resBody = new JSONObject(response.getBody());
                        JSONObject outputJson = resBody.getJSONObject("output");
                        JSONObject resultJson = new JSONObject(outputJson.getString("result"));

                        log.info("get ver odl " + resultJson.toString());
                        return resultJson;
                    } else {
                        log.warn("read version in controller: " + desCtrller.getIp() + " with status code: "
                            + response.getStatus());
                    }
                } catch (UnirestException e) {
                    log.error(e.getMessage());
                }
                break;
            }
            default:
                break;
        }
        return null;
    }

    @Override
    public JSONObject testPing(String src, String dst) {
        JSONObject result = new JSONObject();
        result.put("isPingSuccess", false);
        JSONObject testPing = new JSONObject();
        testPing.put("src", src);
        testPing.put("dst", dst);
        JSONObject logDetail = readDataTestPing();
        String id = "";
        if (logDetail != null) {
            log.info("call api write log " + KcsgListenerManager.serverUrl +
                "/api/Log/log-read-test-ping");
            try {
                HttpResponse<String> response = Unirest
                    .post(KcsgListenerManager.serverUrl + "/api/Log/log-read-test-ping")
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .body(logDetail)
                    .asString();
                if (response.getStatus() == 200) {
                    log.info("write log test ping success");
                    id = response.getBody();
                    result.put("id", id);
                } else {
                    log.warn(response.getBody());
                }
            } catch (UnirestException e) {
                log.error(e.getMessage());
            }

            if (logDetail.getBoolean("isVersionSuccess")) {
                log.info("call api mininet " + KcsgListenerManager.apiMininet + "/forwarding");
                try {
                    HttpResponse<String> response = Unirest
                        .post(KcsgListenerManager.apiMininet + "/forwarding")
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .body(testPing)
                        .asString();
                    if (response.getStatus() == 200) {
                        if (response.getBody().equals("True")) {
                            result.put("isPingSuccess", true);
                        }
                    } else {
                        log.warn("call api mininet with status code: "
                            + response.getStatus());
                    }
                } catch (UnirestException e) {
                    log.error(e.getMessage());
                }
            }
        }
        log.info("result test ping " + result.toString());
        return result;
    }
}
