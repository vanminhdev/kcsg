/*
 * Copyright © 2021 Copyright (c) 2021 Yoyodyne, Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sina.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;
import org.opendaylight.sina.impl.models.InforControllerModel;

public final class HandleVersion {
    private static String INIT_PATH = "/tmp";

    private HandleVersion() {
    }

    public static void createVersion() {
        String path = INIT_PATH;
        JSONObject jsonVersion = new JSONObject();

        InforControllerModel local = getLocal();
        if (local != null) {
            jsonVersion.put(local.ip, 0);
        }

        ArrayList<InforControllerModel> mems = getMembers();
        for (InforControllerModel mem : mems) {
            jsonVersion.put(mem.ip, 0);
        }
        Writer outputStreamWriter = null;
        BufferedWriter bufferWriter = null;
        try {
            outputStreamWriter = new OutputStreamWriter(
                new FileOutputStream(path + "/version.json", false),
                StandardCharsets.UTF_8
            );
            bufferWriter = new BufferedWriter(outputStreamWriter);
            bufferWriter.write(jsonVersion.toString());
        } catch (IOException e) {
            //log.error(e.getMessage(), e);
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
                //log.error(e.getMessage(), e);
            }
        }
    }

    public static int getVersion(String ip) {
        String path = INIT_PATH;

        InputStreamReader inputStreamReader = null;
        BufferedReader buffReader = null;
        try {
            inputStreamReader = new InputStreamReader(
                new FileInputStream(path + "/version.json"), StandardCharsets.UTF_8
            );
            buffReader = new BufferedReader(inputStreamReader);
            String line;

            while ((line = buffReader.readLine()) != null) {
                JSONObject obj = new JSONObject(line);
                return obj.getInt(ip);
            }
        } catch (IOException e) {
            //log.error(e.getMessage());
        } finally {
            try {
                if (buffReader != null) {
                    buffReader.close();
                }
                if (inputStreamReader != null) {
                    inputStreamReader.close();
                }
            } catch (IOException e) {
                //log.error(e.getMessage());
            }
        }
        return -1;
    }

    public static String getVersions() {
        String path = INIT_PATH;

        InputStreamReader inputStreamReader = null;
        BufferedReader buffReader = null;
        try {
            inputStreamReader = new InputStreamReader(
                new FileInputStream(path + "/version.json"), StandardCharsets.UTF_8
            );
            buffReader = new BufferedReader(inputStreamReader);
            String line = buffReader.readLine();
            return line;
        } catch (IOException e) {
            //log.error(e.getMessage());
        } finally {
            try {
                if (buffReader != null) {
                    buffReader.close();
                }
                if (inputStreamReader != null) {
                    inputStreamReader.close();
                }
            } catch (IOException e) {
                //log.error(e.getMessage());
            }
        }
        return null;
    }

    public static void setVersion(String ip, int version) {
        String path = INIT_PATH;
        JSONObject jsonVersion = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader buffReader = null;
        try {
            inputStreamReader = new InputStreamReader(
                new FileInputStream(path + "/version.json"), StandardCharsets.UTF_8
            );
            buffReader = new BufferedReader(inputStreamReader);
            String line;

            while ((line = buffReader.readLine()) != null) {
                jsonVersion = new JSONObject(line);
            }
        } catch (IOException e) {
            //log.error(e.getMessage(), e);
        } finally {
            try {
                if (buffReader != null) {
                    buffReader.close();
                }
                if (inputStreamReader != null) {
                    inputStreamReader.close();
                }
            } catch (IOException e) {
                //log.error(e.getMessage(), e);
            }
        }

        if (jsonVersion == null) {
            return;
        }
        jsonVersion.put(ip, version);

        Writer outputStreamWriter = null;
        BufferedWriter bufferWriter = null;
        try {
            outputStreamWriter = new OutputStreamWriter(
                new FileOutputStream(path + "/version.json", false),
                StandardCharsets.UTF_8
            );
            bufferWriter = new BufferedWriter(outputStreamWriter);
            bufferWriter.write(jsonVersion.toString());
        } catch (IOException e) {
            //log.error(e.getMessage(), e);
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
                //log.error(e.getMessage(), e);
            }
        }
    }

    public static String getData(String ip) {
        String path = INIT_PATH;

        InputStreamReader inputStreamReader = null;
        BufferedReader buffReader = null;
        try {
            inputStreamReader = new InputStreamReader(
                new FileInputStream(path + "/" + ip + ".json"), StandardCharsets.UTF_8
            );
            buffReader = new BufferedReader(inputStreamReader);
            String line;
            StringBuilder strBuilder = new StringBuilder();
            while ((line = buffReader.readLine()) != null) {
                strBuilder.append(line);
            }
            return strBuilder.toString();
        } catch (IOException e) {
            //log.error(e.getMessage());
        } finally {
            try {
                if (buffReader != null) {
                    buffReader.close();
                }
                if (inputStreamReader != null) {
                    inputStreamReader.close();
                }
            } catch (IOException e) {
                //log.error(e.getMessage());
            }
        }
        return null;
    }

    public static String getDiffData(String ip, int oldVer) {
        String path = INIT_PATH;

        InputStreamReader inputStreamReader = null;
        BufferedReader buffReader = null;
        try {
            inputStreamReader = new InputStreamReader(
                new FileInputStream(path + "/" + ip + ".json"), StandardCharsets.UTF_8
            );
            buffReader = new BufferedReader(inputStreamReader);
            String line;
            StringBuilder strBuilder = new StringBuilder();
            int numRow = 0;
            while ((line = buffReader.readLine()) != null) {
                numRow++;
                if (numRow > oldVer) {
                    strBuilder.append(line);
                }
            }
            return strBuilder.toString();
        } catch (IOException e) {
            //log.error(e.getMessage());
        } finally {
            try {
                if (buffReader != null) {
                    buffReader.close();
                }
                if (inputStreamReader != null) {
                    inputStreamReader.close();
                }
            } catch (IOException e) {
                //log.error(e.getMessage());
            }
        }
        return null;
    }

    public static InforControllerModel getLocal() {
        InforControllerModel infor = null;
        String path = INIT_PATH;

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
            infor.ip = localIp;
            infor.kindController = kindController;
        } catch (IOException e) {
            //log.error(e.getMessage());
        } finally {
            try {
                if (buffReader != null) {
                    buffReader.close();
                }
                if (inputStreamReader != null) {
                    inputStreamReader.close();
                }
            } catch (IOException e) {
                //log.error(e.getMessage());
            }
        }
        return infor;
    }

    public static ArrayList<InforControllerModel> getMembers() {
        ArrayList<InforControllerModel> mems = new ArrayList<>();

        String path = INIT_PATH;

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
                model.ip = destIp;
                model.kindController = kindController;

                mems.add(model);
            }
        } catch (IOException e) {
            //log.error(e.getMessage());
        } finally {
            try {
                if (buffReader != null) {
                    buffReader.close();
                }
                if (inputStreamReader != null) {
                    inputStreamReader.close();
                }
            } catch (IOException e) {
                //log.error(e.getMessage());
            }
        }
        return mems;
    }

    public static String getKindController(String ip) {
        String path = INIT_PATH;

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
                if (destIp.equals(ip)) {
                    String kindController = controller.getString("controller");
                    return kindController;
                }
            }
        } catch (IOException e) {
            //log.error(e.getMessage());
        } finally {
            try {
                if (buffReader != null) {
                    buffReader.close();
                }
                if (inputStreamReader != null) {
                    inputStreamReader.close();
                }
            } catch (IOException e) {
                //log.error(e.getMessage());
            }
        }
        return null;
    }
}

