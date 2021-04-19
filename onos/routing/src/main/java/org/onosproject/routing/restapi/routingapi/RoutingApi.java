package org.onosproject.routing.restapi.routingapi;

import org.onlab.rest.BaseResource;
import org.onosproject.routing.handledata.HandleData;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * API.
 */
@Path("routing")
public class RoutingApi extends BaseResource {

    private final Logger log = getLogger(getClass());

    /**
     * get routing.
     * @param src src
     * @param dst dst
     * @return response code OK
     */
    @GET
    @Path("get-routing")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRouting(@QueryParam("src") String src, @QueryParam("dst") String dst) {
        log.info("get rounting for:" + src + " " + dst);
        RoutingApiService service = get(RoutingApiService.class);
        service.getRouting(src, dst);
        return Response.ok("ok").build();
    }

    /**
     * write topo.
     * @return response code OK
     */
    @POST
    @Path("write-routing")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRouting() {
        HandleData ha = new HandleData();
        ha.writeTopo();
        return Response.ok("ok").build();
    }
}
