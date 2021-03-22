using KcsWriteLog.Models;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;

namespace KcsWriteLog.Services.HostedService
{
    public class TimerReadData : IHostedService, IDisposable
    {
        //private int executionCount = 0;
        private readonly ILogger<TimerReadData> _logger;
        private Timer _timer;
        private KCS_DATAContext _context;

        public TimerReadData(ILogger<TimerReadData> logger)
        {
            _logger = logger;
            _context = new KCS_DATAContext();
        }

        public Task StartAsync(CancellationToken stoppingToken)
        {
            _logger.LogInformation("Timer read data running.");
            _timer = new Timer(DoWorkAsync, null, TimeSpan.Zero, TimeSpan.FromSeconds(5));
            return Task.CompletedTask;
        }

        private async void DoWorkAsync(object state)
        {
            //var count = Interlocked.Increment(ref executionCount);
            var config = _context.Configs.OrderByDescending(o => o.Time).FirstOrDefault();
            if (config == null)
            {
                _logger.LogWarning("config is null");
                return;
            }
            var controllers = _context.ControllerIps.Where(o => o.IsActive != null && o.IsActive.Value).ToList();
            if (controllers.Count == 0)
            {
                _logger.LogWarning("controllers count = 0");
                return;
            }

            var random = new Random();
            int indexTarget = random.Next(controllers.Count);
            var targetReadIp = controllers[indexTarget];
            var verTarget = _context.VersionData.FirstOrDefault(o => o.Ip == targetReadIp.RemoteIp)?.Ver ?? -1;

            _logger.LogInformation($"target: {targetReadIp.RemoteIp}, R = {config.R}");
            for (int i = 0; i < config.R; i++)
            {
                if (controllers.Count == 0)
                {
                    break;
                }
                int index = random.Next(controllers.Count);
                var start = DateTime.Now;
                var controler = controllers[index];
                controllers.Remove(controler);
                _logger.LogInformation($"random: {controler.RemoteIp}");
                var isSuccess = await HandleReadDataAsync(targetReadIp.RemoteIp, verTarget, controler);
                var end = DateTime.Now;

                _context.LogReads.Add(new LogRead
                {
                    LocalIp = "ccdn",
                    SrcIp = targetReadIp.RemoteIp,
                    DstIp = controler.RemoteIp,
                    IsSuccess = isSuccess,
                    Length = 0,
                    Start = start,
                    End = end,
                    Version = verTarget
                });
            }
        }

        class ResGetVer
        {
            public int version { get; set; }
        }

        class ResGetVerODL
        {
            public class Output
            {
                public string result { get; set; }
            }

            public Output output { get; set; }
        }

        private async Task<bool> HandleReadDataAsync(string targetIp, int verTarget, ControllerIp fromController)
        {
            var verFromCtrl = -1;
            try
            {
                switch (fromController.ControllerType)
                {
                    case "ONOS":
                        var client = new HttpClient();
                        var content = new StringContent(JsonSerializer.Serialize(new { ip = targetIp }), Encoding.UTF8, "application/json");
                        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Basic", "a2FyYWY6a2FyYWY=");
                        var result = await client.PostAsync($"http://{fromController.RemoteIp}:8181/onos/rwdata/communicate/get-version", content);
                        if (result.IsSuccessStatusCode)
                        {
                            var resBody = await result.Content.ReadAsStringAsync();
                            var resVer = JsonSerializer.Deserialize<ResGetVer>(resBody);
                            verFromCtrl = resVer.version;
                        }
                        else
                        {
                            _logger.LogError($"onos ip: {fromController.RemoteIp} error:\n {await result.Content.ReadAsStringAsync()}");
                        }
                        break;
                    case "Faucet":
                        client = new HttpClient();
                        content = new StringContent(JsonSerializer.Serialize(new { ip = targetIp }), Encoding.UTF8, "application/json");
                        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Basic", "a2FyYWY6a2FyYWY=");
                        result = await client.PostAsync($"http://{fromController.RemoteIp}:8080/faucet/sina/versions/get-version", content);
                        if (result.IsSuccessStatusCode)
                        {
                            var resBody = await result.Content.ReadAsStringAsync();
                            var resVer = JsonSerializer.Deserialize<ResGetVer>(resBody);
                            verFromCtrl = resVer.version;
                        }
                        else
                        {
                            _logger.LogError($"onos ip: {fromController.RemoteIp} error:\n {await result.Content.ReadAsStringAsync()}");
                        }
                        break;
                    case "ODL":
                        client = new HttpClient();
                        var str = JsonSerializer.Serialize(new
                        {
                            input = new
                            {
                                data = JsonSerializer.Serialize(new { ip = targetIp })
                            }
                        });
                        content = new StringContent(str, Encoding.UTF8, "application/json");
                        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Basic", "YWRtaW46YWRtaW4=");
                        result = await client.PostAsync($"http://{fromController.RemoteIp}:8181/restconf/operations/sina:getVersion", content);
                        if (result.IsSuccessStatusCode)
                        {
                            var resBody = await result.Content.ReadAsStringAsync();
                            var resVerODL = JsonSerializer.Deserialize<ResGetVerODL>(resBody);
                            var resVer = JsonSerializer.Deserialize<ResGetVer>(resVerODL.output.result);
                            verFromCtrl = resVer.version;
                        }
                        else
                        {
                            _logger.LogError($"onos ip: {fromController.RemoteIp} error:\n {await result.Content.ReadAsStringAsync()}");
                        }
                        break;
                    default:
                        break;
                }
            }
            catch (Exception e)
            {
                _logger.LogError("call remote ip for read data error : " + e.Message);
            }
            return verFromCtrl != -1 && verFromCtrl == verTarget;
        }

        public Task StopAsync(CancellationToken stoppingToken)
        {
            _logger.LogInformation("Timer read data is stopping.");
            _timer?.Change(Timeout.Infinite, 0);
            return Task.CompletedTask;
        }

        public void Dispose()
        {
            _timer?.Dispose();
        }
    }
}
