using KcsWriteLog.Models;
using KcsWriteLog.Models.Request;
using KcsWriteLog.Services.Interfaces;
using Microsoft.EntityFrameworkCore;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace KcsWriteLog.Services.Implements
{
    public class RemoteIpService : IRemoteIpService
    {
        private readonly KCS_DATAContext _context;
        public RemoteIpService(KCS_DATAContext _context)
        {
            this._context = _context;
        }
        public async Task<List<ControllerIp>> GetCommunicationIpsAsync(string ip)
        {
            return await _context.ControllerIps.Where(ci => (ci.IsActive ?? false) && ci.RemoteIp != ip).AsNoTracking().ToListAsync();
        }

        public async Task<ControllerIp> GetControllerIpAsync(string ip)
        {
            return await _context.ControllerIps.FirstOrDefaultAsync(ci => ci.RemoteIp == ip);
        }
    }
}
