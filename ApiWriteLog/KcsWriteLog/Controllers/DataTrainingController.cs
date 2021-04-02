using KcsWriteLog.Models;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging;
using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using System.Linq;
using System.Threading.Tasks;

namespace KcsWriteLog.Controllers
{
    [Route("api/[controller]")]
    [ApiController]
    public class DataTrainingController : ControllerBase
    {
        private readonly ILogger<DataTrainingController> _logger;
        private readonly KCS_DATAContext _context;

        public DataTrainingController(ILogger<DataTrainingController> logger)
        {
            _logger = logger;
            _context = new KCS_DATAContext();
        }

        [HttpGet]
        [Route("get-all")]
        public IActionResult GetAll()
        {
            var datas = _context.DataTrainings.Select(o => new { 
                o.Id,
                ClientMetric = o.ClientMetric.Milliseconds,
                StaleMetric = o.StaleMetric.Milliseconds,
                o.Overhead,
                o.IsSuccess,
                o.Time
            }).ToList();
            return Ok(datas);
        }

        [HttpDelete]
        [Route("delete-all")]
        public IActionResult DeleteAll()
        {
            _context.DataTrainings.FromSqlRaw("TRUNCATE TABLE [DataTraining]");
            _context.SaveChanges();
            return Ok();
        }

        [HttpGet]
        [Route("get-data-from-time")]
        public IActionResult GetDataFromTime([Required] DateTime? fromTime)
        {
            var datas = _context.DataTrainings.Where(o => o.Time >= fromTime).Select(o => new { 
                o.Id,
                ClientMetric = o.ClientMetric.TotalMilliseconds,
                StaleMetric = o.StaleMetric.TotalMilliseconds,
                o.IsSuccess,
                o.Time,
                o.R,
                o.W
            }).ToList();
            return Ok(datas);
        }
    }
}
