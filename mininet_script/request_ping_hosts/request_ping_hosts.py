import time
from datetime import datetime
import random
import requests
import json

ccdn_api_url = "http://192.168.31.94:5000/"
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
    while(1):
        print("requesting for host ping...")
        controllers = get_controllers()
        controller = random.choice(controllers)
        print("controller random: ", controller)

        hosts = ["h1", "h2", "h3", "h4"]
        sample = random.sample(hosts, 2)
        source = sample[0]
        destination = sample[1]
        test_ping(source, destination, controller)
        time.sleep(5)
    
