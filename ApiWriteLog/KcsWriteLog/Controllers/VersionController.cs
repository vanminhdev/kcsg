using KcsWriteLog.Models;
using KcsWriteLog.ViewModels;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Logging;
using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using System.Linq;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
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
        [Route("get-versions-test")]
        public async Task<IActionResult> GetVersionsTest()
        {
            var ips = _context.ControllerIps.Where(o => o.IsActive != null && o.IsActive.Value).Select(o => o.RemoteIp).ToList();
            var versions = _context.VersionData.Where(o => ips.Contains(o.Ip)).Select(o => new { ip = o.Ip, version = o.Ver }).ToList();

            var controllers = _context.ControllerIps.Where(o => o.IsActive != null && o.IsActive.Value).ToList();
            _context.SaveChanges();

            Dictionary<string, string> dic = new Dictionary<string, string>();
            dic.Add("server", JsonSerializer.Serialize(versions));
            HttpClient client = new HttpClient();
            foreach (var ctrl in controllers)
            {
                if (ctrl.ControllerType == "ONOS")
                {
                    try
                    {
                        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Basic", "a2FyYWY6a2FyYWY=");
                        var res = await client.GetAsync($"http://{ctrl.RemoteIp}:8181/onos/rwdata/communicate/get-versions");
                        dic.Add(ctrl.RemoteIp, await res.Content.ReadAsStringAsync());
                    }
                    catch (Exception e)
                    {
                        _logger.LogError(e.Message);
                    }
                }
                else if (ctrl.ControllerType == "Faucet")
                {
                    try
                    {
                        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Basic", "a2FyYWY6a2FyYWY=");
                        HttpResponseMessage res = await client.GetAsync($"http://{ctrl.RemoteIp}:8080/faucet/sina/versions/get-versions");
                        dic.Add(ctrl.RemoteIp, await res.Content.ReadAsStringAsync());
                    }
                    catch (Exception e)
                    {
                        _logger.LogError(e.Message);
                    }
                }
                else if (ctrl.ControllerType == "ODL")
                {
                    try
                    {
                        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Basic", "YWRtaW46YWRtaW4=");
                        HttpResponseMessage res = await client.PostAsync($"http://{ctrl.RemoteIp}:8181/restconf/operations/sina:getVersions",
                            new StringContent("", Encoding.UTF8, "application/json"));
                        dic.Add(ctrl.RemoteIp, await res.Content.ReadAsStringAsync());
                    }
                    catch (Exception e)
                    {
                        _logger.LogError(e.Message);
                    }
                }
            }
            return Ok(dic);
        }

        [HttpGet]
        [Route("get-versions")]
        public IActionResult GetVersions()
        {
            var ips = _context.ControllerIps.Where(o => o.IsActive != null && o.IsActive.Value).Select(o => o.RemoteIp).ToList();
            var versions = _context.VersionData.Where(o => ips.Contains(o.Ip)).Select(o => new { ip = o.Ip, version = o.Ver }).ToList();
            return Ok(versions);
        }

        [HttpPut]
        [Route("reset-versions")]
        public async Task<IActionResult> ResetVersions()
        {
            var controllers = _context.ControllerIps.Where(o => o.IsActive != null && o.IsActive.Value).ToList();
            var ips = controllers.Select(o => o.RemoteIp).ToList();
            var versions = _context.VersionData.Where(o => ips.Contains(o.Ip)).ToList();
            foreach (var ver in versions)
            {
                ver.Ver = 0;
            }
            _context.SaveChanges();

            HttpClient client = new HttpClient();
            foreach (var ctrl in controllers)
            {
                if (ctrl.ControllerType == "ONOS")
                {
                    try
                    {
                        StringContent content = new StringContent("", Encoding.UTF8, "application/json");
                        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Basic", "a2FyYWY6a2FyYWY=");
                        HttpResponseMessage res = await client.PutAsync($"http://{ctrl.RemoteIp}:8181/onos/rwdata/communicate/reset-versions", content);
                    }
                    catch (Exception e)
                    {
                        _logger.LogError(e.Message);
                    }
                }
                else if (ctrl.ControllerType == "Faucet")
                {
                    try
                    {
                        StringContent content = new StringContent("", Encoding.UTF8, "application/json");
                        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Basic", "a2FyYWY6a2FyYWY=");
                        HttpResponseMessage res = await client.PutAsync($"http://{ctrl.RemoteIp}:8080/faucet/sina/versions/reset-versions", content);
                    }
                    catch (Exception e)
                    {
                        _logger.LogError(e.Message);
                    }
                }
                else if (ctrl.ControllerType == "ODL")
                {
                    try
                    {
                        StringContent content = new StringContent("", Encoding.UTF8, "application/json");
                        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Basic", "YWRtaW46YWRtaW4=");
                        HttpResponseMessage res = await client.PostAsync($"http://{ctrl.RemoteIp}:8181/restconf/operations/sina:resetVersions", content);
                    }
                    catch (Exception e)
                    {
                        _logger.LogError(e.Message);
                    }
                }
            }

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
    }
}
