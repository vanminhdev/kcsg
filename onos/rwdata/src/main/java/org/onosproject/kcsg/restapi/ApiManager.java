package org.onosproject.kcsg.restapi;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONObject;
import org.onosproject.core.CoreService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Component in charge of handling local topology events and applying
 * <p>
 * the mapping between internal changes and the exposed topologies.
 */
@Component(immediate = true, service = {})
public class ApiManager {
    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    public static final String PROVIDER_NAME = "org.onosproject.kcsg.api";

    @Activate
    public void activate() {
        coreService.registerApplication(PROVIDER_NAME);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    public JSONObject getDevices() {
        String url = "http://localhost:8181/onos/v1/devices";
        return requestGet(url);
    }

    public JSONObject getPorts() {
        String url = "http://localhost:8181/onos/v1/devices/ports";
        return requestGet(url);
    }

    public JSONObject getHosts() {
        String url = "http://localhost:8181/onos/v1/hosts";
        return requestGet(url);
    }

    public JSONObject getLinks() {
        String url = "http://localhost:8181/onos/v1/links";
        return requestGet(url);
    }

    private static JSONObject requestGet(String url) {
        try {
            Unirest.setTimeouts(0, 0);
            HttpResponse<String> response = Unirest.get(url).header("Accept", "application/json")
                    .header("Authorization", "Basic a2FyYWY6a2FyYWY=").asString();

            JSONObject object = new JSONObject(response);
            return new JSONObject(object.getString("body"));
        } catch (UnirestException e) {
            return null;
        }
    }
}