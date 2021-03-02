using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace KcsWriteLog.Models.Request
{
    public class LogModel
    {
        public string ipSender { get; set; }
        public string ipReceiver { get; set; }
        public DateTime time { get; set; }
        public int version { get; set; }
    }
}
