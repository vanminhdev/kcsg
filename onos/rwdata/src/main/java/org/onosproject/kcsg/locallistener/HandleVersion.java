package org.onosproject.kcsg.locallistener;

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
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONObject;
import org.onosproject.kcsg.locallistener.models.InforControllerModel;

public final class HandleVersion {
    private static final String INIT_PATH = "/home/onos/sdn";

    private HandleVersion() {}

    public static void createVersion() {
        String path = INIT_PATH;
        JSONObject jsonVersion = new JSONObject();

        InforControllerModel local = getLocal();
        if (local != null) {
            jsonVersion.put(local.getIp(), 0);
        }

        ArrayList<InforControllerModel> mems = getMembers();
        for (InforControllerModel mem : mems) {
            jsonVersion.put(mem.getIp(), 0);
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
        } catch (Exception e) {
            //log.error(e.getMessage(), e);
        } finally {
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
        } catch (Exception e) {
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
        return 0;
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
        } catch (Exception e) {
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
        } catch (Exception e) {
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
        } catch (Exception e) {
            //log.error(e.getMessage(), e);
        } finally {
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
            infor.setIp(localIp);
            infor.setKindController(kindController);
        } catch (Exception e) {
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

    public static String getServerUrl() {
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
            String serverUrl = object.getString("serverUrl");
            return serverUrl;
        } catch (Exception e) {
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
                model.setIp(destIp);
                model.setKindController(kindController);

                mems.add(model);
            }
        } catch (Exception e) {
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

    public static ArrayList<InforControllerModel> getAllController() {
        ArrayList<InforControllerModel> controllers = new ArrayList<>();

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
        } catch (Exception e) {
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
        return controllers;
    }

    public static InforControllerModel getRandomMember() {
        ArrayList<InforControllerModel> memberList = getAllController();
        Random rand = new Random();
        int index = rand.nextInt(memberList.size());
        return memberList.get(index);
    }

    public static ArrayList<InforControllerModel> getRandomMembers(int numMem) {
        ArrayList<InforControllerModel> memberList = getAllController();
        ArrayList<InforControllerModel> result = new ArrayList<InforControllerModel>();
        for (int i = 0; i < numMem; i++) {
            int size = memberList.size();
            if (size > 0) {
                Random rand = new Random();
                int index = rand.nextInt(size);
                result.add(memberList.get(index));
                memberList.remove(index);
            }
        }
        return result;
    }
}