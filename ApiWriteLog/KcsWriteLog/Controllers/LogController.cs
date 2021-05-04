using KcsWriteLog.Models;
using KcsWriteLog.Models.Request;
using KcsWriteLog.Services.Interfaces;
using KcsWriteLog.ViewModels;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Logging;
using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
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
                bool isSuccess = false;
                foreach (var item in lst)
                {
                    if (item.IsSuccess)
                    {
                        isSuccess = true;
                    }

                    _context.LogReads.Add(new Models.LogRead
                    {
                        LocalIp = item.LocalIp,
                        SrcIp = item.SrcIp,
                        DstIp = item.DstIp,
                        Version = item.Version,
                        Start = item.Start,
                        End = item.End,
                        IsSuccess = item.IsSuccess,
                        Length = item.Length
                    });
                }

                _context.DataTrainings.Add(new DataTraining
                {
                    ClientMetric = lst.Max(o => o.End) - lst.Min(o => o.Start),
                    StaleMetric = TimeSpan.Zero,
                    Overhead = lst.Sum(o => o.Length),
                    Time = DateTime.Now,
                    IsVersionSuccess = isSuccess,
                });

                _context.SaveChanges();
                return Ok();
            }
            catch (Exception ex)
            {
                return StatusCode(StatusCodes.Status500InternalServerError, new { message = ex.ToString() });
            }
        }

        public class LogReadTestPingModel
        {
            public string TargetIp { get; set; }
            public DateTime Start { get; set; }
            public DateTime End { get; set; }
            public bool IsVersionSuccess { get; set; }
        }

        [Route("log-read-test-ping")]
        [HttpPost]
        public IActionResult LogReadTestPing([FromBody] LogReadTestPingModel logRead)
        {
            try
            {
                var log = new DataTraining
                {
                    ClientMetric = logRead.End - logRead.Start,
                    StaleMetric = TimeSpan.Zero,
                    Overhead = 0,
                    Time = DateTime.Now,
                    IsVersionSuccess = logRead.IsVersionSuccess
                };
                _context.DataTrainings.Add(log);
                _context.SaveChanges();
                return Ok(log.Id);
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
                        Version = item.Version,
                        Start = item.Start,
                        End = item.End,
                        Length = item.Length
                    });
                }

                _context.DataTrainings.Add(new DataTraining
                {
                    ClientMetric = TimeSpan.Zero,
                    StaleMetric = lst.Max(o => o.End) - lst.Min(o => o.Start),
                    Overhead = lst.Sum(o => o.Length),
                    Time = DateTime.Now,
                    IsVersionSuccess = true
                });
                _context.SaveChanges();
                return Ok();
            }
            catch (Exception ex)
            {
                return StatusCode(StatusCodes.Status500InternalServerError, new { message = ex.ToString() });
            }
        }

        [Route("log-ping")]
        [HttpPut]
        public IActionResult LogPing([Required]int? id, [Required]bool? isPingSuccess)
        {
            try
            {
                var log = _context.DataTrainings.FirstOrDefault(o => o.Id == id);
                if (log == null)
                {
                    return StatusCode(StatusCodes.Status404NotFound, new { message = "id not found" });
                }
                log.IsPingSuccess = isPingSuccess;
                log.TimeUpdate = DateTime.Now;
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
