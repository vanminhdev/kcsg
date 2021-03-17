using KcsWriteLog.Models;
using KcsWriteLog.Models.Request;
using KcsWriteLog.Services.Interfaces;
using KcsWriteLog.ViewModels;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Logging;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Threading.Tasks;

namespace KcsWriteLog.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class LogController : ControllerBase
    {
        private readonly ILogger<LogController> _logger;
        private readonly IActivityLogService _activityLogService;
        private readonly KCS_DATAContext _context;

        public LogController(ILogger<LogController> logger, IActivityLogService _activityLogService)
        {
            _logger = logger;
            this._activityLogService = _activityLogService;
            _context = new KCS_DATAContext();
        }

        [Route("write")]
        [HttpPost]
        public async Task<IActionResult> PostWriteLog([FromBody] LogModel model)
        {
            try
            {
                DateTime s = new DateTime();
                DateTime b = new DateTime();
                var c = s - b;
                await _activityLogService.WriteLog(model);
                return Ok();
            }
            catch (Exception ex)
            {
                return BadRequest(new { Message = ex.ToString() });
            }
        }

        [Route("log-read")]
        [HttpPost]
        public IActionResult LogRead([FromBody] List<LogReadModel> lst)
        {
            try
            {
                foreach (var item in lst)
                {
                    _context.LogReads.Add(new Models.LogRead
                    {
                        LocalIp = item.LocalIp,
                        SrcIp = item.SrcIp,
                        DstIp = item.DstIp,
                        Start = item.Start,
                        End = item.End,
                        IsSuccess = item.IsSuccess,
                        Length = item.Length
                    });
                }
                _context.SaveChanges();
                return Ok();
            }
            catch (Exception ex)
            {
                return StatusCode(StatusCodes.Status500InternalServerError, new { message = ex.ToString() });
            }
        }

        [Route("log-write")]
        [HttpPost]
        public IActionResult LogWrite([FromBody] List<LogWriteModel> lst)
        {
            try
            {
                foreach (var item in lst)
                {
                    _context.LogWrites.Add(new Models.LogWrite
                    {
                        LocalIp = item.LocalIp,
                        SrcIp = item.SrcIp,
                        DstIp = item.DstIp,
                        Start = item.Start,
                        End = item.End,
                        Length = item.Length
                    });
                }
                _context.SaveChanges();
                return Ok();
            }
            catch (Exception ex)
            {
                return StatusCode(StatusCodes.Status500InternalServerError, new { message = ex.ToString() });
            }
        }
    }
}
