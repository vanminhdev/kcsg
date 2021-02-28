/*
 * Copyright © 2021 Copyright (c) 2021 Yoyodyne, Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sina.impl;

import com.google.common.util.concurrent.ListenableFuture;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.jdt.annotation.NonNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.sina.impl.models.DataUpdateModel;
import org.opendaylight.sina.impl.models.InforControllerModel;
import org.opendaylight.sina.impl.models.VersionModel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sina.rev200908.CompareVersionsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sina.rev200908.CompareVersionsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sina.rev200908.CompareVersionsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sina.rev200908.SinaService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sina.rev200908.UpdateNewDataInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sina.rev200908.UpdateNewDataOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sina.rev200908.UpdateNewDataOutputBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SinaProvider implements SinaService, DataTreeChangeListener<Node> {
    private static final Logger LOG = LoggerFactory.getLogger(SinaProvider.class);

    private final DataBroker dataBroker;
    private ObjectRegistration<SinaService> sinaService;
    private final RpcProviderService rpcProviderService;
    private ListenerRegistration<?> listenerRegistration;

    @SuppressWarnings(value = { "MS_PKGPROTECT" })
    @SuppressFBWarnings(value = { "MS_PKGPROTECT" })
    public static boolean lockFlag = false;
    @SuppressWarnings(value = { "MS_PKGPROTECT" })
    @SuppressFBWarnings(value = { "MS_PKGPROTECT" })
    private static final Queue<String> LOGGING_QUEUE = new LinkedList<>();
    @SuppressWarnings(value = { "MS_PKGPROTECT" })
    @SuppressFBWarnings(value = { "MS_PKGPROTECT" })
    private static final Queue<DataUpdateModel> UPDATE_DATA_QUEUE = new LinkedList<>();
    @SuppressWarnings(value = { "MS_PKGPROTECT" })
    @SuppressFBWarnings(value = { "MS_PKGPROTECT" })
    private static final ArrayList<InforControllerModel> MEMBER_LIST = new ArrayList<>();
    @SuppressWarnings(value = { "MS_PKGPROTECT" })
    @SuppressFBWarnings(value = { "MS_PKGPROTECT" })
    public String myIpAddress = null;

    public static final String INIT_PATH = "/tmp";

    final InstanceIdentifier<Node> instanceIdentifier = InstanceIdentifier.builder(Nodes.class).child(Node.class)
            .build();

    public SinaProvider(final DataBroker dataBroker, RpcProviderService rpcProviderService) {
        this.dataBroker = dataBroker;
        this.rpcProviderService = rpcProviderService;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        myIpAddress = "192.168.50.137";
        LOG.info("SinaProvider Session Initiated");
        sinaService = rpcProviderService.registerRpcImplementation(SinaService.class, this);

        listenerRegistration = dataBroker.registerDataTreeChangeListener(
                DataTreeIdentifier.create(LogicalDatastoreType.OPERATIONAL, instanceIdentifier), this);

        createListIpFile();
        scheduleWriteLogChange();
        scheduleUpdateData();
        scheduleCommunicate();
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("SinaProvider Closed");
        if (sinaService != null) {
            sinaService.close();
        }
        if (listenerRegistration != null) {
            listenerRegistration.close();
        }
    }

    @Override
    public void onDataTreeChanged(@NonNull Collection<DataTreeModification<Node>> changes) {
        changes.forEach(this::onDataChanged);
    }

    private void onDataChanged(DataTreeModification<Node> change) {
        final DataObjectModification<Node> node = change.getRootNode();
        switch (node.getModificationType()) {
            case DELETE:
                LOG.info("************************************** Node Remove ***************************************");
                // LOG.info("NETCONF Node: {} was removed", node.getIdentifier());
                logChange("DELETE", requestGet());
                break;
            case SUBTREE_MODIFIED:
                // LOG.info("************************************** Node Modify
                // ************************************");
                // LOG.info("NETCONF Node: {} was updated", node.getIdentifier());
                //logChange("SUBTREE_MODIFIED", requestGet());
                break;
            case WRITE:
                LOG.info("************************************** Node Add *****************************************");
                // LOG.info("NETCONF Node: {} was created", node.getIdentifier());
                logChange("WRITE", requestGet());
                break;
            default:
                throw new IllegalStateException("Unhandled node change" + change);
        }
    }

    @Override
    public void onInitialData() {

    }

    private String requestGet() {
        try {
            Unirest.setTimeouts(0, 0);
            String url = "http://localhost:8181/restconf/operational/network-topology:network-topology/topology/flow:1";
            HttpResponse<String> response;
            response = Unirest.get(url).header("Accept", "application/json")
                    .header("Authorization", "Basic YWRtaW46YWRtaW4=").asString();
            JSONObject object = new JSONObject(response);
            return new JSONObject(object.getString("body")).toString();
        } catch (UnirestException e) {
            String error = e.getMessage();
            LOG.error(error);
            return "";
        }
    }

    @SuppressWarnings(value = { "DLS_DEAD_LOCAL_STORE", "DM_DEFAULT_ENCODING", "OS_OPEN_STREAM_EXCEPTION_PATH" })
    @SuppressFBWarnings(value = { "DLS_DEAD_LOCAL_STORE", "DM_DEFAULT_ENCODING", "OS_OPEN_STREAM_EXCEPTION_PATH" })
    private void createListIpFile() {
        try {
            String path = INIT_PATH;
            path = path + "/listip.json";
            FileOutputStream fos = new FileOutputStream(path);
            OutputStreamWriter wrt = new OutputStreamWriter(fos);
            String data = "{\n"
                    + "\t\"localIp\": \"192.168.50.137\",\n"
                    + "\t\"controller\": \"ODL\",\n"
                    + "\t\"communication\": [\n"
                    + "\t\t{\n"
                    + "\t\t\t\"ip\": \"192.168.50.137\",\n"
                    + "\t\t\t\"controller\": \"ODL\"\n"
                    + "\t\t},\n"
                    + "\t\t{\n"
                    + "\t\t\t\"ip\": \"192.168.50.137\",\n"
                    + "\t\t\t\"controller\": \"ODL\"\n"
                    + "\t\t}\n"
                    + "\t]\n"
                    + "}";
            wrt.write(data);
            wrt.close();
            fos.close();
        } catch (IOException e) {
            LOG.error("Error when create file listip.json");
        }

        // create version.json
        HandleVersion.createVersion();
    }

    public String getListIp() {
        String result = "";
        String path = INIT_PATH;

        InputStreamReader inputStreamReader = null;
        BufferedReader buffReader = null;
        try {
            inputStreamReader = new InputStreamReader(new FileInputStream(path + "/listip.json"),
                    StandardCharsets.UTF_8);
            buffReader = new BufferedReader(inputStreamReader);
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = buffReader.readLine()) != null) {
                content.append(inputLine);
            }
            result = content.toString();
        } catch (FileNotFoundException e) {
            LOG.error("Listip not found");
        } catch (IOException e) {
            LOG.error("Error when read file listip");
        } finally {
            try {
                if (buffReader != null) {
                    buffReader.close();
                }
                if (inputStreamReader != null) {
                    inputStreamReader.close();
                }
            } catch (IOException e) {
                LOG.error("Error when close read file listip");
            }
        }
        return result;
    }

    @SuppressWarnings(value = { "IP_PARAMETER_IS_DEAD_BUT_OVERWRITTEN" })
    @SuppressFBWarnings(value = { "IP_PARAMETER_IS_DEAD_BUT_OVERWRITTEN" })
    private void logChange(String eventType, String data) {
        //data = ""; //test reset
        String strJson = "{\"id\":\"" + java.util.UUID.randomUUID() + "\"," + "\"eventType\":\"" + eventType + "\","
                + "\"time\":\"" + java.time.LocalDateTime.now() + "\"," + "\"data\":" + data + "}\n";
        SinaProvider.LOGGING_QUEUE.add(strJson);
    }

    @SuppressFBWarnings(value = { "DLS_DEAD_LOCAL_STORE", "OS_OPEN_STREAM_EXCEPTION_PATH",
            "SLF4J_FORMAT_SHOULD_BE_CONST", "UPM_UNCALLED_PRIVATE_METHOD", "UWF_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD" })
    private void writeLogChange() {
        if (SinaProvider.lockFlag) {
            return;
        }
        String strLog = SinaProvider.LOGGING_QUEUE.poll();
        if (strLog != null) {
            LOG.info("write a log change");
            // lock
            SinaProvider.lockFlag = true;

            Writer outputStreamWriter = null;
            BufferedWriter bufferWriter = null;
            try {
                // update version
                int ver = HandleVersion.getVersion(myIpAddress);
                HandleVersion.setVersion(myIpAddress, ++ver);

                String path = INIT_PATH;
                outputStreamWriter = new OutputStreamWriter(
                        new FileOutputStream(path + "/" + myIpAddress + ".json", true), StandardCharsets.UTF_8);
                bufferWriter = new BufferedWriter(outputStreamWriter);
                bufferWriter.append(strLog);
            } catch (IOException e) {
                LOG.error("Error when write change log file");
            } finally {
                // unlock
                SinaProvider.lockFlag = false;
                try {
                    if (bufferWriter != null) {
                        bufferWriter.close();
                    }
                    if (outputStreamWriter != null) {
                        outputStreamWriter.close();
                    }
                } catch (IOException e) {
                    LOG.error("Error when close writer change log file");
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

    @SuppressWarnings(value = { "UWF_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD", "UPM_UNCALLED_PRIVATE_METHOD",
            "SLF4J_FORMAT_SHOULD_BE_CONST" })
    @SuppressFBWarnings(value = { "UWF_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD", "UPM_UNCALLED_PRIVATE_METHOD",
            "SLF4J_FORMAT_SHOULD_BE_CONST" })
    private void updateDataFromOtherCtrl() {
        if (SinaProvider.lockFlag) {
            return;
        }
        DataUpdateModel updateData = SinaProvider.UPDATE_DATA_QUEUE.poll();
        if (updateData != null) {
            //LOG.info("check update data " + updateData);
            // lock
            SinaProvider.lockFlag = true;
            int currVer = HandleVersion.getVersion(updateData.getIp());
            // nho hon thi cap nhat
            if (currVer < updateData.getVersion()) {
                LOG.info("start update data from ip: " + updateData.getIp());
                // update version
                HandleVersion.setVersion(updateData.getIp(), updateData.getVersion());
                // update data
                Writer outputStreamWriter = null;
                BufferedWriter bufferWriter = null;
                try {
                    String path = INIT_PATH;
                    outputStreamWriter = new OutputStreamWriter(
                            new FileOutputStream(path + "/" + updateData.getIp() + ".json", true),
                            StandardCharsets.UTF_8);
                    bufferWriter = new BufferedWriter(outputStreamWriter);
                    bufferWriter.append(updateData.getData());
                    LOG.info("update success data from ip:" + updateData.getIp());
                } catch (IOException e) {
                    LOG.error("Error when write data file");
                } finally {
                    try {
                        if (bufferWriter != null) {
                            bufferWriter.close();
                        }
                        if (outputStreamWriter != null) {
                            outputStreamWriter.close();
                        }
                    } catch (IOException e) {
                        LOG.error("Error when close writer update data");
                    }
                }
            }
            // unlock
            SinaProvider.lockFlag = false;
        }
    }

    private void scheduleUpdateData() {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateDataFromOtherCtrl();
            }
        }, 0, 150);
    }

    @SuppressWarnings(value = { "DLS_DEAD_LOCAL_STORE", "DM_DEFAULT_ENCODING", "OS_OPEN_STREAM_EXCEPTION_PATH",
            "SLF4J_FORMAT_SHOULD_BE_CONST", "UPM_UNCALLED_PRIVATE_METHOD" })
    @SuppressFBWarnings(value = { "DLS_DEAD_LOCAL_STORE", "DM_DEFAULT_ENCODING", "OS_OPEN_STREAM_EXCEPTION_PATH",
            "SLF4J_FORMAT_SHOULD_BE_CONST", "UPM_UNCALLED_PRIVATE_METHOD" })
    private void handleCommunicate() {
        int numMem = SinaProvider.MEMBER_LIST.size();
        if (numMem > 0) {
            Random rand = new Random();
            int index = rand.nextInt(numMem);
            InforControllerModel mem = SinaProvider.MEMBER_LIST.get(index);
            SinaProvider.MEMBER_LIST.remove(index);

            String strVer = HandleVersion.getVersions();
            if (mem == null || mem.getIp() == null || mem.getKindController() == null) {
                return;
            }
            switch (mem.getKindController()) {
                case "ONOS": {
                    try {
                        // JSONObject contentComp = new JSONObject();
                        // contentComp.put("ver", jsonVer);

                        // log.info(contentComp.toString());
                        HttpResponse<String> response = Unirest
                                .post("http://" + mem.getIp() + ":8181/onos/kcsg/communicate/compareVersions")
                                .header("Content-Type", "application/json").header("Accept", "application/json")
                                .header("Authorization", "Basic a2FyYWY6a2FyYWY=").body(strVer).asString();
                        if (response.getStatus() == 200) {
                            String body = response.getBody();
                            JSONArray datas = new JSONArray();
                            JSONArray arr = new JSONArray(body);

                            int len = arr.length();
                            for (int i = 0; i < len; i++) {
                                JSONObject resVerModel = arr.getJSONObject(i);
                                String resIp = resVerModel.getString("ip");
                                int resVer = resVerModel.getInt("ver");

                                int ver = HandleVersion.getVersion(resIp);
                                String data = HandleVersion.getDiffData(resIp, resVer);

                                JSONObject json = new JSONObject();
                                json.put("ip", resIp);
                                json.put("ver", ver);
                                json.put("data", data);
                                datas.put(json);
                            }
                            if (len > 0) {
                                HttpResponse<String> resUpdateData = Unirest
                                        .put("http://" + mem.getIp() + ":8181/onos/kcsg/communicate/updateNewLog")
                                        .header("Content-Type", "application/json").header("Accept", "application/json")
                                        .header("Authorization", "Basic a2FyYWY6a2FyYWY=").body(datas.toString())
                                        .asString();
                                if (resUpdateData.getStatus() == 200) {
                                    LOG.info("send update data success");
                                } else {
                                    LOG.warn("send update data fail with status code: " + resUpdateData.getStatus());
                                }
                            }
                        } else {
                            LOG.warn("compare version with status code: " + response.getStatus());
                        }
                    } catch (UnirestException e) {
                        LOG.error(e.getMessage());
                    } catch (JSONException e) {
                        LOG.error(e.getMessage());
                    }
                    break;
                }
                case "Faucet": {
                    LOG.info("http://" + mem.getIp() + ":8080/faucet/sina/versions/get-new");
                    String url = "http://" + mem.getIp() + ":8080/faucet/sina/versions/get-new";

                    try {
                        HttpResponse<String> response = Unirest.post(url).header("Content-Type", "application/json")
                                .header("Accept", "application/json").header("Authorization", "Basic a2FyYWY6a2FyYWY=")
                                .body(strVer).asString();
                        if (response.getStatus() == 200) {
                            String body = response.getBody();
                            LOG.info("BODY: " + body);
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
                                LOG.info("http://" + mem.getIp() + ":8080/faucet/sina/log/update");
                                HttpResponse<String> resUpdateData = Unirest
                                        .post("http://" + mem.getIp() + ":8080/faucet/sina/log/update")
                                        .header("Content-Type", "application/json").header("Accept", "application/json")
                                        .header("Authorization", "Basic a2FyYWY6a2FyYWY=").body(datas.toString())
                                        .asString();
                                if (resUpdateData.getStatus() == 200) {
                                    LOG.info("send update data success");
                                } else {
                                    LOG.warn("send update data fail with status code: " + resUpdateData.getStatus());
                                }
                            }
                        } else {
                            LOG.warn("compare version with status code: " + response.getStatus());
                        }
                    } catch (UnirestException e) {
                        LOG.error(e.getMessage());
                    } catch (JSONException e) {
                        LOG.error(e.getMessage());
                    }
                    break;
                }
                case "ODL": {
                    try {
                        JSONObject bodyComp = new JSONObject();
                        JSONObject verData = new JSONObject();
                        verData.put("data", strVer);
                        bodyComp.put("input", verData);

                        HttpResponse<String> response = Unirest
                                .post("http://" + mem.getIp() + ":8181/restconf/operations/sina:compareVersions")
                                .header("Content-Type", "application/json").header("Accept", "application/json")
                                .header("Authorization", "Basic YWRtaW46YWRtaW4=").body(bodyComp).asString();
                        if (response.getStatus() == 200) {
                            String resBody = response.getBody();
                            LOG.info("res compare versions body: " + resBody);

                            //{"output":{"ips":"[\"192.168.50.137\"]"}}
                            JSONObject resObj = new JSONObject(resBody);
                            JSONObject output = resObj.getJSONObject("output");
                            //chu y ips la kieu string khong phai array
                            JSONArray arr = new JSONArray(output.getString("ips"));

                            JSONArray datas = new JSONArray();
                            int len = arr.length();
                            for (int i = 0; i < len; i++) {
                                JSONObject resVerModel = arr.getJSONObject(i);
                                String resIp = resVerModel.getString("ip");
                                int resVer = resVerModel.getInt("ver");

                                int ver = HandleVersion.getVersion(resIp);
                                String data = HandleVersion.getDiffData(resIp, resVer);

                                JSONObject json = new JSONObject();
                                json.put("ip", resIp);
                                json.put("ver", ver);
                                json.put("data", data);
                                datas.put(json);
                            }

                            //LOG.info("datas " + datas.toString());
                            if (len > 0) {
                                JSONObject bodyUpdate = new JSONObject();
                                JSONObject dataUpdate = new JSONObject();
                                dataUpdate.put("data", datas.toString());
                                bodyUpdate.put("input", dataUpdate);

                                HttpResponse<String> resUpdateData = Unirest
                                        .post("http://" + mem.getIp() + ":8181/restconf/operations/sina:updateNewData")
                                        .header("Content-Type", "application/json").header("Accept", "application/json")
                                        .header("Authorization", "Basic YWRtaW46YWRtaW4=").body(bodyUpdate).asString();
                                if (resUpdateData.getStatus() == 200) {
                                    LOG.info("res update data body: " + resUpdateData.getBody());
                                } else {
                                    LOG.warn("send update data fail with status code: " + resUpdateData.getStatus());
                                }
                            }
                        } else {
                            LOG.warn("compare version with status code: " + response.getStatus());
                        }
                    } catch (UnirestException e) {
                        LOG.error(e.getMessage());
                    } catch (JSONException e) {
                        LOG.error(e.getMessage());
                    }
                    break;
                }
                default:
                    break;
            }
        } else {
            LOG.info("add member");
            SinaProvider.MEMBER_LIST.addAll(HandleVersion.getMembers());
        }
    }

    private void scheduleCommunicate() {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                handleCommunicate();
            }
        }, 0, 5000);
    }

    @SuppressWarnings(value = { "UC_USELESS_OBJECT", "SLF4J_FORMAT_SHOULD_BE_CONST" })
    @SuppressFBWarnings(value = { "UC_USELESS_OBJECT", "SLF4J_FORMAT_SHOULD_BE_CONST" })
    @Override
    public ListenableFuture<RpcResult<CompareVersionsOutput>> compareVersions(CompareVersionsInput input) {
        CompareVersionsOutputBuilder builder = new CompareVersionsOutputBuilder();
        JSONObject jsonObject = new JSONObject(input.getData());
        Iterator<String> keys = jsonObject.keys();
        ArrayList<VersionModel> versions = new ArrayList<>();
        while (keys.hasNext()) {
            String key = keys.next();
            VersionModel model = new VersionModel();
            model.setIp(key);
            model.setVer(jsonObject.getInt(key));
            versions.add(model);
        }

        ArrayList<VersionModel> ips = new ArrayList<>();
        for (VersionModel item : versions) {
            LOG.info("ip :" + item.getIp() + " current ver " + item.getVer());
            int currVer = HandleVersion.getVersion(item.getIp());
            // hien tai nho hon gui toi => can update
            if (currVer < item.getVer()) {
                ips.add(item);
            }
            // ips.add(item.ip);
        }
        // builder.setResult(value);

        JSONArray jsonArray = new JSONArray(ips);
        builder.setIps(jsonArray.toString());
        return RpcResultBuilder.success(builder.build()).buildFuture();
    }

    @SuppressWarnings(value = { "UC_USELESS_OBJECT", "SLF4J_FORMAT_SHOULD_BE_CONST" })
    @SuppressFBWarnings(value = { "UC_USELESS_OBJECT", "SLF4J_FORMAT_SHOULD_BE_CONST" })
    @Override
    public ListenableFuture<RpcResult<UpdateNewDataOutput>> updateNewData(UpdateNewDataInput input) {
        UpdateNewDataOutputBuilder builder = new UpdateNewDataOutputBuilder();
        try {
            JSONArray jsonArray = new JSONArray(input.getData());
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String ip = obj.getString("ip");
                int ver = obj.getInt("ver");
                String data = obj.getString("data");

                DataUpdateModel model = new DataUpdateModel();
                model.setIp(ip);
                model.setVersion(ver);
                model.setData(data);
                UPDATE_DATA_QUEUE.add(model);
            }
            builder.setResult("receive update data success");
        }
        catch (JSONException e) {
            LOG.error(e.getMessage());
            builder.setResult("receive update data faild");
        }
        return RpcResultBuilder.success(builder.build()).buildFuture();
    }
}
