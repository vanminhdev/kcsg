using System;
using System.Collections.Generic;

#nullable disable

namespace KcsWriteLog.Models
{
    public partial class ControllerIp
    {
        public int Id { get; set; }
        public string RemoteIp { get; set; }
        public string Port { get; set; }
        public string ControllerType { get; set; }
        public bool? IsActive { get; set; }
        public string Description { get; set; }
    }
}
