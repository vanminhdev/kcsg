using KcsWriteLog.Models;
using KcsWriteLog.Models.Request;
using KcsWriteLog.Services.Interfaces;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace KcsWriteLog.Services.Implements
{
    public class ActivityLogService : IActivityLogService
    {
        private readonly KCS_DATAContext _context;
        public ActivityLogService(KCS_DATAContext _context)
        {
            this._context = _context;
        }
        public async Task WriteLog(LogModel model)
        {
            try
            {
                var data = new ActivityLog()
                {
                    EntryTime = DateTime.Now,
                    IpUpdate = model.ip,
                    TimeUpdate = model.time,
                    VersionUpdate = model.version
                };
                await _context.ActivityLogs.AddAsync(data);
                await _context.SaveChangesAsync();
            }
            catch (Exception ex)
            {
                throw ex;
            }
        }
    }
}
