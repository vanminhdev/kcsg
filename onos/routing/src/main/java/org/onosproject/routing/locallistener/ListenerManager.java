package org.onosproject.routing.locallistener;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Component in charge of handling local topology events and applying
 * <p>
 * the mapping between internal changes and the exposed topologies.
 */
@Component(immediate = true)
public class ListenerManager {
    private final Logger log = getLogger(getClass());
    private static final String INIT_PATH = "/home/onos/sdn";

    public static final String PROVIDER_NAME = "org.onosproject.routing.listener";
    public static String myIpAddress = null;
    public static String serverUrl = null;

    @Activate
    public void activate() {
        log.info("Started Routing");
        init();
        //scheduleCommunicate();
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped Routing");
    }

    private void init() {
    }
}
