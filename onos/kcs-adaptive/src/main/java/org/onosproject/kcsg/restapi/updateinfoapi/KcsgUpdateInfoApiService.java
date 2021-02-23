package org.onosproject.kcsg.restapi.updateinfoapi;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Kcsg update information api interface.
 */
public interface KcsgUpdateInfoApiService {

    /**
     * Update information about other controller.
     *
     * @param data String data to update
     * @return JSON representation
     */
    JsonNode updateData(String data);
}
