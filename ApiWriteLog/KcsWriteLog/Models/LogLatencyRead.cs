using System;
using System.Collections.Generic;

#nullable disable

namespace KcsWriteLog.Models
{
    public partial class LogLatencyRead
    {
        public int Id { get; set; }
        public double Latency { get; set; }
        public DateTime TimeRun { get; set; }
    }
}
