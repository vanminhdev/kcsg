import time
from datetime import datetime
import random
import requests
import json

ccdn_api_url = "http://localhost:5000/"
onos_api_url = "onos/rwdata/communicate/"
faucet_api_url = "faucet/sina/versions/"
odl_api_url = "restconf/operations/"


def test_ping(source, destination, controller):
    controller_ip = controller["ip"]
    controller_type = controller["type"]
    result = None
    if controller_type == "onos":
        result = requests.post("{}:8181/{}test-ping?source={}&des={}".format(controller_ip, onos_api_url, source.name, destination.name))
    elif controller_type == "faucet":
        result = requests.post("{}:8081/{}test-ping?source={}&des={}".format(controller_ip, faucet_api_url, source.name, destination.name))
    elif controller_type == "odl":
        result = requests.post("{}:8181/{}sina:testPing?source={}&des={}".format(controller_ip, odl_api_url, source.name, destination.name))
    if result != "None" and result.status_code == 200:
        result = json.loads(result.content)
        requests.post("{}update_row?row_id={}&can_ping={}".format(ccdn_api_url, result["row_id"], result["can_ping"]))


def get_controllers():
    result = requests.get("{}get-controllers".format(ccdn_api_url))
    if result.status_code == 200:
        result = json.loads(result.content)
        return result
    return []

if __name__=="__main__":
    while(1):
        print("requesting for host ping...")
        controllers = get_controllers()
        controller = random.choice(controllers)
        hosts = net.hosts
        source = random.choice(hosts)
        destination = source
        while (hosts.length > 2 and destination == source):
            destination = random.choice(hosts)
        test_ping(source, destination, controller)
        time.sleep(5)
    
