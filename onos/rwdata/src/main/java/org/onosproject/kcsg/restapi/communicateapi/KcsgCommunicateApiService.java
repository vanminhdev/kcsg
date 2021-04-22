package org.onosproject.kcsg.restapi.communicateapi;

import org.json.JSONObject;

/**
 * Kcsg topology api interface.
 */
public interface KcsgCommunicateApiService {

    /**
     * Test Ping.
     * @param src src host
     * @param dst dst host
     * @return log test ping
     */
    JSONObject testPing(String src, String dst);
}