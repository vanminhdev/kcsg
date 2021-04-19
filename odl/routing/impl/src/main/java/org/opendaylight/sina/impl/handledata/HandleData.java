/*
 * Copyright © 2021 Copyright (c) 2021 Yoyodyne, Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sina.impl.handledata;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

import org.opendaylight.sina.impl.handledata.models.CusDevice;
import org.opendaylight.sina.impl.handledata.models.CusHost;
import org.opendaylight.sina.impl.handledata.models.CusLink;
import org.opendaylight.sina.impl.handledata.models.CusTopo;
import org.opendaylight.sina.impl.handledata.models.InforControllerModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HandleData {
    private static String initPath = "/home/onos/sdn";
    private static final Logger LOG = LoggerFactory.getLogger(HandleData.class);
    private static final String MSG = "handle data: {}";

    public HandleData() {
    }

    public void writeTopo() {
        // final Iterable<Device> devices = null;
        JSONArray jsonDevices = new JSONArray();
        // for (Device device : devices) {
        //     JSONObject jsonDevice = new JSONObject();
        //     jsonDevice.put("id", device.id().toString());
        //     jsonDevice.put("type", device.type().name());
        //     jsonDevices.put(jsonDevice);
        // }

        //host
        // final Iterable<Host> hosts = null;
        JSONArray jsonHosts = new JSONArray();
        // for (Host host : hosts) {
        //     JSONObject jsonHost = new JSONObject();
        //     jsonHost.put("id", host.id().mac().toString());
        //     jsonHost.put("deviceId", host.location().deviceId().toString());
        //     jsonHost.put("port", host.location().port().toLong());
        //     jsonHosts.put(jsonHost);
        // }

        //link
        // Iterable<Link> links = null;
        JSONArray jsonLinks = new JSONArray();
        // for (Link link : links) {
        //     JSONObject jsonSrc = new JSONObject();
        //     jsonSrc.put("id", link.src().deviceId().toString());
        //     jsonSrc.put("port", link.src().port().toLong());

        //     JSONObject jsonDst = new JSONObject();
        //     jsonDst.put("id", link.dst().deviceId().toString());
        //     jsonDst.put("port", link.dst().port().toLong());

        //     JSONObject jsonLink = new JSONObject();
        //     jsonLink.put("src", jsonSrc);
        //     jsonLink.put("dst", jsonDst);

        //     jsonLinks.put(jsonLink);
        // }

        JSONObject jsonTopo = new JSONObject();
        jsonTopo.put("hosts", jsonHosts);
        jsonTopo.put("devices", jsonDevices);
        jsonTopo.put("links", jsonLinks);

        OutputStreamWriter outputStreamWriter = null;
        BufferedWriter bufferWriter = null;
        try {
            InforControllerModel local = getLocal();
            outputStreamWriter = new OutputStreamWriter(
                new FileOutputStream(initPath + "/" + local.getIp() + ".json", false),
                StandardCharsets.UTF_8);
            bufferWriter = new BufferedWriter(outputStreamWriter);

            bufferWriter.write(jsonTopo.toString());
        } catch (IOException e) {
            LOG.error(MSG, e.getMessage());
        } finally {
            try {
                if (bufferWriter != null) {
                    bufferWriter.close();
                }
                if (outputStreamWriter != null) {
                    outputStreamWriter.close();
                }
            } catch (IOException e) {
                LOG.error(MSG, e.getMessage());
            }
        }
    }

    public static CusTopo getTopo(String ip) {
        CusTopo cusTopo = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader buffReader = null;
        try {
            inputStreamReader = new InputStreamReader(
                new FileInputStream(initPath + "/" + ip + ".json"), StandardCharsets.UTF_8
            );
            buffReader = new BufferedReader(inputStreamReader);
            String line;
            StringBuffer buffer = new StringBuffer();
            while ((line = buffReader.readLine()) != null) {
                buffer.append(line);
            }
            JSONObject jsonTopo = new JSONObject(buffer.toString());
            JSONArray jsonHosts = jsonTopo.getJSONArray("hosts");
            JSONArray jsonDevices = jsonTopo.getJSONArray("devices");
            JSONArray jsonLinks = jsonTopo.getJSONArray("links");

            ArrayList<CusHost> hosts = new ArrayList<>();
            ArrayList<CusDevice> devices = new ArrayList<>();
            ArrayList<CusLink> links = new ArrayList<>();

            for (int i = 0; i < jsonHosts.length(); i++) {
                JSONObject jsonHost = jsonHosts.getJSONObject(i);
                String id = jsonHost.getString("id");
                int port = jsonHost.getInt("port");
                String deviceId = jsonHost.getString("deviceId");
                CusHost cusHost = new CusHost(port, id, deviceId);
                hosts.add(cusHost);
            }

            for (int i = 0; i < jsonDevices.length(); i++) {
                JSONObject jsonDevice = jsonDevices.getJSONObject(i);
                String id = jsonDevice.getString("id");
                String type = jsonDevice.getString("type");
                CusDevice cusDevice = new CusDevice(id, type);
                devices.add(cusDevice);
            }

            for (int i = 0; i < jsonLinks.length(); i++) {
                JSONObject jsonLink = jsonLinks.getJSONObject(i);

                JSONObject jsonSrc = jsonLink.getJSONObject("src");
                JSONObject jsonDst = jsonLink.getJSONObject("dst");

                String idSrc = jsonSrc.getString("id");
                int portSrc = jsonSrc.getInt("port");

                String idDst = jsonDst.getString("id");
                int portDst = jsonDst.getInt("port");

                CusLink cusLink = new CusLink(idSrc, portSrc, idDst, portDst);
                links.add(cusLink);
            }
            cusTopo = new CusTopo();
            cusTopo.setHosts(hosts);
            cusTopo.setDevices(devices);
            cusTopo.setLinks(links);
        } catch (IOException e) {
            LOG.error(MSG, e.getMessage());
        } finally {
            try {
                if (buffReader != null) {
                    buffReader.close();
                }
                if (inputStreamReader != null) {
                    inputStreamReader.close();
                }
            } catch (IOException e) {
                LOG.error(MSG, e.getMessage());
            }
        }
        return cusTopo;
    }

    public static InforControllerModel getLocal() {
        InforControllerModel infor = null;
        String path = initPath;

        InputStreamReader inputStreamReader = null;
        BufferedReader buffReader = null;
        try {
            inputStreamReader = new InputStreamReader(
                new FileInputStream(path + "/listip.json"), StandardCharsets.UTF_8
            );
            buffReader = new BufferedReader(inputStreamReader);
            String line;

            StringBuilder listIpBuilder = new StringBuilder();

            while ((line = buffReader.readLine()) != null) {
                listIpBuilder.append(line);
            }

            JSONObject object = new JSONObject(listIpBuilder.toString());
            String localIp = object.getString("localIp");
            String kindController = object.getString("controller");
            infor = new InforControllerModel();
            infor.setIp(localIp);
            infor.setKindController(kindController);
        } catch (IOException e) {
            LOG.error(MSG, e.getMessage());
        } finally {
            try {
                if (buffReader != null) {
                    buffReader.close();
                }
                if (inputStreamReader != null) {
                    inputStreamReader.close();
                }
            } catch (IOException e) {
                LOG.error(MSG, e.getMessage());
            }
        }
        return infor;
    }

    public static ArrayList<InforControllerModel> getMembers() {
        ArrayList<InforControllerModel> mems = new ArrayList<>();

        String path = initPath;

        InputStreamReader inputStreamReader = null;
        BufferedReader buffReader = null;
        try {
            inputStreamReader = new InputStreamReader(
                new FileInputStream(path + "/listip.json"), StandardCharsets.UTF_8
            );
            buffReader = new BufferedReader(inputStreamReader);
            String line;

            StringBuilder listIpBuilder = new StringBuilder();

            while ((line = buffReader.readLine()) != null) {
                listIpBuilder.append(line);
            }

            JSONObject object = new JSONObject(listIpBuilder.toString());
            JSONArray arr = object.getJSONArray("communication");
            int len = arr.length();

            for (int i = 0; i < len; i++) {
                JSONObject controller = arr.getJSONObject(i);
                String destIp = controller.getString("ip");
                String kindController = controller.getString("controller");

                InforControllerModel model = new InforControllerModel();
                model.setIp(destIp);
                model.setKindController(kindController);

                mems.add(model);
            }
        } catch (IOException e) {
            LOG.error(MSG, e.getMessage());
        } finally {
            try {
                if (buffReader != null) {
                    buffReader.close();
                }
                if (inputStreamReader != null) {
                    inputStreamReader.close();
                }
            } catch (IOException e) {
                LOG.error(MSG, e.getMessage());
            }
        }
        return mems;
    }

    public static ArrayList<InforControllerModel> getAllController() {
        ArrayList<InforControllerModel> controllers = new ArrayList<>();

        String path = initPath;

        InputStreamReader inputStreamReader = null;
        BufferedReader buffReader = null;
        try {
            inputStreamReader = new InputStreamReader(
                new FileInputStream(path + "/listip.json"), StandardCharsets.UTF_8
            );
            buffReader = new BufferedReader(inputStreamReader);
            String line;

            StringBuilder listIpBuilder = new StringBuilder();

            while ((line = buffReader.readLine()) != null) {
                listIpBuilder.append(line);
            }

            JSONObject object = new JSONObject(listIpBuilder.toString());
            InforControllerModel local = new InforControllerModel();
            local.setIp(object.getString("localIp"));
            local.setKindController(object.getString("controller"));
            controllers.add(local);

            JSONArray arr = object.getJSONArray("communication");
            int len = arr.length();

            for (int i = 0; i < len; i++) {
                JSONObject controller = arr.getJSONObject(i);
                String destIp = controller.getString("ip");
                String kindController = controller.getString("controller");

                InforControllerModel model = new InforControllerModel();
                model.setIp(destIp);
                model.setKindController(kindController);

                controllers.add(model);
            }
        } catch (IOException e) {
            LOG.error(MSG, e.getMessage());
        } finally {
            try {
                if (buffReader != null) {
                    buffReader.close();
                }
                if (inputStreamReader != null) {
                    inputStreamReader.close();
                }
            } catch (IOException e) {
                //LOG.error(MSG, e.getMessage());
            }
        }
        return controllers;
    }
}
