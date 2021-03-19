﻿using System;
using System.Collections.Generic;

#nullable disable

namespace KcsWriteLog.Models
{
    public partial class DataTraining
    {
        public int Id { get; set; }
        public TimeSpan ClientMetric { get; set; }
        public TimeSpan StaleMetric { get; set; }
        public int CountFalse { get; set; }
        public int Overhead { get; set; }
        public DateTime Time { get; set; }
    }
}
