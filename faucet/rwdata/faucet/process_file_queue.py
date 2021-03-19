import json
import os
from queue import Queue
import threading


class ProcessFileQueue:
    q = Queue()

    @staticmethod
    def start():
        print('WORKER STARTED')
        threading.Thread(target=ProcessFileQueue.worker, daemon=True).start()

    @staticmethod
    def worker():
        while True:
            item = ProcessFileQueue.q.get()
            if item:
                func = item[0]
                args = item[1:]
                func(*args)
                ProcessFileQueue.q.task_done()


def read_json_file(path, grab):
    if os.path.isfile(path):
        with open(path, 'r') as f:
            grab['data'] = json.load(f)
    else:
        grab['data'] = {}


def read_txt_file(path, grab):
    if os.path.isfile(path):
        with open(path, 'r') as f:
            grab['data'] = f.read()
    else:
        grab['data'] = {}


def write_json_file(path, content):
    with open(path, 'w+') as f:
        json.dump(content, f)


def write_txt_file(path, content):
    with open(path, 'w+') as f:
        f.write(content)
