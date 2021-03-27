using KcsWriteLog.Models;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Logging;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace KcsWriteLog.Controllers
{
    [Route("api/[controller]")]
    [ApiController]
    public class ConfigController : ControllerBase
    {
        private readonly ILogger<ConfigController> _logger;
        private readonly KCS_DATAContext _context;

        public ConfigController(ILogger<ConfigController> logger)
        {
            _logger = logger;
            _context = new KCS_DATAContext();
        }

        [HttpGet]
        [Route("get-configrw")]
        public IActionResult GetConfigRW()
        {
            var config = _context.Configs.OrderByDescending(o => o.Time).FirstOrDefault();
            if (config != null)
            {
                return Ok(new { r = config.R, w = config.W });
            }
            return Ok(new { r = 0, w = 0 });
        }

        [HttpPost]
        [Route("set-configrw")]
        public IActionResult SetConfigRW(int r, int w)
        {
            try
            {
                _context.Configs.Add(new Config
                {
                    R = r,
                    W = w,
                    Time = DateTime.Now
                });
                _context.SaveChanges();
            }
            catch (Exception e)
            {
                return StatusCode(StatusCodes.Status500InternalServerError, new { messaga = e.Message });
            }
            return Ok();
        }
    }
}
