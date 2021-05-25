import networkx
import random
import sys
from threading import Timer
from mininet.net import Mininet
from mininet.topo import Topo
from mininet.link import TCLink
from mininet.node import RemoteController
from mininet.cli import CLI
from mininet.log import MininetLogger

#Domain Controller
# Domain=['192.168.31.132', '192.168.31.238', '192.168.31.95', '192.168.31.17',
#         '192.168.31.219', '192.168.31.229', '192.168.31.174', '192.168.31.203',
#         '192.168.31.184']
Domain=['192.168.31.132']

net = Mininet( topo=None, build=False)

graph = networkx.read_graphml("topologyzoo/sources/Cogentco.graphml")
nodes_graph = graph.nodes()
edges = graph.edges()
hosts_graph = [n for n in nodes_graph if 1 == graph.in_degree()[n] + graph.out_degree()[n]]

print("hosts ", hosts_graph)
print("num hosts ", len(hosts_graph))
print("nodes ", nodes_graph)
print("edge ", edges)

switches = {} #luu theo loai cu the
hosts = {}
nodes = {} #luu theo ten node

for node in nodes_graph:
    if node in hosts_graph:
        host_name = "h" + str(node).replace("n", "")
        host = net.addHost(host_name)
        hosts[host_name] = host
        nodes[node] = host
    else:
        switch_name = "s" + str(node).replace("n", "")
        switch = net.addSwitch(switch_name, protocols='OpenFlow13')
        switches[switch_name] = switch
        nodes[node] = switch

#them canh noi host -> switch, switch -> switch
for edge in edges:
    net.addLink(nodes[edge[0]], nodes[edge[1]])

net.addController("c0", controller=RemoteController, ip='192.168.31.132', port=6653)

net.build()

net.start()
CLI(net)
net.stop()
