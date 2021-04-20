#-*- coding: utf-8 -*-
import time
from datetime import datetime
import os

config_file = '/home/onos/Downloads/mininet/config.txt'
commands = [
    {
        'value': "net.configLinkStatus('h1', 's1', 'down')",
    },
    {
        'value': "net.pingAll()",
    },
    {
        'value': "net.configLinkStatus('h2', 's2', 'down')",
    }
]

print_pause = True

def is_continue(config_file = config_file):
    f = open(config_file,'r')
    value = f.read()
    f.close
    return value == '1'

def init(config_file = config_file):
    if os.path.exists(config_file):
        f = open(config_file, 'w+')
        f.write('1')
        f.close()

init()

while(True):
    if is_continue():
        for cmd in commands:
            #Thay đổi loss và delay trên link
            print('%s'%(cmd['value']))
            exec(cmd['value'])
            print('Done')
            time.sleep(5)
        print_pause = True
    elif print_pause:
        print('...Pause...')
        print_pause = False
# Tạo file config với giá trị là '1'
# py2: py execfile("<ten_file>")
# py3: py exec(open("<ten_file>").read())
