﻿using System;
using System.Collections.Generic;

#nullable disable

namespace ThongKe.Models
{
    public partial class LogLatencyRead
    {
        public int Id { get; set; }
        public double Latency { get; set; }
        public DateTime TimeRun { get; set; }
    }
}
