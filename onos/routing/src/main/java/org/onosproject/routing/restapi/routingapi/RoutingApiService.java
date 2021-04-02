package org.onosproject.routing.restapi.routingapi;

import org.json.JSONObject;

/**
 * Kcsg topology api interface.
 */
public interface RoutingApiService {

    /**
     * get routing.
     * @param src src
     * @param dst dst
     * @return JSON representation
     */
    JSONObject getRouting(String src, String dst);
}