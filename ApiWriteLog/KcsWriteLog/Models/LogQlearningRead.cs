﻿using System;
using System.Collections.Generic;

#nullable disable

namespace KcsWriteLog.Models
{
    public partial class LogQlearningRead
    {
        public int Id { get; set; }
        public int NumViolations { get; set; }
        public DateTime TimeRun { get; set; }
    }
}
