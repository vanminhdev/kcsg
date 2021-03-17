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
            var datas = _context.DataTrainings.ToList();
            return Ok(datas);
        }
    }
}
