import json
import logging
import os
import pathlib
import random
import uuid
from datetime import datetime
from threading import Timer

import requests

from faucet.process_file_queue import ProcessFileQueue, read_json_file, write_append_line_json_file, \
    read_txt_file_from_line

kinds = {
    'ONOS': 'ONOS',
    'Faucet': 'Faucet',
    'ODL': 'ODL'
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
    member_list = []
    visited_list = []
    temp_versions = {}

    @staticmethod
    def start_scan():
        print('scanning...')
        Kcs.random_send_notify()
        Timer(1, Kcs.start_scan).start()

    @staticmethod
    def write_response_log(status, msg):
        try:
            mode = 'a' if os.path.isfile(Kcs.file_path['log']) else 'w+'
            with open(Kcs.file_path['log'], mode) as f:
                f.write(str(datetime.now()) + ' | ' + str(status) + ': ' + msg + '\n')
        except Exception as e:
            print('write response log got error')
            logging.error(e)

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

            local_log_file = temp_data_dir + Kcs.local_ip + '.json'
            print(local_log_file)

            content = {
                'id': str(uuid.uuid4()),
                'event_type': event_type,
                'time': str(datetime.now()),
                'data': data
            }
            ProcessFileQueue.q.put((write_append_line_json_file, local_log_file, json.dumps(content)))
            ProcessFileQueue.q.join()

            local_ver = int(Kcs.get_version_by_ip(Kcs.local_ip))
            print(local_ver)
            local_ver += 1
            Kcs.call_log_api(Kcs.local_ip, Kcs.local_ip, local_ver)
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
        selected_node = Kcs.member_list.pop(index)

        print(selected_node)

        Kcs.visited_list.append(selected_node)
        ip = selected_node['ip']
        kind = selected_node['kind']

        try:
            Kcs.send_versions_file(ip, kind)
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

        api = ''

        try:
            if kind == kinds['Faucet']:
                api = 'http://' + ip + ':8080/faucet/sina/versions/get-new'
                print(api)

                send_data = json.dumps(data)
                print('send_data: ' + send_data)
                r = requests.post(url=api, data=send_data, headers=headers)
                print('r.text: ' + r.text)
                res = json.loads(r.text)
                Kcs.send_log_files(ip, res, kind)
                Kcs.write_response_log(200, ip + ' | ' + api)
            elif kind == kinds['ONOS']:
                api = 'http://' + ip + ':8181/onos/kcsg/communicate/compareVersions'
                print(api)

                send_data = json.dumps(data)
                print('send_data: ' + send_data)

                r = requests.post(url=api, data=send_data, headers=headers)
                res = json.loads(r.text)
                print('res: ' + r.text)
                Kcs.send_log_files(ip, res, kind)
                Kcs.write_response_log(200, ip + ' | ' + api)
            elif kind == kinds['ODL']:
                headers['Authorization'] = 'Basic YWRtaW46YWRtaW4='
                api = 'http://' + ip + ':8181/restconf/operations/sina:compareVersions'
                print(api)

                odl_input = {
                    'input': {
                        'data': json.dumps(data)
                    }
                }
                send_data = json.dumps(odl_input)
                print('send_data: ' + send_data)

                r = requests.post(url=api, data=send_data, headers=headers)
                res = json.loads(r.text)
                print('res: ' + r.text)

                output_odl = res['output']
                ips_odl = []
                print(output_odl)
                print('ips' in output_odl)
                if 'ips' in output_odl:
                    ips_odl = json.loads(output_odl['ips'])
                    print('in')

                print(ips_odl)
                Kcs.send_log_files(ip, ips_odl, kind)
                Kcs.write_response_log(200, ip + ' | ' + api)
        except requests.exceptions.RequestException as e:  # This is the correct syntax
            print('exception requests: ')
            Kcs.write_response_log(500, ip + ' | ' + api + ' | ' + e.strerror)
            logging.error(e)

    @staticmethod
    def send_log_files(ip, list_ip_version, kind):
        if not list_ip_version:
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

        api = ''

        try:
            if kind == kinds['Faucet']:
                api = 'http://' + ip + ':8080/faucet/sina/log/update'
                print(api)

                for info in list_ip_version:
                    info_ip = info['ip']
                    info_current_line = info['version']

                    item = {
                        'ip': info_ip,
                        'content': '',
                        'version': ''
                    }

                    grab = {'data': {}}
                    ProcessFileQueue.q.put(
                        (read_txt_file_from_line, temp_data_dir + info_ip + '.json', grab, info_current_line))
                    ProcessFileQueue.q.join()
                    content = grab['data'] or ''
                    print('content: ' + content)

                    item['content'] = content
                    item['version'] = versions[info_ip]
                    data.append(item)
                    print('DATA')
                    print(data)

                send_data = json.dumps(data)
                print('send_data')
                print(send_data)
                r = requests.post(url=api, data=send_data, headers=headers)
                print(r.text)
                Kcs.write_response_log(200, ip + ' | ' + api)
            elif kind == kinds['ONOS']:
                api = 'http://' + ip + ':8181/onos/kcsg/communicate/updateNewLog'
                print(api)

                for info in list_ip_version:
                    info_ip = info['ip']
                    info_current_line = info['ver']
                    print('onos: ' + info_ip + str(info_current_line))
                    print(info)
                    item = {
                        'ip': info_ip,
                        'data': '',
                        'ver': ''
                    }

                    grab = {'data': {}}
                    ProcessFileQueue.q.put((read_txt_file_from_line, temp_data_dir + info_ip + '.json', grab, info_current_line))
                    ProcessFileQueue.q.join()
                    content = grab['data'] or ''
                    print(content)
                    item['data'] = content
                    item['ver'] = versions[info_ip]
                    data.append(item)

                send_data = json.dumps(data)
                print('send_data')
                print(send_data)
                r = requests.put(url=api, data=send_data, headers=headers)
                print('result:')
                print(r)
                Kcs.write_response_log(200, ip + ' | ' + api)
            elif kind == kinds['ODL']:
                headers['Authorization'] = 'Basic YWRtaW46YWRtaW4='
                api = 'http://' + ip + ':8181/restconf/operations/sina:updateNewData'
                print(api)

                for info in list_ip_version:
                    info_ip = info['ip']
                    info_current_line = info['ver']
                    print('odl: ' + info_ip + str(info_current_line))
                    print(info)
                    item = {
                        'ip': info_ip,
                        'data': '',
                        'ver': ''
                    }

                    grab = {'data': {}}
                    ProcessFileQueue.q.put(
                        (read_txt_file_from_line, temp_data_dir + info_ip + '.json', grab, info_current_line))
                    ProcessFileQueue.q.join()
                    content = grab['data'] or ''
                    print(content)
                    item['data'] = content
                    item['ver'] = versions[info_ip]
                    data.append(item)

                odl_input = {
                    'input': {
                        'data': json.dumps(data)
                    }
                }
                send_data = json.dumps(odl_input)
                print('send_data')
                print(send_data)
                r = requests.post(url=api, data=send_data, headers=headers)
                print('result odl:')
                print(r)
                Kcs.write_response_log(200, ip + ' | ' + api)
        except requests.exceptions.RequestException as e:  # This is the correct syntax
            print('exception requests: ')
            Kcs.write_response_log(500, ip + ' | ' + api + ' | ' + e.strerror)
            logging.error(e)

    @staticmethod
    def call_log_api(sender_ip, receiver_ip, version):
        server_url = Kcs.get_server_url()
        api = server_url + '/api/log/write'
        send_data = {
            'ipSender': sender_ip,
            'ipReceiver': receiver_ip,
            'time': datetime.now().strftime("%Y-%m-%dT%H:%M:%S"),
            'version': version
        }
        headers = {"Content-Type": "application/json",
                   "Accept": "application/json"}
        print('call_log_api: ' + json.dumps(send_data))
        try:
            r = requests.post(url=api, data=json.dumps(send_data), headers=headers)
            print('r:')
            print(r.text)
        except requests.exceptions.RequestException as e:  # This is the correct syntax
            logging.error(e)

    @staticmethod
    def init_listip_config():
        server_url = Kcs.get_server_url()
        api = server_url + '/api/remoteIp/list-ip'
        headers = {"Content-Type": "application/json",
                   "Accept": "application/json"}
        try:
            r = requests.get(url=api, headers=headers)
            with open(Kcs.file_path['listip'], 'w+') as f:
                f.write(r.text)
        except requests.exceptions.RequestException as e:  # This is the correct syntax
            print('Error calling api listip')
            logging.error(e)
            if os.path.isfile(Kcs.file_path['listip']):
                with open(Kcs.file_path['listip'], 'w+') as f:
                    f.write("{\n" +
                            "\t\"localIp\": \"192.168.131.137\",\n" +
                            "\t\"controller\": \"Faucet\", \n" +
                            "\t\"communication\": [\n" +
                            "\t\t{\n" +
                            "\t\t\t\"ip\": \"192.168.131.138\", \n" +
                            "\t\t\t\"controller\": \"Faucet\"\n" +
                            "\t\t}\n" +
                            "\t]\n" +
                            "}")
        finally:
            ProcessFileQueue.start()
            Kcs.start_scan()

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
