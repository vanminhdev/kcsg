using KcsWriteLog.Models.Request;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace KcsWriteLog.Services.Interfaces
{
    public interface IActivityLogService
    {
        public Task WriteLog(LogModel model);
    }
}
