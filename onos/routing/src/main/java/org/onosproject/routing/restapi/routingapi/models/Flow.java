/**
 * Flow.
 */
package org.onosproject.routing.restapi.routingapi.models;

public class Flow {
    public int priority;
    public int timeout;
    public boolean isPermanent;
    public String deviceId;
    public Treatment treatment;
    public Selector selector;
}
