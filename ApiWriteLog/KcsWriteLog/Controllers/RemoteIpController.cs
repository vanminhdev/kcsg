using KcsWriteLog.Models.Request;
using KcsWriteLog.Services.Implements;
using KcsWriteLog.Services.Interfaces;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace KcsWriteLog.Controllers
{
    [Route("api/[controller]")]
    [ApiController]
    public class RemoteIpController : ControllerBase
    {
        private readonly IRemoteIpService _remoteIpService;
        public RemoteIpController(IRemoteIpService _remoteIpService)
        {
            this._remoteIpService = _remoteIpService;
        }
        [HttpGet]
        [Route("list-ip")]
        public async Task<IActionResult> GetListIp()
        {
            try
            {
                string clientIp = HttpContext.Connection.RemoteIpAddress.ToString();
                var communcationIp = await _remoteIpService.GetCommunicationIpsAsync(clientIp);
                var thisIp = await _remoteIpService.GetControllerIpAsync(clientIp);

                var listIp = new ListIpModel()
                {
                    localIp = clientIp,
                    controller = thisIp != null ? thisIp.ControllerType : "",
                    communication = new List<CommunicationMember>()
                };

                foreach (var member in communcationIp)
                {
                    listIp.communication.Add(new CommunicationMember
                    {
                        ip = member.RemoteIp,
                        controller = member.ControllerType
                    });
                };

                //string json = JsonConvert.SerializeObject(listIp);
                return Ok(listIp);
            }
            catch (Exception ex)
            {
                return BadRequest(new { status = 1, message = ex.Message });
            }
        }
    }
}
