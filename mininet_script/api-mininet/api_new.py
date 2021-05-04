from flask import Flask, request
import json
import os
from threading import Timer
import random
 
from mininet.net import Mininet
from mininet.node import Controller, RemoteController
from mininet.cli import CLI
from mininet.util import dumpNetConnections
from mininet.log import setLogLevel, info

from dijkstra import DijkstraSPF
from graph import Graph

os.system("sudo mn -c")

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

convert_to_ip = lambda n: '10.0.' + str((int(n[1:])+1)//256) + '.' + str((int(n[1:])+1)%256)
convert_to_mac = lambda n: '12:34:56:78:' + '%02X'%((int(n[1:])+1)//256) + ':' + '%02X'%((int(n[1:])+1)%256)

net = Mininet( topo=None, build=False)
#Cogentco
graph = networkx.read_graphml("topologyzoo/sources/Nordu1989.graphml")
nodes_graph = graph.nodes()
edges = graph.edges()
hosts_graph = [n for n in nodes_graph if 1 == graph.in_degree()[n] + graph.out_degree()[n]]

# print("hosts ", hosts_graph)
# print("num hosts ", len(hosts_graph))
# print("nodes ", nodes_graph)
# print("edge ", edges)

switches_save = {} #luu theo loai cu the
hosts_save = {}
nodes_save = {} #luu theo ten node

for node in nodes_graph:
    if node in hosts_graph:
        host_name = "h" + str(node).replace("n", "")
        host = net.addHost(host_name)
        hosts_save[host_name] = host
        nodes_save[node] = host
    else:
        switch_name = "s" + str(node).replace("n", "")
        switch = net.addSwitch(switch_name)
        switches_save[switch_name] = switch
        nodes_save[node] = switch

#them canh noi host -> switch, switch -> switch
for edge in edges:
    net.addLink(nodes_save[edge[0]], nodes_save[edge[1]])

net.addController("c0", controller=RemoteController, ip='192.168.31.230', port=6653)

net.build()

def get_links(net):
    data = {}
    def dumpConnections(node):
        lk = {}
        for intf in node.intfList():
            if intf.link:
                intfs = [ intf.link.intf1, intf.link.intf2 ]
                intfs.remove(intf) #xoa ban than no di
                #print(intfs[0])
                #print(len(intfs))
                src = (str(intf)).split("-")
                dst = (str(intfs[0])).split("-")
                lk[dst[0]] = src[1].replace("eth", "")
        return lk

    nodes = net.switches + net.hosts

    for node in nodes:
        data[node.name] = dumpConnections(node)
    return data

def find_path(links, src, dst):
    graph = Graph()
    for keySrc in links:
        for keyDst in links[keySrc]:
            #print(keySrc + " " + keyDst)
            graph.add_edge(keySrc, keyDst, 1)

    dijkstra = DijkstraSPF(graph, src)
    path = []
    try:
        path = dijkstra.get_path(dst)
    except:
        path = []
    return path

def add_flow(links, path):
    if len(path) >= 3:
        for i in range(len(path) - 1):
            if i >= 1 and i <= len(path) - 1:
                print(path[i])
                cmd = "sudo ovs-ofctl add-flow " + path[i] + " in_port:" + links[path[i]][path[i-1]] + ",action=output:" + links[path[i]][path[i+1]]
                print(cmd)
                os.system(cmd)

app = Flask(__name__)

print('topo: ', get_links(net))

@app.route('/save-topo/<ip>', methods=['POST'])
def save_topo(ip):
    if os.path.isdir("data/") == False:
        os.mkdir("data")

    links = get_links(net)
    f = open("data/" + ip + ".json", "w")
    f.write(json.dumps(links))
    f.close()
    return ('', 200)

@app.route('/topo', methods=['GET'])
def link():
    data = get_links(net)
    return json.dumps(data)

@app.route('/forwarding', methods=['POST'])
def forwarding():
    input = json.loads(request.data)
    src = input["src"]
    dst = input["dst"]
    links = get_links(net)
    print(links)
    pathFromSrc = find_path(links, src, dst)
    print("path from src ", pathFromSrc)
    add_flow(links, pathFromSrc)

    pathFromDst = find_path(links, dst, src)
    print("path from dst ", pathFromDst)
    add_flow(links, pathFromDst)

    hostSrc = hosts_save[src]
    hostDst = hosts_save[dst]

    print("host src ",hostSrc)
    print("host dst ",hostDst)
    print(hostDst.IP())
    comm = 'ping -c1 -W 1 ' + str(hostDst.IP())
    print(comm)
    result = hostSrc.cmd("ifconfig")

    sent, received = net._parsePing(result)
    print(sent, received)
    return (str(sent == received), 200)

#app.run(host='0.0.0.0', debug=True, use_reloader=False)

CLI(net)
