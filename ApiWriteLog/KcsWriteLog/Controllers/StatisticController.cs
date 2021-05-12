using KcsWriteLog.Models;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace KcsWriteLog.Controllers
{
    [Route("api/[controller]")]
    [ApiController]
    public class StatisticController : ControllerBase
    {
        private readonly ILogger<StatisticController> _logger;
        private readonly KCS_DATAContext _context;

        public StatisticController(ILogger<StatisticController> logger)
        {
            _logger = logger;
            _context = new KCS_DATAContext();
        }

        [Route("get-num-success")]
        [HttpGet]
        public IActionResult GetNumSuccess()
        {
            var list = _context.DataTrainings.Where(o => o.ClientMetric != TimeSpan.Zero).Select(o => new
            {
                o.Id,
                o.IsVersionSuccess,
                o.Time
            });
            _context.Database.ExecuteSqlRaw("truncate table Temp");
            int skip = 0;
            int take = 100;
            int id = 1;
            list.Take(100);
            while (true)
            {
                var subList = list.Skip(skip).Take(take);
                if (subList.Count() == 0)
                {
                    break;
                }
                skip += take;
                var test = subList.Count();
                var temp = new Temp
                {
                    Id = id++,
                    Num = subList.Where(o => o.IsVersionSuccess).Count(),
                    TimeRun = DateTime.Now
                };
                _context.Temps.Add(temp);
            }

            _context.SaveChanges();
            return Ok(list);
        }

        [Route("thong-ke")]
        [HttpGet]
        public IActionResult ThongKe()
        {
            var list = _context.DataTrainings.Where(o => o.ClientMetric != TimeSpan.Zero).Select(o => new
            {
                o.Id,
                o.VstalenessMax,
                o.VstalenessMin,
                o.VstalenessAvg,
                o.TstalenessAvg
            });
            _context.Database.ExecuteSqlRaw("truncate table Temp");
            int skip = 0;
            int take = 100;
            int id = 1;
            list.Take(100);
            while (true)
            {
                var subList = list.Skip(skip).Take(take);
                if (subList.Count() == 0)
                {
                    break;
                }
                skip += take;
                var test = subList.Count();
                var temp = new Temp
                {
                    Id = id++,
                    TimeRun = DateTime.Now
                };
                _context.Temps.Add(temp);
            }

            _context.SaveChanges();
            return Ok(list);
        }
    }
}
