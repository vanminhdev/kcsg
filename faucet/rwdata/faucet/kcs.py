import json
import logging
import os
import pathlib
from datetime import datetime
import random

import requests

from faucet.HandleCallServer import HandleCallServer

kinds = {
    'ONOS': 'ONOS',
    'Faucet': 'Faucet',
}

current_working_dir = str(pathlib.Path().absolute())
temp_data_dir = current_working_dir + '/tmp/'


class Kcs:
    file_path = {
        'listip': temp_data_dir + 'listip.json',
        'version': temp_data_dir + 'version.json',
        'log': temp_data_dir + 'response_log.txt',
        'config': current_working_dir + '/config.json',
    }
    local_ip = ''

    @staticmethod
    def get_all_controller():
        controllers = []
        if not os.path.isfile(Kcs.file_path['listip']):
            return controllers
        f = open(Kcs.file_path['listip'], 'r')
        info = json.load(f)
        f.close()
        controllers.append({
            'ip': info['localIp'],
            'kind': info['controller']
        })
        communication = info['communication']
        for member in communication:
            new_member = {
                'ip': member['ip'],
                'kind': member['controller']
            }
            controllers.append(new_member)
        return controllers

    @staticmethod
    def get_random_member():
        controllers = Kcs.get_all_controller()
        if len(controllers) > 1:
            return random.sample(controllers, 1)[0]
        return

    @staticmethod
    def get_random_members(num):
        controllers = Kcs.get_all_controller()
        if num > len(controllers):
            num = len(controllers)
        random_list = random.sample(controllers, num)
        return random_list

    @staticmethod
    def set_local_ip():
        print('SETTING LOCAL IP...')
        f = open(Kcs.file_path['listip'], 'r')
        info = json.load(f)
        f.close()

        Kcs.local_ip = info['localIp']

    @staticmethod
    def update_version_file(new_version_list):
        print('UPDATING VERSION FILE...')
        versions = {}

        if os.path.isfile(Kcs.file_path['version']):
            f = open(Kcs.file_path['version'], 'r')
            versions = json.load(f)
            f.close()

        for new_version in new_version_list:
            ip = new_version['ip']
            versions[ip] = new_version['version']

        with open(Kcs.file_path['version'], 'w+') as outfile:
            json.dump(versions, outfile)

    @staticmethod
    def write_local_log(api_path, event_type):
        print('WRITING LOG...')
        try:
            headers = {"Content-Type": "application/json", "Accept": "application/json"}
            r = requests.get(url=api_path, headers=headers)
            print(r.text)
            Kcs.set_local_ip()

            local_ver = int(Kcs.get_version_by_ip(Kcs.local_ip))
            print(local_ver)
            local_ver += 1
            Kcs.update_version_file([{'ip': Kcs.local_ip, 'version': local_ver}])
            Kcs.write_data(Kcs.local_ip, local_ver)
        except Exception as e:
            logging.error(e)

    @staticmethod
    def get_version_by_ip(ip):
        if os.path.isfile(Kcs.file_path['version']):
            with open(Kcs.file_path['version'], 'r') as f:
                versions = json.load(f)
            return versions[ip] if ip in versions else 0
        return 0

    @staticmethod
    def set_version(ip, version):
        versions = {}
        if os.path.isfile(Kcs.file_path['version']):
            with open(Kcs.file_path['version'], 'r') as f:
                versions = json.load(f)

        versions[ip] = version
        with open(Kcs.file_path['version'], 'w+') as outfile:
            json.dump(versions, outfile)

    @staticmethod
    def init_listip_config():
        server_url = Kcs.get_server_url()
        api = server_url + '/api/remoteIp/list-ip'
        headers = {"Content-Type": "application/json",
                   "Accept": "application/json"}
        try:
            r = requests.get(url=api, headers=headers)
            print(r.text)
            with open(Kcs.file_path['listip'], 'w+') as f:
                f.write(r.text)
        except requests.exceptions.RequestException as e:  # This is the correct syntax
            print('Error calling api listip')
            logging.error(e)
            if os.path.isfile(Kcs.file_path['listip']):
                with open(Kcs.file_path['listip'], 'w+') as f:
                    f.write("{\n" +
                            "\t\"localIp\": \"...\",\n" +
                            "\t\"controller\": \"...\", \n" +
                            "\t\"communication\": [\n" +
                            "\t\t{\n" +
                            "\t\t\t\"ip\": \"...\", \n" +
                            "\t\t\t\"controller\": \"...\"\n" +
                            "\t\t}\n" +
                            "\t]\n" +
                            "}")

    @staticmethod
    def init_version():
        controllers = Kcs.get_all_controller()
        versions = {}
        for c in controllers:
            versions[c["ip"]] = 0
        with open(Kcs.file_path['version'], 'w+') as f:
            f.write(json.dumps(versions))

    @staticmethod
    def get_server_url():
        config = {
            'serverUrl': 'http://192.168.43.176:8085'
        }
        print(Kcs.file_path['config'])
        if os.path.isfile(Kcs.file_path['config']):
            with open(Kcs.file_path['config'], 'r') as f:
                config = json.load(f)
        return config['serverUrl']

    @staticmethod
    def write_data(ip, version):
        config = HandleCallServer.get_rw_config()
        controllers = Kcs.get_random_members(config["w"])
        log_write = []
        for c in controllers:
            log_detail = {
                "localIp": Kcs.local_ip,
                "srcIp": ip,
                "dstIp": c["ip"],
                "start": datetime.now().isoformat(),
                "version": version
            }
            length = Kcs.handle_write_data(ip, version, c["ip"], c["kind"])
            if length is not None:
                log_detail["length"] = length
            else:
                log_detail["length"] = 0
            log_detail["end"] = datetime.now().isoformat()
            log_write.append(log_detail)
        HandleCallServer.send_log_write(log_write)

    @staticmethod
    def handle_write_data(ip, version, ip_dst, kind_dst):
        headers = {"Authorization": "Basic a2FyYWY6a2FyYWY=",
                   "Content-Type": "application/json",
                   "Accept": "application/json"}
        try:
            if kind_dst == kinds['Faucet']:
                api = 'http://' + ip_dst + ':8080/faucet/sina/versions/update-version'
                print(api)
                data = {"ip": ip, "version": version}
                send_data = json.dumps(data)
                print('send_data: ' + send_data)

                r = requests.post(url=api, data=send_data, headers=headers)
                print('r.text: ' + r.text)
                res = json.loads(r.text)
                print(res)

                return len(bytes(send_data))
            elif kind_dst == kinds['ONOS']:
                api = 'http://' + ip_dst + ':8181/onos/kcsg/communicate/update-version'
                print(api)
                data = {"ip": ip, "version": version}
                send_data = json.dumps(data)
                print(send_data)

                r = requests.post(url=api, data=send_data, headers=headers)
                res = json.loads(r.text)
                print(res)
                return len(bytes(send_data))
            elif kind_dst == kinds['ODL']:
                api = 'http://' + ip_dst + ':8181/restconf/operations/sina:updateVersion'
                print(api)
                data = {"input": {"data": json.dumps({"ip": ip, "version": version})}}
                send_data = json.dumps(data)
                print(send_data)

                r = requests.post(url=api, data=send_data, headers=headers)
                res = json.loads(r.text)
                print(res)
                return len(bytes(send_data))
        except Exception as e:
            logging.error(e)
        return

    @staticmethod
    def read_data():
        config = HandleCallServer.get_rw_config()

        controller_target = Kcs.get_random_member()
        controllers = Kcs.get_random_members(config["r"])

        version_from_server = HandleCallServer.get_version_from_server(controller_target["ip"])
        log_read = []
        for c in controllers:
            log_detail = {
                "localIp": Kcs.local_ip,
                "srcIp": controller_target["ip"],
                "dstIp": c["ip"],
                "start": datetime.now().isoformat(),
                "version": version_from_server
            }
            result = Kcs.handle_read_data(controller_target["ip"], version_from_server, c["ip"], c["kind"])
            if result is not None:
                log_detail["length"] = result["length"]
                log_detail["isSuccess"] = result["isSuccess"]
            else:
                log_detail["length"] = 0
                log_detail["isSuccess"] = False
            log_detail["end"] = datetime.now().isoformat()
            log_read.append(log_detail)
        HandleCallServer.send_log_read(log_read)

    @staticmethod
    def handle_read_data(ip_src, ver_from_server, ip_dst, kind_dst):
        headers = {"Authorization": "Basic a2FyYWY6a2FyYWY=",
                   "Content-Type": "application/json",
                   "Accept": "application/json"}
        try:
            if kind_dst == kinds['Faucet']:
                api = 'http://' + ip_dst + ':8080/faucet/sina/versions/get-version'
                print(api)
                data = {"ip": ip_src}
                send_data = json.dumps(data)
                print('send_data: ' + send_data)

                r = requests.post(url=api, data=send_data, headers=headers)
                print('r.text: ' + r.text)
                res = json.loads(r.text)
                print(res)
                is_success = res["version"] == ver_from_server
                return {"length": len(bytes(send_data)), "isSuccess": is_success}
            elif kind_dst == kinds['ONOS']:
                api = 'http://' + ip_dst + ':8181/onos/kcsg/communicate/get-version'
                print(api)
                data = {"ip": ip_src}
                send_data = json.dumps(data)
                print(send_data)

                r = requests.post(url=api, data=send_data, headers=headers)
                res = json.loads(r.text)
                print(res)
                is_success = res["version"] == ver_from_server
                return {"length": len(bytes(send_data)), "isSuccess": is_success}
            elif kind_dst == kinds['ODL']:
                api = 'http://' + ip_dst + ':8181/restconf/operations/sina:getVersion'
                print(api)
                data = {"input": {"data": json.dumps({"ip": ip_src})}}
                send_data = json.dumps(data)
                print(send_data)

                r = requests.post(url=api, data=send_data, headers=headers)
                res = json.loads(r.text)
                print(res)
                is_success = res["version"] == ver_from_server
                return {"length": len(bytes(send_data)), "isSuccess": is_success}
        except Exception as e:
            logging.error(e)
        return
