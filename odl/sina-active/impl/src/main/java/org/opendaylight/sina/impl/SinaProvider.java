/*
 * Copyright © 2018 Copyright (c) 2018 Yoyodyne, Inc and others.  All rights reserved.
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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

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
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sina.rev200908.SimpleApiInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sina.rev200908.SimpleApiOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sina.rev200908.SimpleApiOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sina.rev200908.SinaService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sina.rev200908.UpdateDataInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sina.rev200908.UpdateDataOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sina.rev200908.UpdateDataOutputBuilder;
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

    final InstanceIdentifier<Node> instanceIdentifier = InstanceIdentifier
            .builder(Nodes.class).child(Node.class).build();

    @SuppressWarnings(value = { "MS_PKGPROTECT", "MS_SHOULD_BE_FINAL" })
    @SuppressFBWarnings(value = { "MS_PKGPROTECT", "MS_SHOULD_BE_FINAL" })
    public static String myIpAddress = null;
    @SuppressWarnings(value = { "MS_PKGPROTECT" })
    @SuppressFBWarnings(value = { "MS_PKGPROTECT" })
    public static String SERVER_URL = null;

    public static final String INIT_PATH = "/home/odl/sdn";

    public SinaProvider(final DataBroker dataBroker, RpcProviderService rpcProviderService) {
        this.dataBroker = dataBroker;
        this.rpcProviderService = rpcProviderService;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("SinaProvider Session Initiated");
        sinaService = rpcProviderService.registerRpcImplementation(SinaService.class, this);

        listenerRegistration = dataBroker.registerDataTreeChangeListener(
                DataTreeIdentifier.create(LogicalDatastoreType.OPERATIONAL, instanceIdentifier), this);


        createListIpFile();
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
    @SuppressWarnings(value = {"DLS_DEAD_LOCAL_STORE", "DM_DEFAULT_ENCODING", "OS_OPEN_STREAM_EXCEPTION_PATH"})
    @SuppressFBWarnings(value = {"DLS_DEAD_LOCAL_STORE", "DM_DEFAULT_ENCODING", "OS_OPEN_STREAM_EXCEPTION_PATH"})
    public ListenableFuture<RpcResult<UpdateDataOutput>> updateData(UpdateDataInput input) {
        UpdateDataOutputBuilder updateDataOutputBuilder = new UpdateDataOutputBuilder();
        try {
            assert input.getData() != null;
            JSONObject object = new JSONObject(input.getData());
            String ip = object.getString("ip");
            String kind = object.getString("kind");
            final String dataWrite = object.getString("data");
            String path = INIT_PATH;
            path = path + "/" + ip + "_" + kind + ".json";
            FileOutputStream fos = new FileOutputStream(path);
            OutputStreamWriter wrt = new OutputStreamWriter(fos);
            wrt.write(dataWrite);

            wrt.close();
            fos.close();

            updateDataOutputBuilder.setResult("Success");
            LOG.info("Update Data Success");

            JSONObject bodyReq = new JSONObject();
            bodyReq.put("ipSender", ip);
            bodyReq.put("ipReceiver", myIpAddress);
            bodyReq.put("time", java.time.LocalDateTime.now());
            bodyReq.put("version", 0);

            HttpResponse<String> response = Unirest
                .post(SERVER_URL + "/api/log/write")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .body(bodyReq).asString();
        } catch (IOException e) {
            String err = e.getMessage();
            updateDataOutputBuilder.setResult(err);
            LOG.info("Update Data Error");
        } catch (UnirestException e) {
            String err = e.getMessage();
            updateDataOutputBuilder.setResult(err);
            LOG.info("Call server Error");
        }
        return RpcResultBuilder.success(updateDataOutputBuilder.build()).buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<SimpleApiOutput>> simpleApi(SimpleApiInput input) {
        SimpleApiOutputBuilder helloBuilder = new SimpleApiOutputBuilder();
        helloBuilder.setOut("Hello " + input.getIn());
        LOG.info("Request Api success");
        return RpcResultBuilder.success(helloBuilder.build()).buildFuture();
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
//                LOG.info("NETCONF Node: {} was removed", node.getIdentifier());
                sendNotify(requestGet());
                break;
            case SUBTREE_MODIFIED:
//                LOG.info("************************************** Node Modify ************************************");
//                LOG.info("NETCONF Node: {} was updated", node.getIdentifier());
                break;
            case WRITE:
                LOG.info("************************************** Node Add *****************************************");
//                LOG.info("NETCONF Node: {} was created", node.getIdentifier());
                sendNotify(requestGet());
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
            response = Unirest.get(url)
                    .header("Accept", "application/json")
                    .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                    .asString();
            JSONObject object = new JSONObject(response);
            return new JSONObject(object.getString("body")).toString();
        } catch (UnirestException e) {
            String error = e.getMessage();
            LOG.error(error);
            return "";
        }
    }

    @SuppressWarnings(value = {"DLS_DEAD_LOCAL_STORE", "DM_DEFAULT_ENCODING",
            "OS_OPEN_STREAM_EXCEPTION_PATH", "SLF4J_FORMAT_SHOULD_BE_CONST"})
    @SuppressFBWarnings(value = {"DLS_DEAD_LOCAL_STORE", "DM_DEFAULT_ENCODING",
            "OS_OPEN_STREAM_EXCEPTION_PATH", "SLF4J_FORMAT_SHOULD_BE_CONST"})
    private void writeData(String data) {
        OutputStreamWriter outputStreamWriter = null;
        BufferedWriter bufferWriter = null;
        try {
            //String path = System.getProperty("java.io.tmpdir");
            String path = INIT_PATH;
            outputStreamWriter = new OutputStreamWriter(
                new FileOutputStream(path + "/data.json", true), StandardCharsets.UTF_8);
            bufferWriter = new BufferedWriter(outputStreamWriter);
            JSONObject json = new JSONObject();
            json.put("data", data);
            bufferWriter.append(json.toString());

            JSONObject bodyReq = new JSONObject();
            bodyReq.put("ipSender", myIpAddress);
            bodyReq.put("ipReceiver", myIpAddress);
            bodyReq.put("time", java.time.LocalDateTime.now());
            bodyReq.put("version", 0);

            HttpResponse<String> response = Unirest
                .post(SERVER_URL + "/api/log/write")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .body(bodyReq).asString();
        } catch (IOException e) {
            LOG.error("Error when write file data.json");
        } catch (UnirestException e) {
            LOG.error("Error when call api server");
        } finally {
            try {
                if (bufferWriter != null) {
                    bufferWriter.close();
                }
                if (outputStreamWriter != null) {
                    outputStreamWriter.close();
                }
            } catch (IOException e) {
                LOG.error("Error when close write file data.json");
            }
        }
    }

    @SuppressWarnings(value = {"DLS_DEAD_LOCAL_STORE", "DM_DEFAULT_ENCODING",
            "OS_OPEN_STREAM_EXCEPTION_PATH", "SLF4J_FORMAT_SHOULD_BE_CONST"})
    @SuppressFBWarnings(value = {"DLS_DEAD_LOCAL_STORE", "DM_DEFAULT_ENCODING",
            "OS_OPEN_STREAM_EXCEPTION_PATH", "SLF4J_FORMAT_SHOULD_BE_CONST"})
    private void sendNotify(String data) {
        writeData(data);
        try {
            String path = INIT_PATH;

            FileInputStream fis = new FileInputStream(path + "/listip.json");
            InputStreamReader rd = new InputStreamReader(fis);

            BufferedReader br = new BufferedReader(rd);
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = br.readLine()) != null) {
                content.append(inputLine);
            }
            br.close();

            JSONObject object = new JSONObject(new String(content));
            String localIp = object.getString("localIp");
            JSONArray array = object.getJSONArray("communication");

            JSONObject dataObject = new JSONObject();
            dataObject.put("ip", localIp);
            dataObject.put("kind", "topology");
            dataObject.put("data", data);

            int len = array.length();
            for (int i = 0; i < len; i++) {
                JSONObject controller = array.getJSONObject(i);
                String destIp = controller.getString("ip");
                String kindController = controller.getString("controller");

                switch (kindController) {
                    case "ONOS": {
                        final String urlOnos = "http://" + destIp + ":8181/onos/sina/updateInfo/updateData";

                        Unirest.setTimeouts(0, 0);
                        HttpResponse<String> responseOnos = Unirest.post(urlOnos)
                                .header("Content-Type", "application/json")
                                .header("Accept", "application/json")
                                .header("Authorization", "Basic a2FyYWY6a2FyYWY=")
                                .body(dataObject)
                                .asString();
                        LOG.info("Send Data to ONOS Success");
                        break;
                    }
                    case "Faucet": {
                        final String urlFaucet = "http://" + destIp + ":8080/faucet/sina/updateInfo/updateData";

                        Unirest.setTimeouts(0, 0);
                        HttpResponse<String> responseFaucet = Unirest.post(urlFaucet)
                                .header("Content-Type", "application/json")
                                .header("Accept", "application/json")
                                .body(dataObject)
                                .asString();
                        LOG.info("Send Data to Faucet Success");
                        break;
                    }
                    case "ODL": {
                        final String urlOdl = "http://" + destIp + ":8181/restconf/operations/sina:updateData";

                        JSONObject dataForm = new JSONObject();
                        JSONObject dataFinal = new JSONObject();
                        dataForm.put("data", dataObject.toString());
                        dataFinal.put("input", dataForm);

                        Unirest.setTimeouts(0, 0);
                        HttpResponse<String> responseOdl = Unirest.post(urlOdl)
                                .header("Content-Type", "application/json")
                                .header("Accept", "application/json")
                                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                                .body(dataFinal)
                                .asString();
                        LOG.info("Send Data to ODL Success");
                        break;
                    }
                    default: {
                        LOG.info("Send data Error");
                        break;
                    }
                }
            }


        } catch (IOException e) {
            LOG.error("Cannot find listip.json file");
        } catch (UnirestException e) {
            LOG.error("Error when call API");
        }
    }

    @SuppressWarnings(value = {"DLS_DEAD_LOCAL_STORE", "DM_DEFAULT_ENCODING", "OS_OPEN_STREAM_EXCEPTION_PATH",
        "SLF4J_FORMAT_SHOULD_BE_CONST"})
    @SuppressFBWarnings(value = {"DLS_DEAD_LOCAL_STORE", "DM_DEFAULT_ENCODING", "OS_OPEN_STREAM_EXCEPTION_PATH",
        "SLF4J_FORMAT_SHOULD_BE_CONST"})
    private void createListIpFile() {
        String path = INIT_PATH;
        InputStreamReader inputStreamReader = null;
        BufferedReader buffReader = null;
        try {
            inputStreamReader = new InputStreamReader(
                new FileInputStream(path + "/config.json"), StandardCharsets.UTF_8
            );
            buffReader = new BufferedReader(inputStreamReader);
            String line;

            StringBuilder listIpBuilder = new StringBuilder();

            while ((line = buffReader.readLine()) != null) {
                listIpBuilder.append(line);
            }

            JSONObject object = new JSONObject(listIpBuilder.toString());
            SERVER_URL = object.getString("serverUrl");
        } catch (IOException e) {
            // log.error(e.getMessage());
        } finally {
            try {
                if (buffReader != null) {
                    buffReader.close();
                }
                if (inputStreamReader != null) {
                    inputStreamReader.close();
                }
            } catch (IOException e) {
                // log.error(e.getMessage());
            }
        }

        FileOutputStream fos = null;
        OutputStreamWriter wrt = null;
        try {
            fos = new FileOutputStream(path + "/listip.json");
            wrt = new OutputStreamWriter(fos, StandardCharsets.UTF_8);

            File fileListIp = new File(path + "/listip.json");

            try {
                HttpResponse<String> response = Unirest.get(SERVER_URL + "/api/remoteIp/list-ip")
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .asString();

                if (response.getStatus() == 200) {
                    wrt.write(response.getBody());
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

        InputStreamReader inputStreamReader2 = null;
        BufferedReader buffReader2 = null;
        try {
            inputStreamReader2 = new InputStreamReader(
                new FileInputStream(path + "/listip.json"), StandardCharsets.UTF_8
            );
            buffReader2 = new BufferedReader(inputStreamReader2);
            String line;

            StringBuilder listIpBuilder = new StringBuilder();

            while ((line = buffReader2.readLine()) != null) {
                listIpBuilder.append(line);
            }

            JSONObject object = new JSONObject(listIpBuilder.toString());
            myIpAddress = object.getString("localIp");
        } catch (IOException e) {
            LOG.error("Error when read localIp in listip");
        } finally {
            try {
                if (buffReader2 != null) {
                    buffReader2.close();
                }
                if (inputStreamReader2 != null) {
                    inputStreamReader2.close();
                }
            } catch (IOException e) {
                // log.error(e.getMessage());
            }
        }
        LOG.info("myIp :" + myIpAddress + " serverUrl: " + SERVER_URL);
    }
}
