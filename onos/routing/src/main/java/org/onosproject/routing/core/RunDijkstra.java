package org.onosproject.routing.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunDijkstra {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private Graph graph;
    private ArrayList<Node> nodes;
    public RunDijkstra() {
        graph = new Graph();
        nodes = new ArrayList<>();
    }

    private Node getNodeByName(String name) {
        for (Node node : nodes) {
            if (node.getName().equals(name)) {
                return node;
            }
        }
        return null;
    }

    public void addLink(String src, String dst, int distance) {
        Node srcNode = getNodeByName(src);
        Node dstNode = getNodeByName(dst);
        if (dstNode == null) {
            dstNode = new Node(dst);
            nodes.add(dstNode);
        }
        if (srcNode == null) {
            srcNode = new Node(src);
            srcNode.addDestination(dstNode, distance);
            nodes.add(srcNode);
        } else {
            srcNode.addDestination(dstNode, distance);
        }
    }

    public List<Node> run(String src, String dst) {
        //show graph
        for (Node node : nodes) {
            log.info("node: " + node.getName());
            for (Map.Entry<Node, Integer> entry : node.getAdjacentNodes().entrySet()) {
                log.info("\t" + entry.getKey().getName() + " " + entry.getValue());
            }
        }

        List<Node> path = new ArrayList<Node>();
        Node nodeStart = getNodeByName(src);
        for (Node node : nodes) {
            graph.addNode(node);
        }

        log.info("nodeStart: " + nodeStart.getName());
        graph = Dijkstra.calculateShortestPathFromSource(graph, nodeStart);
        Set<Node> nodesResult = graph.getNodes();
        for (Node node: nodesResult) {
            log.info("dst: " + node.getName());
            for (Node node2 : node.getShortestPath()) {
                log.info("\t" + node2.getName());
            }

            if (node.getName().equals(dst)) {
                List<Node> shortestPath = node.getShortestPath();
                Node dstNode = new Node(dst);
                shortestPath.add(dstNode);
                return shortestPath;
            }
        }
        return path;
    }
}
