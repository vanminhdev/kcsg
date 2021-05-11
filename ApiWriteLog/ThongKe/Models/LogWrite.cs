using System;
using System.Collections.Generic;

#nullable disable

namespace ThongKe.Models
{
    public partial class LogWrite
    {
        public int Id { get; set; }
        public string LocalIp { get; set; }
        public string SrcIp { get; set; }
        public string DstIp { get; set; }
        public int Version { get; set; }
        public DateTime Start { get; set; }
        public DateTime End { get; set; }
        public int Length { get; set; }
    }
}
