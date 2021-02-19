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

import org.json.JSONArray;
import org.json.JSONObject;
import org.onosproject.kcsg.locallistener.models.InforControllerModel;

public final class HandleVersion {
    private HandleVersion() {}

    public static void createVersion() {
        String path = System.getProperty("java.io.tmpdir");
        JSONObject jsonVersion = new JSONObject();

        InforControllerModel local = getLocal();
        if (local != null) {
            jsonVersion.put(local.ip, 1);
        }

        ArrayList<InforControllerModel> mems = getMembers();
        for (InforControllerModel mem : mems) {
            jsonVersion.put(mem.ip, 1);
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
                //log.error(e.getMessage(), e);
            }
        }
    }

    public static int getVersion(String ip) {
        String path = System.getProperty("java.io.tmpdir");

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
        return -1;
    }

    public static String getVersions() {
        String path = System.getProperty("java.io.tmpdir");

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
        String path = System.getProperty("java.io.tmpdir");
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
                //log.error(e.getMessage(), e);
            }
        }
    }

    public static String getData(String ip) {
        String path = System.getProperty("java.io.tmpdir");

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

    public static InforControllerModel getLocal() {
        InforControllerModel infor = null;
        String path = System.getProperty("java.io.tmpdir");

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

    public static ArrayList<InforControllerModel> getMembers() {
        ArrayList<InforControllerModel> mems = new ArrayList<>();

        String path = System.getProperty("java.io.tmpdir");

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

    public static String getKindController(String ip) {
        String path = System.getProperty("java.io.tmpdir");

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
                if (destIp == ip) {
                    String kindController = controller.getString("controller");
                    return kindController;
                }
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
        return null;
    }
}
