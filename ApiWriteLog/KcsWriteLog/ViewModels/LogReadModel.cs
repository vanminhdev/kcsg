using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace KcsWriteLog.ViewModels
{
    public class LogReadModel
    {
        public string LocalIp { get; set; }
        public string SrcIp { get; set; }
        public string DstIp { get; set; }
        public int Version { get; set; }
        public DateTime Start { get; set; }
        public DateTime End { get; set; }
        public bool IsSuccess { get; set; }
        public int Length { get; set; }
    }
}
