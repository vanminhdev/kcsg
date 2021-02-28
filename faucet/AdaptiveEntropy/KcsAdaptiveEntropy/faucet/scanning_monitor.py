from queue import Queue
from threading import Timer


class ScanningMonitor:
    scanning_interval = 5
    local_interval = 5
    changed_quantity = {
        'old': 0,
        'now': 0
    }
    max_size_queue = 3
    last_times_v = Queue(max_size_queue)

    @staticmethod
    def check_changed_items():
        now = ScanningMonitor.changed_quantity['now']
        old = ScanningMonitor.changed_quantity['old']

        ScanningMonitor.changed_quantity['now'] = 0
        ScanningMonitor.changed_quantity['old'] = now

        f1 = now
        f2 = old
        v = 0 if f1 == 0 and f2 == 0 else abs(f1 - f2) / (f1 + f2)
        print('f1, f2, v: ' + str(f1) + ' ' + str(f2) + ' ' + str(v))
        if ScanningMonitor.last_times_v.full():
            ScanningMonitor.last_times_v.get()
        ScanningMonitor.last_times_v.put(v)
        print(list(ScanningMonitor.last_times_v.queue))

        print('Queue: ')
        print(list(ScanningMonitor.last_times_v.queue))
        list_v = list(ScanningMonitor.last_times_v.queue)
        pivot = sum(list_v) / ScanningMonitor.max_size_queue
        ScanningMonitor.update_scanning_interval(pivot)

    @staticmethod
    def update_scanning_interval(pivot):
        if pivot > 0.6:
            ScanningMonitor.scanning_interval = 3
        elif pivot < 0.4:
            ScanningMonitor.scanning_interval = 10
        elif 0.4 <= pivot <= 0.6:
            ScanningMonitor.scanning_interval = 5
        print('interval: ' + str(ScanningMonitor.scanning_interval))

    @staticmethod
    def start():
        print('CHECKING CHANGED ITEMS...')
        ScanningMonitor.check_changed_items()
        Timer(ScanningMonitor.local_interval, ScanningMonitor.start).start()
