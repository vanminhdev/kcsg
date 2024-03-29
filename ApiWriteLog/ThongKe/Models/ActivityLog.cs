﻿using System;
using System.Collections.Generic;

#nullable disable

namespace ThongKe.Models
{
    public partial class ActivityLog
    {
        public long Id { get; set; }
        public string IpUpdate { get; set; }
        public long? VersionUpdate { get; set; }
        public DateTime? TimeUpdate { get; set; }
        public DateTime? EntryTime { get; set; }
        public string IpFrom { get; set; }
    }
}
