﻿using System;
using System.Collections.Generic;

#nullable disable

namespace ThongKe.Models
{
    public partial class LogQlearningRatio
    {
        public int Id { get; set; }
        public double Ratio { get; set; }
        public DateTime TimeRun { get; set; }
    }
}
