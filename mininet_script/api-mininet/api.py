from flask import Flask, request
import json
import os
from threading import Timer
 
from mininet.net import Mininet
from mininet.node import Controller, RemoteController
from mininet.cli import CLI
from mininet.util import dumpNetConnections
from mininet.log import setLogLevel, info

from dijkstra import DijkstraSPF
from graph import Graph

os.system("sudo mn -c")

n_controllers = 9

#Domain Controller
Domain=['192.168.254.128', '192.168.254.128', '192.168.254.128', '192.168.254.128',
        '192.168.254.128', '192.168.254.128', '192.168.254.128', '192.168.254.128',
        '192.168.254.128']
#Domain=['192.168.254.128', '192.168.254.128']

net = Mininet( topo=None, build=False)

controllers = {}
# Add Controllers
for index in range(0, n_controllers):
    controller_name = "c" + str(index + 1)
    controller = net.addController(controller_name, controller=RemoteController, ip=Domain[index], port=6653)
    controllers[controller_name] = controller 

hosts = {}
switches = {}
# Create network
for index in range(0, n_controllers):
    # add 2 hosts for each controller
    host_name1 = "h" + str(index * 2 + 1)
    host1 = net.addHost(host_name1)
    hosts[host_name1] = host1

    host_name2 = "h" + str(index * 2 + 2)
    host2 = net.addHost(host_name2)
    hosts[host_name2] = host2

    #add 2 switches
    switch_name1 = "s" + str(index * 2 + 1)
    switch1 = net.addSwitch(switch_name1)
    switches[switch_name1] = switch1

    switch_name2 = "s" + str(index * 2 + 2)
    switch2 = net.addSwitch(switch_name2)
    switches[switch_name2] = switch2

    net.addLink(hosts[host_name1], switch1)
    net.addLink(hosts[host_name2], switch2)

    print(switch_name1, switch_name2)
    net.addLink(switch1, switch2)

    if index >= 1:
        prev_switch = switches["s" + str((index - 1) * 2 + 2)]
        net.addLink(prev_switch, switch1)

    if index == n_controllers - 1:
        net.addLink(switch2, switches["s1"])

net.build()

# assign switches to controllers
for index in range(0, n_controllers):
    controller = controllers["c" + str(index + 1)]
    switch = switches["s" + str(index * 2 + 1)]
    switch.start([controller])
    switch = switches["s" + str(index * 2 + 2)]
    switch.start([controller])

#change network
commands = [
    {
        'value': "net.configLinkStatus('s1', 's2', 'down')",
    },
    {
        'value': "net.configLinkStatus('s1', 's2', 'up')",
    }
]

is_continue = True

index_cmd = 0

def change_network():
    cmd = commands[index_cmd]['value']
    print('%s'%(cmd))
    exec(cmd)
    print('Done')
    Timer(5, change_network).start()

change_network()

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
            print(keySrc + " " + keyDst)
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
    print(pathFromSrc)
    add_flow(links, pathFromSrc)

    pathFromDst = find_path(links, dst, src)
    print(pathFromDst)
    add_flow(links, pathFromDst)

    hostSrc = hosts[src]
    hostDst = hosts[dst]

    comm = 'ping -c1 -W 1 ' + str(hostDst.IP())
    result = hostSrc.cmd(comm)

    sent, received = net._parsePing(result)
    return (str(sent == received), 200)

app.run(host='0.0.0.0', debug=True, use_reloader=False)
