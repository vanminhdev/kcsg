using KcsWriteLog.Models;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace KcsWriteLog.Services.Interfaces
{
    public interface IRemoteIpService
    {
        public Task<List<ControllerIp>> GetCommunicationIpsAsync(string ip);
        public Task<ControllerIp> GetControllerIpAsync(string ip);
    }
}
