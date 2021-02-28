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
        grab['data'] = ''


def read_txt_file_from_line(path, grab, line_number):
    grab['data'] = ''
    if os.path.isfile(path):
        with open(path, 'r') as f:
            for i, line in enumerate(f):
                if i >= line_number:
                    grab['data'] += line


def write_json_file(path, content):
    with open(path, 'w+') as f:
        json.dump(content, f)


def write_txt_file(path, content):
    with open(path, 'w+') as f:
        f.write(content)


def write_append_line_json_file(path, content):
    mode = 'a' if os.path.isfile(path) else 'w+'
    with open(path, mode) as f:
        if mode == 'a':
            f.write('\n')
        f.write(content)
