using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace KcsWriteLog.Models.Request
{
    public class ListIpModel
    {
        public string localIp { get; set; }
        public string controller { get; set; }
        public List<CommunicationMember> communication { get; set; }
}

    public class CommunicationMember
    {
        public string ip { get; set; }
        public string controller { get; set; }
    }

}
