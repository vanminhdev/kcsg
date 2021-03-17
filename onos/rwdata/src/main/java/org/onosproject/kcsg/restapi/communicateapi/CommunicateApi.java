package org.onosproject.kcsg.restapi.communicateapi;

import org.json.JSONObject;
import org.onlab.rest.BaseResource;
import org.onosproject.kcsg.locallistener.HandleVersion;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Kcsg Communicate Data API.
 */
@Path("communicate")
public class CommunicateApi extends BaseResource {

    private final Logger log = getLogger(getClass());

    /**
     * update version.
     * @param jsonObject input
     * @return response code OK
     */
    @POST
    @Path("update-version")
    @Produces(MediaType.APPLICATION_JSON)
    public Response writeVesion(JSONObject jsonObject) {
        String ip = jsonObject.getString("ip");
        int version = jsonObject.getInt("version");
        HandleVersion.setVersion(ip, version);
        return Response.ok().build();
    }

    /**
     * get version.
     * @param jsonObject input
     * @return response code OK
     */
    @POST
    @Path("get-version")
    @Produces(MediaType.APPLICATION_JSON)
    public Response readVesion(JSONObject jsonObject) {
        String ip = jsonObject.getString("ip");
        JSONObject response = new JSONObject();
        response.put("verison", HandleVersion.getVersion(ip));
        return Response.ok(response).build();
    }
}
