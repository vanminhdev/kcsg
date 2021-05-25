from flask import Flask, request
import json
import os
from threading import Timer
import random
import networkx
import sys
import numpy
import sched, time
from mininet.net import Mininet
from mininet.node import Controller, OVSSwitch, RemoteController
from mininet.cli import CLI
from mininet.util import dumpNetConnections
from mininet.log import setLogLevel, info

from dijkstra import DijkstraSPF
from graph import Graph

os.system("sudo mn -c")

#Domain Controller
Domain=['192.168.31.132', '192.168.31.238', '192.168.31.174', '192.168.31.17',
        '192.168.31.69', '192.168.31.11', '192.168.31.115', '192.168.31.184',
        '192.168.31.219', '192.168.31.241', '192.168.31.203', '192.168.31.229',
        '192.168.31.153', '192.168.31.70', '192.168.31.31', '192.168.31.218',
        '192.168.31.71', '192.168.31.104']
# Domain=['192.168.31.132', '192.168.31.174','192.168.31.17', '192.168.31.238' ,'192.168.31.184','192.168.31.219','192.168.31.229','192.168.31.203']

convert_to_ip = lambda n: '10.0.' + str((int(n[1:])+1)//256) + '.' + str((int(n[1:])+1)%256)
convert_to_mac = lambda n: '12:34:56:78:' + '%02X'%((int(n[1:])+1)//256) + ':' + '%02X'%((int(n[1:])+1)%256)

net = Mininet(controller=Controller, switch=OVSSwitch, waitConnected=False)
#Cogentco
#Cesnet200706
graph = networkx.read_graphml("topologyzoo/sources/Cogentco.graphml")
nodes_graph = graph.nodes()
edges = graph.edges()
hosts_graph = [n for n in nodes_graph if 1 == graph.in_degree()[n] + graph.out_degree()[n]]

# print("hosts ", hosts_graph)
# print("num hosts ", len(hosts_graph))
# print("nodes ", nodes_graph)
# print("edge ", edges)

controllers_save = {}
switches_save = {} #luu theo loai cu the
hosts_save = {}
nodes_save = {} #luu theo ten node

#net.addController(controller_name,controller= lambda controller_name: RemoteController(controller_name, ip=Domain[i], port=6653))
for i in range(len(Domain)):
    controller_name = "c" + str(i)
    print(controller_name, " : ", Domain[i])
    controller = net.addController(controller_name, controller=RemoteController, ip=Domain[i], port=6653)
    controllers_save[controller_name] = controller

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

net.build() #sinh ip cho cac host

# host_name = "h2"
# host = net.addHost(host_name)
# hosts_save[host_name] = host
# nodes_save[node] = host
# net.addLink(host, switches_save["s11"])

print("hosts: ", list(hosts_save.keys()))

print("num switch: ", len(switches_save))
print("num host: ", len(hosts_save))

#c2 c7 c10 faucet
#c3 c8 c11 odl
map_switch_controller = {
    "c0": ["s2","s41","s39","s42","s36","s37","s38"],
    "c1": ["s0","s13","s12","s10","s11","s9","s103"],
    "c2": ["s8","s7","s6","s4","s5"],
    "c3": ["s23","s27","s26","s101","s28","s102","s61"],
    "c4": ["s30","s60","s31","s32","s34","s35","s53"],
    "c5": ["s104","s63","s64","s105","s62","s59","s65","s66","s67","s68","s69"],
    "c6": ["s58","s79","s78","s76","s75","s71","s72","s73"],
    "c7": ["s47","s44","s46","s49","s48"],
    "c8": ["s57","s56","s52","s51","s55","s50"],
    "c9": ["s70","s83","s85","s88","s80","s90","s91","s96","s94","s95","s87","s100","s98"],
    "c10": ["s14","s15","s183","s18","s17","s19","s22","s20","s21"],
    "c11": ["s107","s122","s109","s108","s110","s111","s121","s123"],
    "c12": ["s112","s113","s115","s119","s196","s117"],
    "c13": ["s125","s130","s120","s126","s129","s138"],
    "c14": ["s133","s132","s131","s134","s136","s153","s152","s154"],
    "c15": ["s155","s157","s158","s159","s160","s162","s161","s163"],
    "c16": ["s170","s169","s168","s176","s177","s178","s179","s167","s166"],
    "c17": ["s181","s171","s174","s173","s175"]
}

#====================map switch voi controller=========================
for key in switches_save.keys():
    switches_save[key].start([])

for key in controllers_save.keys():
    controllers_save[key].start()

for key in map_switch_controller.keys():
    for switch_name in map_switch_controller[key]:
        controller = [controllers_save[key]]
        switches_save[switch_name].start(controller)
#======================================================================

#net.start() #start toan bo switch, k nen dung lenh nay

# switches_save["s0"].stop()
# switches_save["s0"].start([])

cmds = []
f = open("cmd.txt", "r")
cmds = json.loads(f.read())

def chang_network_state(index_time):
    cmd = cmds[index_time][0]
    set_time = cmds[index_time][1]
    print(cmd)
    exec(cmd)
    index_time += 1
    if index_time == len(cmds):
        index_time = 0
    Timer(set_time, chang_network_state, (index_time,)).start()

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

def check_switch_in_faucet(sw):
    return sw in map_switch_controller["c2"] or sw in map_switch_controller["c7"] or sw in map_switch_controller["c10"]

def check_switch_in_odl(sw):
    return sw in map_switch_controller["c3"] or sw in map_switch_controller["c8"] or sw in map_switch_controller["c11"]

#sinh thay doi mang
def gen_network_script():
    cmds = []
    links_cmd = get_links(net)
    for src in links_cmd.keys():
        cmd_temp = []
        times = 1
        print("gen change topo:", src)
        if check_switch_in_faucet(src):
            times = 2
        elif check_switch_in_odl(src):
            times = 2
        for i in range(times):
            for dst in links_cmd[src].keys():
                set_time = random.uniform(1,2)
                cmd_temp.append((min(src, dst), max(src, dst)))
                # cmd_temp.append("up")
                cmd_temp.append(set_time)
                cmds.append(cmd_temp)
                # cmd_temp.append(("net.configLinkStatus('" + src + "', '" + dst + "', 'up')", set_time))
                cmd_temp = []
                set_time = random.uniform(1,2)
                cmd_temp.append((min(src, dst), max(src, dst)))
                # cmd_temp.append("down")
                cmd_temp.append(set_time)
                cmds.append(cmd_temp)
            # cmd_temp.append(("net.configLinkStatus('" + src + "', '" + dst + "', 'down')", set_time))
    
    cmds = cmds * 3
    random.shuffle(cmds)
    exec_cmds = []
    state = {}
    for cmd in cmds:
        if (not cmd[0] in state.keys()) or state[cmd[0]] == "up":
            exec_cmds.append(("net.configLinkStatus('" + cmd[0][0] + "', '" + cmd[0][1] + "', 'down')", cmd[1]))
            state[cmd[0]] = "down"
        else:
            exec_cmds.append(("net.configLinkStatus('" + cmd[0][0] + "', '" + cmd[0][1] + "', 'up')", cmd[1]))
            state[cmd[0]] = "up"
    f = open("cmd.txt", "w")
    f.write(json.dumps(exec_cmds))

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

@app.route('/start-change', methods=['GET'])
def start_change_net():
    chang_network_state(0)
    return ('', 200)

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
    return (str(True), 200)

# @app.route('/forwarding', methods=['POST'])
# def forwarding():
#     input = json.loads(request.data)
#     src = input["src"]
#     dst = input["dst"]
#     links = get_links(net)
#     print(links)
#     pathFromSrc = find_path(links, src, dst)
#     print("path from src ", pathFromSrc)
#     add_flow(links, pathFromSrc)

#     pathFromDst = find_path(links, dst, src)
#     print("path from dst ", pathFromDst)
#     add_flow(links, pathFromDst)

#     hostSrc = hosts_save[src]
#     hostDst = hosts_save[dst]

#     print("host src ",hostSrc)
#     print("host dst ",hostDst)
#     print(hostDst.IP())
#     comm = 'ping -c1 -W 1 ' + str(hostDst.IP())
#     print(comm)
#     result = hostSrc.cmd(comm)
#     sent, received = net._parsePing(result)
#     print(sent, received)
#     return (str(sent == received), 200)

    # if str(hostSrc) in pathFromSrc and str(hostDst) in pathFromSrc:
    #     print('path success')
    #     return (str(True), 200)
    # else:
    #     print('path fail')
    #     return (str(False), 200)


#run api
# requests.get('http://192.168.31.148:5000/start-change')

# hosts_save["h1"].cmd("vlc")
# hosts_save["h106"].cmd("vlc")

#net.start()
app.run(host='0.0.0.0', debug=True, use_reloader=False)
#CLI(net)
# net.stop()
