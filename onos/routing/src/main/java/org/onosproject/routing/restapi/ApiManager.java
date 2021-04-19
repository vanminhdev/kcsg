package org.onosproject.routing.restapi;

import org.json.JSONArray;
import org.json.JSONObject;
import org.onlab.rest.BaseResource;
import org.onosproject.core.CoreService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.topology.PathService;
import org.onosproject.routing.core.Node;
import org.onosproject.routing.core.RunDijkstra;
import org.onosproject.routing.handledata.HandleData;
import org.onosproject.routing.handledata.models.CusDevice;
import org.onosproject.routing.handledata.models.CusHost;
import org.onosproject.routing.handledata.models.CusLink;
import org.onosproject.routing.handledata.models.CusTopo;
import org.onosproject.routing.handledata.models.InforControllerModel;
import org.onosproject.routing.restapi.routingapi.RoutingApiService;
import org.onosproject.routing.restapi.routingapi.models.Criteria;
import org.onosproject.routing.restapi.routingapi.models.Flow;
import org.onosproject.routing.restapi.routingapi.models.Flows;
import org.onosproject.routing.restapi.routingapi.models.Instruction;
import org.onosproject.routing.restapi.routingapi.models.Selector;
import org.onosproject.routing.restapi.routingapi.models.Treatment;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

/**
 * Component in charge of handling local topology events and applying
 * <p>
 * the mapping between internal changes and the exposed topologies.
 */
@Component(immediate = true, service = {RoutingApiService.class})
public class ApiManager extends BaseResource implements RoutingApiService {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private static final String MSG = "Api manager: {}";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    public static final String PROVIDER_NAME = "org.onosproject.routing.api";
    final DeviceService deviceService = get(DeviceService.class);
    final PathService pathService = get(PathService.class);
    final HostService hostService = get(HostService.class);
    final LinkService linkService = get(LinkService.class);
    final FlowRuleService flowRuleService = get(FlowRuleService.class);

    @Activate
    public void activate() {
        coreService.registerApplication(PROVIDER_NAME);
        log.info(MSG, "Started api");
    }

    @Deactivate
    public void deactivate() {
        log.info(MSG, "Stopped api");
    }

    private CusDevice findDevice(ArrayList<CusDevice> lstDevices, String id) {
        CusDevice device = null;
        for (CusDevice dev : lstDevices) {
            if (dev.getId().equals(id)) {
                return dev;
            }
        }
        return device;
    }

    private CusHost findHost(ArrayList<CusHost> lstHosts, String id) {
        CusHost hosts = null;
        for (CusHost hos : lstHosts) {
            log.info(MSG, "find host: " + hos.getId());
            if (hos.getId().equals(id)) {
                return hos;
            }
        }
        return hosts;
    }

    private CusLink findLink(Iterable<CusLink> links, String src, String dst) {
        CusLink link = null;
        for (CusLink lik: links) {
            if (lik.getIdSrc().equals(src) && lik.getIdDst().equals(dst)) {
                return lik;
            }
        }
        return link;
    }

    private void getFlows(List<Node> result, Flows flows,
        String src, String dst,
        ArrayList<CusHost> lstHosts, ArrayList<CusDevice> lstDevices, ArrayList<CusLink> lstLinks) {
        for (int i = 0; i < result.size(); i++) {
            //Host host = findHost(lstHosts, result.get(i).getName());
            CusDevice dev = findDevice(lstDevices, result.get(i).getName());
            if (dev != null && i >= 1 && i < result.size() - 1 && result.size() >= 3) {
                log.info(MSG, "dev != null " + dev.getId());
                log.info(MSG, "left find: " + result.get(i - 1).getName());
                log.info(MSG, "right find: " + result.get(i + 1).getName());
                CusHost hostLeft = findHost(lstHosts, result.get(i - 1).getName());
                CusDevice devLeft = findDevice(lstDevices, result.get(i - 1).getName());

                CusHost hostRight = findHost(lstHosts, result.get(i + 1).getName());
                CusDevice devRight = findDevice(lstDevices, result.get(i + 1).getName());

                log.info(MSG, "host left " + hostLeft);
                log.info(MSG, "dev left " + devLeft);
                log.info(MSG, "host right " + hostRight);
                log.info(MSG, "dev right " + devRight);

                Flow flow = new Flow();
                flow.priority = 200;
                flow.timeout = 0;
                flow.isPermanent = true;
                flow.deviceId = dev.getId();
                Instruction instruction = new Instruction(); //ra
                instruction.type = "OUTPUT";
                Treatment treatment = new Treatment();
                treatment.instructions = new ArrayList<Instruction>();
                treatment.instructions.add(instruction);
                flow.treatment = treatment;
                flow.selector = new Selector();
                flow.selector.criteria = new ArrayList<>();
                Criteria criteria = new Criteria();
                criteria.type = "IN_PORT";
                flow.selector.criteria.add(criteria);

                if (hostLeft != null) {
                    criteria.port = hostLeft.getPort(); //vao
                }

                if (devLeft != null) {
                    log.info(MSG, "test " + devLeft);
                    //tim link
                    CusLink link = findLink(lstLinks, devLeft.getId(), dev.getId());
                    if (link != null) {
                        criteria.port = link.getPortDst();
                    } else {
                        log.error(MSG, "khong tim thay link: " + devLeft.getId() + " " + dev.getId());
                    }
                }

                if (hostRight != null) {
                    log.info(MSG, "test " + hostRight);
                    instruction.port = Integer.toString(hostRight.getPort());
                }

                if (devRight != null) {
                    CusLink link = findLink(lstLinks, dev.getId(), devRight.getId());
                    if (link != null) {
                        instruction.port = Integer.toString(link.getPortSrc());
                    } else {
                        log.error(MSG, "khong tim thay link: " + dev.getId() + " " + devRight.getId());
                    }
                }

                flow.selector.criteria.add(new Criteria(src, "ETH_SRC", 0));
                flow.selector.criteria.add(new Criteria(dst, "ETH_DST", 0));
                flows.flows.add(flow);
            }
        }
    }

    private void findPath(String src, String dst, Flows flows) {
        ArrayList<InforControllerModel> allCtrl = HandleData.getAllController();
        ArrayList<CusHost> cusHosts = new ArrayList<>();
        ArrayList<CusDevice> cusDevices = new ArrayList<>();
        ArrayList<CusLink> cusLinks = new ArrayList<>();

        for (InforControllerModel ctrl : allCtrl) {
            CusTopo cusTopo = HandleData.getTopo(ctrl.getIp());
            if (cusTopo != null) {
                cusHosts.addAll(cusTopo.getHosts());
                cusDevices.addAll(cusTopo.getDevices());
                cusLinks.addAll(cusTopo.getLinks());
            }
        }
        log.info(MSG, "size " + cusHosts.size());
        log.info(MSG, "size " + cusDevices.size());
        log.info(MSG, "size " + cusLinks.size());
        RunDijkstra runner = new RunDijkstra();

        //device la switch
        // for (CusDevice device : cusDevices) {
        //     if (device.getType().equals("SWITCH")) {
        //     }
        // }

        for (CusDevice device : cusDevices) {
            log.info(MSG, device.getId() + " " + device.getType());
        }

        //host
        for (CusHost host : cusHosts) {
            log.info(MSG, host.getId() + " " + host.getDeviceId() + " " + host.getPort());
            runner.addLink(host.getId(), host.getDeviceId(), 1);
            runner.addLink(host.getDeviceId(), host.getId(), 1);
        }

        //link
        for (CusLink link: cusLinks) {
            log.info(MSG, link.getIdSrc() + " " + link.getPortSrc() + " "
                + link.getIdDst() + " " + link.getPortDst());
            runner.addLink(link.getIdSrc(), link.getIdDst(), 1);
            runner.addLink(link.getIdDst(), link.getIdSrc(), 1);
        }

        List<Node> resultPath = runner.run(src, dst);
        getFlows(resultPath, flows, src, dst, cusHosts, cusDevices, cusLinks);
    }

    @Override
    public JSONObject getRouting(String src, String dst) {
        Flows flows = new Flows();
        flows.flows = new ArrayList<Flow>();
        findPath(src, dst, flows);
        findPath(dst, src, flows);

        log.info(MSG, "flows:");
        JSONObject jsonRule = new JSONObject();
        JSONArray jsonFlows = new JSONArray();
        jsonRule.put("flows", jsonFlows);
        for (Flow f : flows.flows) {
            JSONObject jsonFlow = new JSONObject();
            jsonFlows.put(jsonFlow);
            jsonFlow.put("priority", f.priority);
            jsonFlow.put("timeout", f.timeout);
            jsonFlow.put("isPermanent", f.isPermanent);
            jsonFlow.put("deviceId", f.deviceId);

            JSONObject jsonTreatment = new JSONObject();
            JSONArray jsonInstructions = new JSONArray();
            jsonTreatment.put("instructions", jsonInstructions);
            for (Instruction i : f.treatment.instructions) {
                JSONObject jsonInstruction = new JSONObject();
                jsonInstructions.put(jsonInstruction);
                jsonInstruction.put("type", i.type);
                jsonInstruction.put("port", i.port);
            }
            jsonFlow.put("treatment", jsonTreatment);
            JSONObject jsonSelector = new JSONObject();
            jsonFlow.put("selector", jsonSelector);
            JSONArray jsonCriterias = new JSONArray();
            jsonSelector.put("criteria", jsonCriterias);

            for (Criteria c : f.selector.criteria) {
                JSONObject jsonCriteria = new JSONObject();
                jsonCriterias.put(jsonCriteria);
                jsonCriteria.put("type", c.type);
                jsonCriteria.put("port", c.port);
                jsonCriteria.put("mac", c.mac);
            }
        }
        log.info(MSG, jsonRule.toString());

        try {
            HttpResponse<String> response = Unirest
                .post("http://localhost:8181/onos/v1/flows?appId=onos.onosproject.routing")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Basic a2FyYWY6a2FyYWY=")
                .body(jsonRule.toString())
                .asString();
            log.info(MSG, "add rule status: " + response.getStatus());
            log.info(MSG, "add rule message: " + response.getBody());
        } catch (UnirestException e) {
            log.error(MSG, "them rule that bai");
        }
        return null;
    }
}