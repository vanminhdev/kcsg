package org.onosproject.kcsg.restapi.communicateapi;

import org.json.JSONArray;
import org.json.JSONObject;
import org.onlab.rest.BaseResource;
import org.onosproject.kcsg.locallistener.KcsgListenerManager;
import org.onosproject.kcsg.restapi.communicateapi.models.DataUpdateModel;
import org.onosproject.kcsg.restapi.communicateapi.models.VersionModel;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Iterator;

import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Kcsg Communicate Data API.
 */
@Path("communicate")
public class KcsgCommunicateApi extends BaseResource {

    private final Logger log = getLogger(getClass());

    /**
     * compare versions.
     * @param strVer versions
     * @return response code OK
     */
    @POST
    @Path("compareVersions")
    @Produces(MediaType.APPLICATION_JSON)
    public Response compareVersions(String strVer) {
        JSONObject jsonObject = new JSONObject(strVer);
        Iterator<String> keys = jsonObject.keys();
        ArrayList<VersionModel> versions = new ArrayList<>();
        while (keys.hasNext()) {
            String key = keys.next();
            log.info(key);
            VersionModel model = new VersionModel();
            model.ip = key;
            model.ver = jsonObject.getInt(key);
            versions.add(model);
        }
        KcsgCommunicateApiService service = get(KcsgCommunicateApiService.class);
        try {
            var result = service.compareVersions(versions);
            JSONArray jsonResult = new JSONArray(result);
            return Response.ok(jsonResult.toString()).build();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return Response.status(500).build();
    }

    /**
     * update log file.
     * @param updateData data update
     * @return response code OK
     */
    @PUT
    @Path("updateNewLog")
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateNewLog(String updateData) {
        JSONArray jsonArray = new JSONArray(updateData);
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject obj = jsonArray.getJSONObject(i);
            String ip = obj.getString("ip");
            int ver = obj.getInt("ver");
            String data = obj.getString("data");

            DataUpdateModel model = new DataUpdateModel();
            model.ip = ip;
            model.version = ver;
            model.data = data;
            KcsgListenerManager.updateDataQueue.add(model);
        }
        return Response.ok().build();
    }
}
