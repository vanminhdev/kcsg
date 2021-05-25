import time
from datetime import datetime
import random
import requests
import json
import itertools
from threading import Timer
import requests

ccdn_api_url = "http://192.168.31.108:5000/"
onos_api_url = "onos/rwdata/communicate/"
faucet_api_url = "faucet/sina/communicate/"
odl_api_url = "restconf/operations/"


def test_ping(source, destination, controller):
    controller_ip = controller["ip"]
    controller_type = controller["type"]
    result = None

    headers = {"Authorization": "Basic a2FyYWY6a2FyYWY=",
                "Content-Type": "application/json",
                "Accept": "application/json"}
    data = data=json.dumps({"src": source, "dst": destination})
    if controller_type == "ONOS":
        res = requests.post(url="http://{}:8181/{}test-ping".format(controller_ip, onos_api_url),
                                data=data, headers=headers)
        print("\ttest ping onos ", res.status_code, " ", res.text)
        if res.status_code == 200:
            result = json.loads(res.content)
    elif controller_type == "Faucet":
        res = requests.post(url="http://{}:8080/{}test-ping".format(controller_ip, faucet_api_url),
                                data=data, headers=headers)
        print("\ttest ping faucet ", res.status_code, " ", res.text)
        if res.status_code == 200:
            result = json.loads(res.content)
    elif controller_type == "ODL":
        headers["Authorization"] = "Basic YWRtaW46YWRtaW4="
        res = requests.post(url="http://{}:8181/{}sina:testPing".format(controller_ip, odl_api_url),
                                data=json.dumps({"input": {"data": data}}), headers=headers)
        print("\ttest ping odl ", res.status_code, " ", res.text)
        if res.status_code == 200:
            resBody = json.loads(res.content)
            result = json.loads(resBody["output"]["result"])

    if result != None:
        res = requests.put(url="{}api/log/log-ping?id={}&isPingSuccess={}".format(ccdn_api_url,
                        result["id"], json.dumps(result["isPingSuccess"])))
        print("res update isPingSuccess ", res.status_code, " ", res.text)


def get_controllers():
    result = requests.get(ccdn_api_url + "api/RemoteIp/get-list-controller")
    if result.status_code == 200:
        result = json.loads(result.content)
        return result
    else:
        print("get controller error: ", result.status_code, " ", result.text)
    return []

if __name__=="__main__":
    hosts = ['h1', 'h3', 'h24', 'h29', 'h54', 'h74', 'h77', 'h81', 'h106', 'h114', 'h118', 'h124', 'h127', 'h128', 'h135', 'h140', 'h164', 'h165', 'h180', 'h182', 'h195']
    pairs = list(itertools.combinations(hosts, 2)) # chon cap 2 host
    print("len pair: ", len(pairs))
    time_pairs = pairs * 17
    random.shuffle(time_pairs) #random lai thu tu

    # cmds = []
    # controllers = get_controllers()
    # for pair in time_pairs:
    #     controller = random.choice(controllers)
    #     set_time = random.uniform(1,2)
    #     cmds.append((controller, pair[0], pair[1], set_time))
    # f = open("cmd.txt", "w")
    # f.write(json.dumps(cmds))

    cmds2 = []
    f = open("cmd.txt", "r")
    cmds2 = json.loads(f.read())
    def handle_test_ping(index_time):
        print("requesting for host ping...")
        controller = cmds2[index_time][0]
        print("controller random: ", controller)
        source = cmds2[index_time][1]
        destination = cmds2[index_time][2]
        test_ping(source, destination, controller)

        set_time = cmds2[index_time][3]
        index_time += 1
        if index_time == len(cmds2):
            index_time = 0
        Timer(set_time, handle_test_ping, (index_time,)).start()

    handle_test_ping(0)

    def change_network():
        print("start change network")
        requests.get("http://localhost:5000/start-change")
    Timer(5, change_network).start()
