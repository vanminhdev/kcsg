﻿using System;
using System.Collections.Generic;

#nullable disable

namespace KcsWriteLog.Models
{
    public partial class LogQlearningWrite
    {
        public int Id { get; set; }
        public int NumViolations { get; set; }
        public DateTime TimeRun { get; set; }
    }
}