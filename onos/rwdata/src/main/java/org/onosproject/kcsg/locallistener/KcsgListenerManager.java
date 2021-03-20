package org.onosproject.kcsg.locallistener;

import org.json.JSONArray;
import org.json.JSONObject;
import org.onosproject.core.CoreService;
import org.onosproject.kcsg.locallistener.models.ConfigRWModel;
import org.onosproject.kcsg.locallistener.models.ResultReadModel;
import org.onosproject.kcsg.locallistener.models.ResultWriteModel;
import org.onosproject.kcsg.locallistener.models.InforControllerModel;
import org.onosproject.kcsg.restapi.ApiManager;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkListener;
import org.onosproject.net.link.LinkService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Component in charge of handling local topology events and applying
 * <p>
 * the mapping between internal changes and the exposed topologies.
 */
@Component(immediate = true)
public class KcsgListenerManager {
    private final Logger log = getLogger(getClass());
    private static final String INIT_PATH = "/home/onos/sdn";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LinkService linkService;

    public static final String PROVIDER_NAME = "org.onosproject.kcsg.listener";
    public static String myIpAddress = null;
    public static String serverUrl = null;

    private final LocalDeviceListener deviceListener = new LocalDeviceListener();
    private final LocalHostListener hostListener = new LocalHostListener();
    private final LocalLinkListener linkListener = new LocalLinkListener();

    @Activate
    public void activate() {
        coreService.registerApplication(PROVIDER_NAME);

        deviceService.addListener(deviceListener);
        linkService.addListener(linkListener);
        hostService.addListener(hostListener);
        log.info("Started");
        init();
        scheduleCommunicate();
    }

    @Deactivate
    public void deactivate() {
        deviceService.removeListener(deviceListener);
        linkService.removeListener(linkListener);
        hostService.removeListener(hostListener);
        log.info("Stopped");
    }

    private void init() {
        serverUrl = HandleVersion.getServerUrl();
        String path = INIT_PATH;
        path = path + "/listip.json";
        FileOutputStream fos = null;
        OutputStreamWriter wrt = null;
        try {
            fos = new FileOutputStream(path);
            wrt = new OutputStreamWriter(fos, StandardCharsets.UTF_8);

            File f = new File(path);

            try {
                HttpResponse<String> response = Unirest
                .get(serverUrl + "/api/remoteIp/list-ip")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .asString();

                if (response.getStatus() == 200) {
                    wrt.write(response.getBody());
                }
            } catch (UnirestException e) {
                if (!f.exists()) {
                    wrt.write("{\n" + "\t\"localIp\": \"...\",\n"
                        + "\t\"controller\": \"...\", \n"
                        + "\t\"communication\": [\n"
                        + "\t\t{\n"
                        + "\t\t\t\"ip\": \"...\", \n"
                        + "\t\t\t\"controller\": \"...\"\n"
                        + "\t\t},\n"
                        + "\t\t{\n"
                        + "\t\t\t\"ip\": \"...\", \n"
                        + "\t\t\t\"controller\": \"...\"\n"
                        + "\t\t}\n"
                        + "\t]"
                        + "}");
                }
            }
        } catch (IOException e) {
            log.error("Error when create file listip.json");
        } finally {
            try {
                if (wrt != null) {
                    wrt.close();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                log.error("Error when close write file listip");
            }
        }
        //create version.json
        HandleVersion.createVersion();
        var local = HandleVersion.getLocal();
        if (local != null) {
            myIpAddress = local.getIp();
        }
        log.info("myIp :" + myIpAddress + " serverUrl: " + serverUrl);
    }

    private class LocalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {
            ApiManager sinaApiManager = new ApiManager();
            JSONObject object = sinaApiManager.getDevices();
            switch (event.type()) {
                case DEVICE_ADDED:
                    logChange("DeviceListener DEVICE_ADDED", object.toString());
                    break;
                case DEVICE_REMOVED:
                    logChange("DeviceListener DEVICE_REMOVED", object.toString());
                    break;
                case DEVICE_SUSPENDED:
                    // sendNotify("devices", object.toString());
                    logChange("DeviceListener DEVICE_SUSPENDED", object.toString());
                    break;
                case PORT_REMOVED:
                    logChange("DeviceListener PORT_REMOVED", object.toString());
                    break;
                case PORT_ADDED:
                    // JSONObject object1 = sinaApiManager.getPorts();
                    // sendNotify("ports", object1.toString());
                    logChange("DeviceListener PORT_ADDED", object.toString());
                    break;
                default:
                    // logNotify("DeviceListener " + event.type().toString(), object.toString());
                    break;
            }
        }
    }

    private class LocalHostListener implements HostListener {

        @Override
        public void event(HostEvent event) {
            ApiManager sinaApiManager = new ApiManager();
            JSONObject object = sinaApiManager.getHosts();
            switch (event.type()) {
                case HOST_ADDED:
                    logChange("HostListener HOST_ADDED", object.toString());
                    break;
                case HOST_REMOVED:
                    // sendNotify("hosts", object.toString());
                    logChange("HostListener HOST_REMOVED", object.toString());
                    break;
                default:
                    logChange("HostListener " + event.type().toString(), object.toString());
                    break;
            }
        }
    }

    private class LocalLinkListener implements LinkListener {

        @Override
        public void event(LinkEvent event) {
            ApiManager sinaApiManager = new ApiManager();
            JSONObject object = sinaApiManager.getLinks();
            switch (event.type()) {
                case LINK_ADDED:
                    logChange("LinkListener LINK_ADDED", object.toString());
                    break;
                case LINK_REMOVED:
                    logChange("LinkListener LINK_REMOVED", object.toString());
                    break;
                case LINK_UPDATED:
                    logChange("LinkListener LINK_UPDATED", object.toString());
                    break;
                default:
                    logChange("LinkListener " + event.type().toString(), object.toString());
                    break;
            }
        }
    }

    private void logChange(String eventType, String data) {
        // data = ""; //test reset
        String strJson = "{\"id\":\"" + java.util.UUID.randomUUID() + "\","
            + "\"eventType\":\"" + eventType + "\","
            + "\"time\":\"" + java.time.LocalDateTime.now() + "\","
            + "\"data\":" + data + "}\n";
        writeLogChange(strJson);
    }

    private void writeLogChange(String strLog) {
        log.info("write a log change");
        // update version
        int ver = HandleVersion.getVersion(myIpAddress);
        HandleVersion.setVersion(myIpAddress, ++ver);

        JSONObject bodyReq = new JSONObject();
        bodyReq.put("ip", myIpAddress);
        bodyReq.put("version", ver);
        HandleCallServer.updateVersion(bodyReq);
        writeData(myIpAddress, ver);
    }

    private void writeData(String ip, int version) {
        ConfigRWModel config = HandleCallServer.getRWConfig();
        ArrayList<InforControllerModel> controllers = HandleVersion.getRandomMembers(config.getW());

        JSONArray log = new JSONArray();
        for (InforControllerModel dstController : controllers) {
            JSONObject logDetail = new JSONObject();
            logDetail.put("localIp", myIpAddress);
            logDetail.put("srcIp", ip);
            logDetail.put("dstIp", dstController.getIp());
            logDetail.put("start", java.time.LocalDateTime.now());
            logDetail.put("version", version);
            ResultWriteModel result = null;
            if (ip != dstController.getIp()) {
                result = handleWrite(ip, version, dstController);
            } else {
                logDetail.put("length", 0);
            }
            if (result != null) {
                logDetail.put("length", result.getLength());
            }
            logDetail.put("end", java.time.LocalDateTime.now());
            log.put(logDetail);
        }
        HandleCallServer.sendLogWrite(log);
    }

    private ResultWriteModel handleWrite(String srcIp, int srcVersion, InforControllerModel desCtrller) {
        switch (desCtrller.getKindController()) {
        case "ONOS": {
            try {
                JSONObject bodyReq = new JSONObject();
                bodyReq.put("ip", srcIp);
                bodyReq.put("version", srcVersion);
                HttpResponse<String> response = Unirest
                        .post("http://" + desCtrller.getIp() + ":8181/onos/kcsg/communicate/update-version")
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .header("Authorization", "Basic a2FyYWY6a2FyYWY=")
                        .body(bodyReq)
                        .asString();
                ResultWriteModel result = new ResultWriteModel();
                if (response.getStatus() == 200) {
                    log.info("update success in controller: " + desCtrller.getIp());
                } else {
                    log.warn("update version in controller: " + desCtrller.getIp() +
                        " with status code: " + response.getStatus());
                }
                result.setLength(bodyReq.toString().getBytes().length);
                return result;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            break;
        }
        case "Faucet": {
            try {
                JSONObject bodyReq = new JSONObject();
                bodyReq.put("ip", srcIp);
                bodyReq.put("version", srcVersion);
                HttpResponse<String> response = Unirest
                    .post("http://" + desCtrller.getIp() + ":8080/faucet/sina/versions/update-version")
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Authorization", "Basic a2FyYWY6a2FyYWY=")
                    .body(bodyReq)
                    .asString();

                ResultWriteModel result = new ResultWriteModel();
                if (response.getStatus() == 200) {
                    log.info("update success in controller: " + desCtrller.getIp());
                } else {
                    log.warn("read version in controller: " + desCtrller.getIp() +
                        " with status code: " + response.getStatus());
                }
                result.setLength(bodyReq.toString().getBytes().length);
                return result;
            } catch (Exception e) {
                log.error(e.getMessage());
            }
            break;
        }
        case "ODL": {
            try {
                JSONObject ipVerJson = new JSONObject();
                ipVerJson.put("ip", srcIp);
                ipVerJson.put("version", srcVersion);

                JSONObject dataJson = new JSONObject();
                dataJson.put("data", ipVerJson.toString());

                JSONObject bodyReq = new JSONObject();
                bodyReq.put("input", dataJson);

                HttpResponse<String> response = Unirest
                    .post("http://" + desCtrller.getIp() + ":8181/restconf/operations/sina:updateVersion")
                    .header("Content-Type", "application/json").header("Accept", "application/json")
                    .header("Authorization", "Basic YWRtaW46YWRtaW4=").body(bodyReq).asString();
                ResultWriteModel result = new ResultWriteModel();
                if (response.getStatus() == 200) {
                    log.info("update success in controller: " + desCtrller.getIp());
                } else {
                    log.warn("read version in controller: " + desCtrller.getIp() + " with status code: "
                        + response.getStatus());
                }
                result.setLength(bodyReq.toString().getBytes().length);
                return result;
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

    private void readData() {
        ConfigRWModel config = HandleCallServer.getRWConfig();
        InforControllerModel controllerTarget = HandleVersion.getRandomMember();
        int versionFromServer = HandleCallServer.getVersionFromServer(controllerTarget.getIp());
        ArrayList<InforControllerModel> controllers = HandleVersion.getRandomMembers(config.getR());

        JSONArray log = new JSONArray();
        for (InforControllerModel dstController : controllers) {
            JSONObject logDetail = new JSONObject();
            logDetail.put("localIp", myIpAddress);
            logDetail.put("srcIp", controllerTarget.getIp());
            logDetail.put("dstIp", dstController.getIp());
            logDetail.put("start", java.time.LocalDateTime.now());
            logDetail.put("version", versionFromServer);
            ResultReadModel result = null;
            if (!controllerTarget.getIp().equals(dstController.getIp())) {
                result = handleRead(controllerTarget, dstController, versionFromServer);
            } else {
                logDetail.put("isSuccess", true);
                logDetail.put("length", 0);
            }
            if (result != null) {
                logDetail.put("isSuccess", result.isSuccess());
                logDetail.put("length", result.getLength());
            }
            logDetail.put("end", java.time.LocalDateTime.now());
            log.put(logDetail);
        }
        HandleCallServer.sendLogRead(log);
    }

    private ResultReadModel handleRead(InforControllerModel srcCtrller, InforControllerModel desCtrller, int srcVer) {
        switch (desCtrller.getKindController()) {
        case "ONOS": {
            try {
                JSONObject bodyReq = new JSONObject();
                bodyReq.put("ip", srcCtrller.getIp());
                HttpResponse<String> response = Unirest
                        .post("http://" + desCtrller.getIp() + ":8181/onos/kcsg/communicate/get-version")
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .header("Authorization", "Basic a2FyYWY6a2FyYWY=")
                        .body(bodyReq)
                        .asString();
                ResultReadModel result = new ResultReadModel();
                if (response.getStatus() == 200) {
                    JSONObject resBody = new JSONObject(response.getBody());
                    result.setSuccess(resBody.getInt("version") == srcVer);
                } else {
                    result.setSuccess(false);
                    log.warn("read version in controller: " + desCtrller.getIp() +
                        " with status code: " + response.getStatus());
                }
                result.setLength(bodyReq.toString().getBytes().length);
                return result;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            break;
        }
        case "Faucet": {
            try {
                JSONObject bodyReq = new JSONObject();
                bodyReq.put("ip", srcCtrller.getIp());
                HttpResponse<String> response = Unirest
                    .post("http://" + desCtrller.getIp() + ":8080/faucet/sina/versions/get-version")
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Authorization", "Basic a2FyYWY6a2FyYWY=")
                    .body(bodyReq)
                    .asString();

                ResultReadModel result = new ResultReadModel();
                if (response.getStatus() == 200) {
                    JSONObject resBody = new JSONObject(response.getBody());
                    log.info("BODY: " + resBody);
                    result.setSuccess(resBody.getInt("version") == srcVer);
                } else {
                    result.setSuccess(false);
                    log.warn("read version in controller: " + desCtrller.getIp() +
                        " with status code: " + response.getStatus());
                }
                result.setLength(bodyReq.toString().getBytes().length);
                return result;
            } catch (Exception e) {
                log.error(e.getMessage());
            }
            break;
        }
        case "ODL": {
            try {
                JSONObject ipJson = new JSONObject();
                ipJson.put("ip", srcCtrller.getIp());

                JSONObject dataJson = new JSONObject();
                dataJson.put("data", ipJson.toString());

                JSONObject bodyReq = new JSONObject();
                bodyReq.put("input", dataJson);

                HttpResponse<String> response = Unirest
                    .post("http://" + desCtrller.getIp() + ":8181/restconf/operations/sina:getVersion")
                    .header("Content-Type", "application/json").header("Accept", "application/json")
                    .header("Authorization", "Basic YWRtaW46YWRtaW4=").body(bodyReq).asString();
                ResultReadModel result = new ResultReadModel();
                if (response.getStatus() == 200) {
                    JSONObject resBody = new JSONObject(response.getBody());
                    JSONObject outputJson = resBody.getJSONObject("output");
                    JSONObject verJson = new JSONObject(outputJson.getString("result"));
                    result.setSuccess(verJson.getInt("version") == srcVer);
                } else {
                    result.setSuccess(false);
                    log.warn("read version in controller: " + desCtrller.getIp() + " with status code: "
                        + response.getStatus());
                }
                result.setLength(bodyReq.toString().getBytes().length);
                return result;
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

    private void scheduleCommunicate() {
        new Timer().scheduleAtFixedRate(
            new TimerTask() {
                @Override
                public void run() {
                    readData();
                }
            }, 0, 1000
        );
    }
}
