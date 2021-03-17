using System;
using System.Collections.Generic;

#nullable disable

namespace KcsWriteLog.Models
{
    public partial class LogRead
    {
        public int Id { get; set; }
        public string LocalIp { get; set; }
        public string SrcIp { get; set; }
        public string DstIp { get; set; }
        public DateTime Start { get; set; }
        public DateTime End { get; set; }
        public bool IsSuccess { get; set; }
        public int Length { get; set; }
    }
}
