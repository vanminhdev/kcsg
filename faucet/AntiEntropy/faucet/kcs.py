import json
import logging
import os
import uuid
from datetime import datetime
import random

import requests

from faucet.process_file_queue import ProcessFileQueue, read_json_file, write_json_file, read_txt_file

kinds = {
        'ONOS': 'ONOS',
        'Faucet': 'Faucet',
}

class Kcs:
    file_path = {
        'listip': '/tmp/listip.json',
        'version': '/tmp/version.json'
    }
    local_ip = ''
    member_list = []
    visited_list = []

    @staticmethod
    def set_member_list():
        print('SETTING MEMBER LIST...')
        if not os.path.isfile(Kcs.file_path['listip']):
            return
        f = open(Kcs.file_path['listip'], 'r')
        info = json.load(f)
        f.close()

        communication = info['communication']
        for member in communication:
            new_member = {
                'ip': member['ip'],
                'kind': member['controller']
            }
            Kcs.member_list.append(new_member)

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
            data = json.dumps(str(r.text))
            Kcs.set_local_ip()

            local_log_file = '/tmp/' + Kcs.local_ip + '.json'
            print(local_log_file)

            grab = {'data': {}}
            ProcessFileQueue.q.put((read_json_file, local_log_file, grab))
            ProcessFileQueue.q.join()

            content = grab['data']
            if 'data' not in content:
                content = {
                    'data': []
                }


            content_data = list(content['data'])
            content_data.append({
                'id': str(uuid.uuid4()),
                'event_type': event_type,
                'time': str(datetime.now()),
                'data': data
            })
            content['data'] = content_data
            print(content)
            ProcessFileQueue.q.put((write_json_file, local_log_file, content))
            ProcessFileQueue.q.join()

            local_ver = int(Kcs.get_version_by_ip(Kcs.local_ip))
            print(local_ver)
            local_ver += 1
            Kcs.update_version_file([{'ip': Kcs.local_ip, 'version': local_ver}])

        except Exception as e:
            logging.error(e)

    @staticmethod
    def get_version_by_ip(ip):
        if os.path.isfile(Kcs.file_path['version']):
            with open(Kcs.file_path['version'], 'r') as f:
                versions = json.load(f)
            return versions[ip] if ip in versions else 0
        return '0'

    @staticmethod
    def random_send_notify():
        print('SENDING RANDOM NOTIFY...')

        if not Kcs.member_list or len(Kcs.member_list) < 1:
            Kcs.set_member_list()
            Kcs.set_local_ip()
            Kcs.visited_list = []

        member_index = list(range(0, len(Kcs.member_list)))
        if not member_index:
            return
        index = random.choice(member_index)
        node = Kcs.member_list.pop(index)

        print(node)

        Kcs.visited_list.append(node)
        ip = node['ip']
        kind = node['kind']

        try:
            try:
                Kcs.send_versions_file(ip, kind)
            except requests.exceptions.RequestException as e:  # This is the correct syntax
                print('exception requests: ')
                logging.error(e)

        except Exception as e:
            print('exception: ')
            logging.error(e)

    @staticmethod
    def send_versions_file(ip, kind):
        print('SENDING_VERSIONS_FILE')
        headers = {"Authorization": "Basic a2FyYWY6a2FyYWY=",
                   "Content-Type": "application/json",
                   "Accept": "application/json"}

        grab = {'data': {}}
        ProcessFileQueue.q.put((read_json_file, Kcs.file_path['version'], grab))
        ProcessFileQueue.q.join()
        data = grab['data'] or {}

        if kind == kinds['Faucet']:
            api = 'http://' + ip + ':8080/faucet/sina/versions/get-new'
            print(api)

            send_data = json.dumps(data)
            print('send_data: ' + send_data)

            r = requests.post(url=api, data=send_data, headers=headers)
            print('r.text: ' + r.text)
            res = json.loads(r.text)
            Kcs.send_log_files(ip, res, kind)
        elif kind == kinds['ONOS']:
            api = 'http://' + ip + ':8181/onos/kcsg/communicate/compareVersions'
            print(api)

            send_data = json.dumps(data)
            print(send_data)

            r = requests.post(url=api, data=send_data, headers=headers)
            res = json.loads(r.text)
            print(res)
            Kcs.send_log_files(ip, res, kind)

    @staticmethod
    def send_log_files(ip, list_ip, kind):
        if not list_ip:
            return
        print('SENDING_LOG_FILES')
        headers = {'Authorization': 'Basic a2FyYWY6a2FyYWY=',
                   'Content-Type': 'application/json',
                   'Accept': 'application/json'}
        data = []

        grab = {'data': {}}
        ProcessFileQueue.q.put((read_json_file, Kcs.file_path['version'], grab))
        ProcessFileQueue.q.join()
        versions = grab['data'] or {}

        if kind == kinds['Faucet']:
            api = 'http://' + ip + ':8080/faucet/sina/log/update'
            print(api)

            for ip in list_ip:
                item = {
                    'ip': ip,
                    'content': '',
                    'version': ''
                }

                grab = {'data': {}}
                ProcessFileQueue.q.put((read_txt_file, '/tmp/' + ip + '.json', grab))
                ProcessFileQueue.q.join()
                content = grab['data'] or ''

                item['content'] = content
                item['version'] = versions[ip]
                data.append(item)

            send_data = json.dumps(data)
            print('send_data')
            print(send_data)
            r = requests.post(url=api, data=send_data, headers=headers)
            print(r.text)
        elif kind == kinds['ONOS']:
            api = 'http://' + ip + ':8181/onos/kcsg/communicate/updateNewLog'
            print(api)

            for ip in list_ip:
                item = {
                    'ip': ip,
                    'data': '',
                    'ver': ''
                }

                grab = {'data': {}}
                ProcessFileQueue.q.put((read_txt_file, '/tmp/' + ip + '.json', grab))
                ProcessFileQueue.q.join()
                content = grab['data'] or ''

                item['data'] = json.dumps(content)
                item['ver'] = versions[ip]
                data.append(item)

            send_data = json.dumps(data)
            print('send_data')
            print(send_data)
            r = requests.put(url=api, data=send_data, headers=headers)
            print('result:')
            print(r)
