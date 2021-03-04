package org.onosproject.kcsg.restapi.communicateapi;

import java.util.ArrayList;

import org.onosproject.kcsg.restapi.communicateapi.models.VersionModel;

/**
 * Kcsg topology api interface.
 */
public interface KcsgCommunicateApiService {

    /**
     * compare versions.
     * @param versions versions
     * @return ip list for update
     */
    ArrayList<VersionModel> compareVersions(ArrayList<VersionModel> versions);
}
