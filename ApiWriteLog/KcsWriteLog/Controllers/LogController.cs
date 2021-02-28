using KcsWriteLog.Models.Request;
using KcsWriteLog.Services.Interfaces;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Logging;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace KcsWriteLog.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class LogController : ControllerBase
    {
        private readonly ILogger<LogController> _logger;
        private readonly IActivityLogService _activityLogService;

        public LogController(ILogger<LogController> logger, IActivityLogService _activityLogService)
        {
            this._logger = logger;
            this._activityLogService = _activityLogService;
        }

        [Route("write")]
        [HttpPost]
        public async Task<IActionResult> PostWriteLog([FromBody] LogModel model)
        {
            try
            {
                if (string.IsNullOrEmpty(model.ip))
                {
                    model.ip = HttpContext.Connection.RemoteIpAddress.ToString();
                }
                await _activityLogService.WriteLog(model);
                return Ok();
            }
            catch (Exception ex)
            {
                return BadRequest(new { Message = ex.ToString() });
            }
        }
    }
}
