import json
import os
import pathlib
import requests


current_working_dir = str(pathlib.Path().absolute())
temp_data_dir = current_working_dir + '/tmp/'

class HandleCallServer:

    @staticmethod
    def get_server_url():
        config = {
            'serverUrl': 'http://192.168.43.176:8085'
        }
        if os.path.isfile(current_working_dir + '/config.json'):
            with open(current_working_dir + '/config.json', 'r') as f:
                config = json.load(f)
        return config['serverUrl']

    @staticmethod
    def get_rw_config():
        headers = {"Content-Type": "application/json",
                   "Accept": "application/json"}
        response = requests.get(url=HandleCallServer.get_server_url() + "/api/config/get-configrw", headers=headers)
        body = json.loads(response.text)
        print(body)
        return {"r": body["r"], "w": body["w"]}

    @staticmethod
    def get_version_from_server(ip):
        headers = {"Content-Type": "application/json",
                   "Accept": "application/json"}
        response = requests.get(url=HandleCallServer.get_server_url() + "/api/version/get-version?ip=" + ip,
                                headers=headers)
        body = json.loads(response.text)
        print(body)
        return body["version"]

    @staticmethod
    def update_version(ip, version):
        headers = {"Content-Type": "application/json",
                   "Accept": "application/json"}
        data = {"ip": ip, "version": version}
        response = requests.post(url=HandleCallServer.get_server_url() + "/api/version/update-version",
                                 data=json.dumps(data), headers=headers)
        print("update version to server: ", response.status_code)

    @staticmethod
    def send_log_read(data):
        headers = {"Content-Type": "application/json",
                   "Accept": "application/json"}
        try:
            response = requests.post(url=HandleCallServer.get_server_url() + "/api/log/log-read",
                                     data=json.dumps(data), headers=headers)
            print("send log read to server: ", response.status_code, response.text)
        except Exception as e:
            print(e)

    @staticmethod
    def send_log_write(data):
        headers = {"Content-Type": "application/json",
                   "Accept": "application/json"}
        try:
            response = requests.post(url=HandleCallServer.get_server_url() + "/api/log/log-write",
                                     data=json.dumps(data), headers=headers)
            print("send log write to server: ", response.status_code, response.text)
        except Exception as e:
            print(e)
