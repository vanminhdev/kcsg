package org.onosproject.kcsg.locallistener;

import org.json.JSONArray;
import org.json.JSONObject;
import org.onosproject.core.CoreService;
import org.onosproject.kcsg.locallistener.models.InforControllerModel;
import org.onosproject.kcsg.restapi.KcsgApiManager;
import org.onosproject.kcsg.restapi.communicateapi.models.DataUpdateModel;
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

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Component in charge of handling local topology events and applying
 * <p>
 * the mapping between internal changes and the exposed topologies.
 */
@Component(immediate = true)
public class KcsgListenerManager {
    private final Logger log = getLogger(getClass());

    public static boolean lockFlag = false;
    public static Queue<String> loggingQueue = new LinkedList<>();
    public static Queue<DataUpdateModel> updateDataQueue = new LinkedList<>();

    public static ArrayList<InforControllerModel> memberList = new ArrayList<>();

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

    private final LocalDeviceListener deviceListener = new LocalDeviceListener();
    private final LocalHostListener hostListener = new LocalHostListener();
    private final LocalLinkListener linkListener = new LocalLinkListener();

    @Activate
    public void activate() {
        coreService.registerApplication(PROVIDER_NAME);

        deviceService.addListener(deviceListener);
        linkService.addListener(linkListener);
        hostService.addListener(hostListener);
        log.info("Started kcsg");
        myIpAddress = "192.168.50.131";
        init();
        scheduleWriteLogChange();
        scheduleUpdateData();
        scheduleCommunicate();
    }

    @Deactivate
    public void deactivate() {
        deviceService.removeListener(deviceListener);
        linkService.removeListener(linkListener);
        hostService.removeListener(hostListener);

        log.info("Stopped kcsg");
    }

    private void init() {
        //create listip.json
        try {
            String path = System.getProperty("java.io.tmpdir");
            path = path + "/listip.json";
            FileOutputStream fos = new FileOutputStream(path);

            OutputStreamWriter wrt = new OutputStreamWriter(fos);

            wrt.write("{\n" + "\t\"localIp\": \"" + myIpAddress
                    + "\",\n"
                    + "\t\"controller\": \"ONOS\", \n"
                    + "\t\"communication\": [\n"
                    + "\t\t{\n"
                    + "\t\t\t\"ip\": \"192.168.50.131\", \n"
                    + "\t\t\t\"controller\": \"ONOS\"\n"
                    + "\t\t}, \n"
                    + "\t\t{\n"
                    + "\t\t\t\"ip\": \"192.168.50.131\", \n"
                    + "\t\t\t\"controller\": \"ONOS\"\n"
                    + "\t\t}\n"
                    + "\t]\n"
                    + "}");
            wrt.close();
            fos.close();
        } catch (IOException e) {
            log.info("Error when create file listip.json");
        }
        //create version.json
        HandleVersion.createVersion();
    }

    private class LocalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {
            KcsgApiManager sinaApiManager = new KcsgApiManager();
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
            KcsgApiManager sinaApiManager = new KcsgApiManager();
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
            KcsgApiManager sinaApiManager = new KcsgApiManager();
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
        KcsgListenerManager.loggingQueue.add(strJson);
    }

    private void writeLogChange() {
        if (KcsgListenerManager.lockFlag) {
            return;
        }
        String strLog = KcsgListenerManager.loggingQueue.poll();
        if (strLog != null) {
            log.info("write a log change");
            // lock
            KcsgListenerManager.lockFlag = true;

            Writer outputStreamWriter = null;
            BufferedWriter bufferWriter = null;
            try {
                // update version
                int ver = HandleVersion.getVersion(myIpAddress);
                HandleVersion.setVersion(myIpAddress, ++ver);

                String path = System.getProperty("java.io.tmpdir");
                outputStreamWriter = new OutputStreamWriter(
                    new FileOutputStream(path + "/" + myIpAddress + ".json", true),
                    StandardCharsets.UTF_8
                );
                bufferWriter = new BufferedWriter(outputStreamWriter);
                bufferWriter.append(strLog);
            } catch (Exception e) {
                log.error("Error when write change log file");
            } finally {
                // unlock
                KcsgListenerManager.lockFlag = false;
                try {
                    if (bufferWriter != null) {
                        bufferWriter.close();
                    }
                    if (outputStreamWriter != null) {
                        outputStreamWriter.close();
                    }
                } catch (IOException e) {
                    log.error("Error when close writer change log file");
                }
            }
        }
    }

    private void scheduleWriteLogChange() {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                writeLogChange();
            }
        }, 0, 100);
    }

    /**
     * update data from other SDN.
     */
    private void updateData() {
        if (KcsgListenerManager.lockFlag) {
            return;
        }
        DataUpdateModel updateData = KcsgListenerManager.updateDataQueue.poll();
        if (updateData != null) {
            // lock
            KcsgListenerManager.lockFlag = true;
            int currVer = HandleVersion.getVersion(updateData.getIp());
            // nho hon thi cap nhat
            if (currVer < updateData.getVersion()) {
                log.info("start update data from ip:" + updateData.getIp());
                //update version
                HandleVersion.setVersion(updateData.getIp(), updateData.getVersion());
                //update data
                Writer outputStreamWriter = null;
                BufferedWriter bufferWriter = null;
                try {
                    String path = System.getProperty("java.io.tmpdir");
                    outputStreamWriter = new OutputStreamWriter(
                        new FileOutputStream(path + "/" + updateData.getIp() + ".json", false),
                        StandardCharsets.UTF_8
                    );
                    bufferWriter = new BufferedWriter(outputStreamWriter);
                    bufferWriter.write(updateData.getData());
                    log.info("update success data from ip:" + updateData.getIp());
                } catch (IOException e) {
                    log.error("Error when write data file");
                } finally {
                    try {
                        if (bufferWriter != null) {
                            bufferWriter.close();
                        }
                        if (outputStreamWriter != null) {
                            outputStreamWriter.close();
                        }
                    } catch (IOException e) {
                        log.error("Error when close writer update data");
                    }
                }
                // unlock
                KcsgListenerManager.lockFlag = false;
            }
        }
    }

    private void scheduleUpdateData() {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateData();
            }
        }, 0, 150);
    }

    private void handleCommunicate() {
        int numMem = KcsgListenerManager.memberList.size();
        if (numMem > 0) {
            Random rand = new Random();
            int index = rand.nextInt(numMem);
            InforControllerModel mem = KcsgListenerManager.memberList.get(index);
            KcsgListenerManager.memberList.remove(index);

            String jsonVer = HandleVersion.getVersions();
            if (mem == null || mem.getIp() == null || mem.getKindController() == null) {
                return;
            }
            switch (mem.getKindController()) {
                case "ONOS": {
                    Unirest.setTimeouts(0, 0);
                    try {
                        // JSONObject contentComp = new JSONObject();
                        // contentComp.put("ver", jsonVer);

                        // log.info(contentComp.toString());
                        HttpResponse<String> response = Unirest
                            .post("http://" + mem.getIp() + ":8181/onos/kcsg/communicate/compareVersions")
                            .header("Content-Type", "application/json")
                            .header("Accept", "application/json")
                            .header("Authorization", "Basic a2FyYWY6a2FyYWY=")
                            .body(jsonVer).asString();
                        if (response.getStatus() == 200) {
                            String body = response.getBody();
                            JSONArray datas = new JSONArray();
                            JSONArray arr = new JSONArray(body);

                            int len = arr.length();
                            for (int i = 0; i < len; i++) {
                                String ip = arr.getString(i);
                                int ver = HandleVersion.getVersion(ip);
                                String data = HandleVersion.getData(ip);

                                JSONObject json = new JSONObject();
                                json.put("ip", ip);
                                json.put("ver", ver);
                                json.put("data", data);
                                datas.put(json);
                            }
                            if (len > 0) {
                                HttpResponse<String> resUpdateData = Unirest
                                    .put("http://" + mem.getIp() + ":8181/onos/kcsg/communicate/updateNewLog")
                                    .header("Content-Type", "application/json")
                                    .header("Accept", "application/json")
                                    .header("Authorization", "Basic a2FyYWY6a2FyYWY=")
                                    .body(datas.toString()).asString();
                                if (resUpdateData.getStatus() == 200) {
                                    log.info("send update data success");
                                } else {
                                    log.warn("send update data fail with status code: " + resUpdateData.getStatus());
                                }
                            }
                        } else {
                            log.warn("compare version with status code: " + response.getStatus());
                        }
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                    break;
                }
                case "Faucet": {
                    log.info("http://" + mem.getIp() + ":8080/faucet/sina/versions/get-new");
                    String url = "http://" + mem.getIp() + ":8080/faucet/sina/versions/get-new";

                    Unirest.setTimeouts(0, 0);
                    try {
                        HttpResponse<String> response = Unirest.post(url)
                                .header("Content-Type", "application/json")
                                .header("Accept", "application/json")
                                .header("Authorization", "Basic a2FyYWY6a2FyYWY=")
                                .body(jsonVer).asString();
                        if (response.getStatus() == 200) {
                            String body = response.getBody();
                            log.info("BODY: " + body);
                            JSONArray datas = new JSONArray();
                            JSONArray arr = new JSONArray(body);

                            int len = arr.length();
                            for (int i = 0; i < len; i++) {
                                String ip = arr.getString(i);
                                int ver = HandleVersion.getVersion(ip);
                                String data = HandleVersion.getData(ip);

                                JSONObject json = new JSONObject();
                                json.put("ip", ip);
                                json.put("version", ver);
                                json.put("content", data);
                                datas.put(json);
                            }
                            if (len > 0) {
                                log.info("http://" + mem.getIp() + ":8080/faucet/sina/log/update");
                                HttpResponse<String> resUpdateData = Unirest
                                    .post("http://" + mem.getIp() + ":8080/faucet/sina/log/update")
                                    .header("Content-Type", "application/json")
                                    .header("Accept", "application/json")
                                    .header("Authorization", "Basic a2FyYWY6a2FyYWY=")
                                    .body(datas.toString()).asString();
                                if (resUpdateData.getStatus() == 200) {
                                    log.info("send update data success");
                                } else {
                                    log.warn("send update data fail with status code: " + resUpdateData.getStatus());
                                }
                            }
                        } else {
                            log.warn("compare version with status code: " + response.getStatus());
                        }
                    } catch (Exception e) {
                        log.error(e.getMessage());
                    }
                    break;
                }
                case "ODL": {
                    String url = "http://" + mem.getIp() + ":8181/restconf/operations/kcsg:compareVersions";

                    Unirest.setTimeouts(0, 0);
                    try {
                        HttpResponse<String> response = Unirest.post(url).header("Content-Type", "application/json")
                                .header("Accept", "application/json").header("Authorization", "Basic YWRtaW46YWRtaW4=")
                                .body(jsonVer).asString();
                    } catch (Exception e) {
                        log.error(e.getMessage());
                    }
                    break;
                }
                default:
                    break;
            }
        } else {
            KcsgListenerManager.memberList = HandleVersion.getMembers();
        }
    }

    private void scheduleCommunicate() {
        new Timer().scheduleAtFixedRate(
            new TimerTask() {
                @Override
                public void run() {
                    handleCommunicate();
                }
            }, 0, 5000
        );
    }
}