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
     * @param input input
     * @return response code OK
     */
    @POST
    @Path("update-version")
    @Produces(MediaType.APPLICATION_JSON)
    public Response writeVesion(String input) {
        log.info(input);
        try {
            JSONObject jsonObject = new JSONObject(input);
            String ip = jsonObject.getString("ip");
            int version = jsonObject.getInt("version");
            log.info("update ver of " + ip + " to " + version);
            HandleVersion.setVersion(ip, version);
            return Response.ok().build();
        } catch (Exception e) {
            log.error(e.getMessage());
            return Response.status(500).build();
        }
    }

    /**
     * get version.
     * @param input input
     * @return response code OK
     */
    @POST
    @Path("get-version")
    @Produces(MediaType.APPLICATION_JSON)
    public Response readVesion(String input) {
        log.info(input);
        try {
            JSONObject jsonObject = new JSONObject(input);
            String ip = jsonObject.getString("ip");
            log.info("get ver of " + ip);
            JSONObject response = new JSONObject();
            response.put("version", HandleVersion.getVersion(ip));
            return Response.ok(response.toString()).build();
        } catch (Exception e) {
            log.error(e.getMessage());
            return Response.status(500).build();
        }
    }
}
