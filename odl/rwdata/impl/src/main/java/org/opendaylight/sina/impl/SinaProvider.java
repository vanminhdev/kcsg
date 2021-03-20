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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.jdt.annotation.NonNull;
import org.json.JSONArray;
import org.json.JSONObject;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.sina.impl.models.ConfigRWModel;
import org.opendaylight.sina.impl.models.InforControllerModel;
import org.opendaylight.sina.impl.models.ResultReadModel;
import org.opendaylight.sina.impl.models.ResultWriteModel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sina.rev200908.GetVersionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sina.rev200908.GetVersionOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sina.rev200908.GetVersionOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sina.rev200908.SinaService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sina.rev200908.UpdateVersionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sina.rev200908.UpdateVersionOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sina.rev200908.UpdateVersionOutputBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SinaProvider implements SinaService, DataTreeChangeListener<Node> {
    private static final Logger LOG = LoggerFactory.getLogger(SinaProvider.class);
    private static final String MSG = "msg: {}";

    private final DataBroker dataBroker;
    private ObjectRegistration<SinaService> sinaService;
    private final RpcProviderService rpcProviderService;
    private ListenerRegistration<?> listenerRegistration;

    @SuppressWarnings(value = { "MS_PKGPROTECT" })
    @SuppressFBWarnings(value = { "MS_PKGPROTECT" })
    public static String myIpAddress = null;
    @SuppressWarnings(value = { "MS_PKGPROTECT" })
    @SuppressFBWarnings(value = { "MS_PKGPROTECT" })
    public static String SERVER_URL = null;

    public static final String INIT_PATH = "/home/onos/sdn";

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
        LOG.info(MSG, "SinaProvider Session Initiated");
        sinaService = rpcProviderService.registerRpcImplementation(SinaService.class, this);

        listenerRegistration = dataBroker.registerDataTreeChangeListener(
                DataTreeIdentifier.create(LogicalDatastoreType.OPERATIONAL, instanceIdentifier), this);

        createListIpFile();
        scheduleCommunicate();
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info(MSG, "SinaProvider Closed");
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
                LOG.info(MSG, "********************** Node Remove ***************");
                // LOG.info(MSG, "NETCONF Node: {} was removed", node.getIdentifier());
                logChange("DELETE", requestGet());
                break;
            case SUBTREE_MODIFIED:
                // LOG.info(MSG, "****************** Node Modify
                // ***************");
                // LOG.info(MSG, "NETCONF Node: {} was updated", node.getIdentifier());
                //logChange("SUBTREE_MODIFIED", requestGet());
                break;
            case WRITE:
                LOG.info(MSG, "********************* Node Add ************************");
                // LOG.info(MSG, "NETCONF Node: {} was created", node.getIdentifier());
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

    @SuppressWarnings(value = { "DLS_DEAD_LOCAL_STORE", "DM_DEFAULT_ENCODING", "OS_OPEN_STREAM_EXCEPTION_PATH",
        "SLF4J_FORMAT_SHOULD_BE_CONST" })
    @SuppressFBWarnings(value = { "DLS_DEAD_LOCAL_STORE", "DM_DEFAULT_ENCODING", "OS_OPEN_STREAM_EXCEPTION_PATH",
        "SLF4J_FORMAT_SHOULD_BE_CONST"})
    private void createListIpFile() {
        SERVER_URL = HandleVersion.getServerUrl();
        String path = INIT_PATH;
        path = path + "/listip.json";
        FileOutputStream fos = null;
        OutputStreamWriter wrt = null;
        try {
            fos = new FileOutputStream(path);
            wrt = new OutputStreamWriter(fos, StandardCharsets.UTF_8);

            File fileListIp = new File(path);

            try {
                HttpResponse<String> response = Unirest
                    .get(SERVER_URL + "/api/remoteIp/list-ip")
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .asString();

                if (response.getStatus() == 200) {
                    LOG.info(MSG, "list_ip" + response.getBody());
                    wrt.write(response.getBody());
                } else {
                    LOG.info(MSG, "list_ip" + response.getStatus());
                }
            } catch (UnirestException e) {
                if (!fileListIp.exists()) {
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
            LOG.error("Error when create file listip.json");
        } finally {
            try {
                if (wrt != null) {
                    wrt.close();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                LOG.error("Error when close write file listip");
            }
        }
        //create version.json
        HandleVersion.createVersion();
        var local = HandleVersion.getLocal();
        if (local != null) {
            myIpAddress = local.getIp();
        }
        LOG.info(MSG, "myIp :" + myIpAddress + " serverUrl: " + SERVER_URL);
    }

    @SuppressWarnings(value = { "IP_PARAMETER_IS_DEAD_BUT_OVERWRITTEN" })
    @SuppressFBWarnings(value = { "IP_PARAMETER_IS_DEAD_BUT_OVERWRITTEN" })
    private void logChange(String eventType, String data) {
        //data = ""; //test reset
        String strJson = "{\"id\":\"" + java.util.UUID.randomUUID() + "\"," + "\"eventType\":\"" + eventType + "\","
            + "\"time\":\"" + java.time.LocalDateTime.now() + "\"," + "\"data\":" + data + "}\n";
        writeLogChange(strJson);
    }

    private void writeLogChange(String strLog) {
        LOG.info(MSG, "write a log change");
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
            if (ip.equals(myIpAddress)) {
                logDetail.put("length", 0);
            } else {
                result = handleWrite(ip, version, dstController);
            }
            if (result != null) {
                logDetail.put("length", result.getLength());
            }
            logDetail.put("end", java.time.LocalDateTime.now());
            log.put(logDetail);
        }
        HandleCallServer.sendLogWrite(log);
    }


    @SuppressWarnings(value = { "DM_DEFAULT_ENCODING" })
    @SuppressFBWarnings(value = { "DM_DEFAULT_ENCODING" })
    private ResultWriteModel handleWrite(String srcIp, int srcVersion, InforControllerModel desCtrller) {
        switch (desCtrller.getKindController()) {
            case "ONOS": {
                try {
                    JSONObject bodyReq = new JSONObject();
                    bodyReq.put("ip", srcIp);
                    bodyReq.put("version", srcVersion);
                    HttpResponse<String> response = Unirest
                        .post("http://" + desCtrller.getIp() + ":8181/onos/rwdata/communicate/update-version")
                        .header("Content-Type", "application/json").header("Accept", "application/json")
                        .header("Authorization", "Basic a2FyYWY6a2FyYWY=").body(bodyReq).asString();
                    ResultWriteModel result = new ResultWriteModel();
                    if (response.getStatus() == 200) {
                        LOG.info(MSG, "update success in controller: " + desCtrller.getIp());
                    } else {
                        LOG.warn(MSG, "update version in controller: " + desCtrller.getIp() + " with status code: "
                            + response.getStatus());
                    }
                    result.setLength(bodyReq.toString().getBytes().length);
                    return result;
                } catch (UnirestException e) {
                    LOG.error(MSG, e.getMessage());
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
                        .header("Content-Type", "application/json").header("Accept", "application/json")
                        .header("Authorization", "Basic a2FyYWY6a2FyYWY=").body(bodyReq).asString();

                    ResultWriteModel result = new ResultWriteModel();
                    if (response.getStatus() == 200) {
                        LOG.info(MSG, "update success in controller: " + desCtrller.getIp());
                    } else {
                        LOG.warn(MSG, "read version in controller: " + desCtrller.getIp() + " with status code: "
                                + response.getStatus());
                    }
                    result.setLength(bodyReq.toString().getBytes().length);
                    return result;
                } catch (UnirestException e) {
                    LOG.error(MSG, e.getMessage());
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
                        LOG.info(MSG, "update success in controller: " + desCtrller.getIp());
                    } else {
                        LOG.warn(MSG, "read version in controller: " + desCtrller.getIp() + " with status code: "
                            + response.getStatus());
                    }
                    result.setLength(bodyReq.toString().getBytes().length);
                    return result;
                } catch (UnirestException e) {
                    LOG.error(MSG, e.getMessage());
                }
                break;
            }
            default:
                break;
        }
        return null;
    }

    @SuppressWarnings(value = { "UPM_UNCALLED_PRIVATE_METHOD" })
    @SuppressFBWarnings(value = { "UPM_UNCALLED_PRIVATE_METHOD" })
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
            if (controllerTarget.getIp().equals(myIpAddress)) {
                logDetail.put("isSuccess", true);
                logDetail.put("length", 0);
            } else {
                result = handleRead(controllerTarget, dstController, versionFromServer);
            }
            if (result != null) {
                logDetail.put("isSuccess", false);
                logDetail.put("length", result.getLength());
            }
            logDetail.put("end", java.time.LocalDateTime.now());
            log.put(logDetail);
        }
        HandleCallServer.sendLogRead(log);
    }

    @SuppressWarnings(value = { "DM_DEFAULT_ENCODING" })
    @SuppressFBWarnings(value = { "DM_DEFAULT_ENCODING" })
    private ResultReadModel handleRead(InforControllerModel srcCtrller, InforControllerModel desCtrller, int srcVer) {
        switch (desCtrller.getKindController()) {
            case "ONOS": {
                try {
                    JSONObject bodyReq = new JSONObject();
                    bodyReq.put("ip", srcCtrller.getIp());
                    HttpResponse<String> response = Unirest
                        .post("http://" + desCtrller.getIp() + ":8181/onos/rwdata/communicate/get-version")
                        .header("Content-Type", "application/json").header("Accept", "application/json")
                        .header("Authorization", "Basic a2FyYWY6a2FyYWY=").body(bodyReq).asString();
                    ResultReadModel result = new ResultReadModel();
                    if (response.getStatus() == 200) {
                        JSONObject resBody = new JSONObject(response.getBody());
                        result.setSuccess(resBody.getInt("version") == srcVer);
                    } else {
                        result.setSuccess(false);
                        LOG.warn(MSG, "read version in controller: " + desCtrller.getIp() + " with status code: "
                                + response.getStatus());
                    }
                    result.setLength(bodyReq.toString().getBytes().length);
                    return result;
                } catch (UnirestException e) {
                    LOG.error(MSG, e.getMessage());
                }
                break;
            }
            case "Faucet": {
                try {
                    JSONObject bodyReq = new JSONObject();
                    bodyReq.put("ip", srcCtrller.getIp());
                    HttpResponse<String> response = Unirest
                        .post("http://" + desCtrller.getIp() + ":8080/faucet/sina/versions/get-version")
                        .header("Content-Type", "application/json").header("Accept", "application/json")
                        .header("Authorization", "Basic a2FyYWY6a2FyYWY=").body(bodyReq).asString();

                    ResultReadModel result = new ResultReadModel();
                    if (response.getStatus() == 200) {
                        JSONObject resBody = new JSONObject(response.getBody());
                        LOG.info(MSG, "BODY: " + resBody);
                        result.setSuccess(resBody.getInt("version") == srcVer);
                    } else {
                        result.setSuccess(false);
                        LOG.warn(MSG, "read version in controller: " + desCtrller.getIp() + " with status code: "
                            + response.getStatus());
                    }
                    result.setLength(bodyReq.toString().getBytes().length);
                    return result;
                } catch (UnirestException e) {
                    LOG.error(MSG, e.getMessage());
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
                        LOG.warn(MSG, "read version in controller: " + desCtrller.getIp() + " with status code: "
                            + response.getStatus());
                    }
                    result.setLength(bodyReq.toString().getBytes().length);
                    return result;
                } catch (UnirestException e) {
                    LOG.error(MSG, e.getMessage());
                }
                break;
            }
            default:
                break;
        }
        return null;
    }

    private void scheduleCommunicate() {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                readData();
            }
        }, 0, 5000);
    }

    @Override
    public ListenableFuture<RpcResult<GetVersionOutput>> getVersion(GetVersionInput input) {
        JSONObject jsonObject = new JSONObject(input.getData());
        LOG.info(MSG, jsonObject.toString());
        int version = HandleVersion.getVersion(jsonObject.getString("ip"));
        LOG.info(MSG, version);
        JSONObject jsonResult = new JSONObject();
        jsonResult.put("version", version);
        GetVersionOutputBuilder builder = new GetVersionOutputBuilder();
        builder.setResult(jsonResult.toString());
        return RpcResultBuilder.success(builder.build()).buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<UpdateVersionOutput>> updateVersion(UpdateVersionInput input) {
        UpdateVersionOutputBuilder builder = new UpdateVersionOutputBuilder();
        JSONObject jsonObject = new JSONObject(input.getData());
        HandleVersion.setVersion(jsonObject.getString("ip"), jsonObject.getInt("version"));
        builder.setResult("success");
        return RpcResultBuilder.success(builder.build()).buildFuture();
    }
}
