﻿using KcsWriteLog.Models;
using KcsWriteLog.ViewModels;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
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
    public class VersionController : ControllerBase
    {
        private readonly ILogger<VersionController> _logger;
        private readonly KCS_DATAContext _context;

        public VersionController(ILogger<VersionController> logger)
        {
            _logger = logger;
            _context = new KCS_DATAContext();
        }

        [HttpGet]
        [Route("get-version")]
        public IActionResult GetVersion([Required] string ip)
        {
            var version = _context.VersionData.FirstOrDefault(o => o.Ip == ip);
            if (version != null)
            {
                return Ok(new { version = version.Ver });
            }
            return Ok(new { version = 0 });
        }

        [HttpGet]
        [Route("get-versions")]
        public IActionResult GetVersions()
        {
            var ips = _context.ControllerIps.Where(o => o.IsActive != null && o.IsActive.Value).Select(o => o.RemoteIp).ToList();
            var versions = _context.VersionData.Where(o => ips.Contains(o.Ip)).Select(o => new { ip = o.Ip, version = o.Ver }).ToList();
            return Ok(versions);
        }

        [HttpPost]
        [Route("update-version")]
        public IActionResult UpdateVersion([FromBody] UpdateVersionModel update)
        {
            var version = _context.VersionData.FirstOrDefault(o => o.Ip == update.Ip);
            if (version != null)
            {
                version.Ver = update.Version;
            }
            else
            {
                _context.VersionData.Add(new VersionDatum
                {
                    Ip = update.Ip,
                    Ver = update.Version
                });
            }
            _context.SaveChanges();
            return Ok();
        }

        [HttpPut]
        [Route("reset-version")]
        public IActionResult ResetVersion()
        {
            var versions = _context.VersionData.ToList();
            foreach(var ver in versions)
            {
                ver.Ver = 0;
            }
            _context.SaveChanges();
            return Ok();
        }
    }
}
