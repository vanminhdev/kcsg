using System;
using System.Collections.Generic;

#nullable disable

namespace KcsWriteLog.Models
{
    public partial class Config
    {
        public int Id { get; set; }
        public int R { get; set; }
        public int W { get; set; }
        public DateTime Time { get; set; }
    }
}
