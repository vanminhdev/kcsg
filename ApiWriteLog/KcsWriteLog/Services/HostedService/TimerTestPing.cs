using KcsWriteLog.Models;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
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
    public class TimerTestPing : IHostedService, IDisposable
    {
        private readonly ILogger<TimerTestPing> _logger;
        private Timer _timer;
        private readonly IServiceScopeFactory _scopeFactory;
        private readonly IConfiguration _configuration;

        public TimerTestPing(ILogger<TimerTestPing> logger, IServiceScopeFactory scopeFactory, IConfiguration configuration)
        {
            _logger = logger;
            _scopeFactory = scopeFactory;
            _configuration = configuration;
        }

        public Task StartAsync(CancellationToken stoppingToken)
        {
            _logger.LogInformation("Timer test ping running.");
            _timer = new Timer(DoWorkAsync, null, TimeSpan.Zero, TimeSpan.FromSeconds(10));
            return Task.CompletedTask;
        }

        private async void DoWorkAsync(object state)
        {
            var scope = _scopeFactory.CreateScope();
            var _context = scope.ServiceProvider.GetRequiredService<KCS_DATAContext>();

            var client = new HttpClient();


            var jsonData = JsonSerializer.Serialize(new { });
            var content = new StringContent(jsonData, Encoding.UTF8, "application/json");

            var controllers = _context.ControllerIps.Where(o => o.IsActive != null && o.IsActive.Value).ToList();
            if (controllers.Count == 0)
            {
                _logger.LogWarning("controllers count = 0");
                return;
            }

            var random = new Random();
            int indexTarget = random.Next(controllers.Count);
            var targetTestPing = controllers[indexTarget];

            var verTarget = _context.VersionData.FirstOrDefault(o => o.Ip == targetTestPing.RemoteIp)?.Ver ?? -1;

            var numHost = _configuration.GetValue<int>("NumHost");
            var src = $"h{random.Next(1, numHost)}";
            var dst = $"h{random.Next(1, numHost)}";

            var resultTestPing = await HandleTestPingAsync(targetTestPing, src, dst);
        }

        class ResTestPingODL
        {
            public class Output
            {
                public string result { get; set; }
            }

            public Output output { get; set; }
        }

        private async Task<bool> HandleTestPingAsync(ControllerIp fromController, string src, string dst)
        {
            var pingIsSucess = false;
            try
            {
                switch (fromController.ControllerType)
                {
                    case "ONOS":
                        var client = new HttpClient();
                        var content = new StringContent(JsonSerializer.Serialize(new { src, dst }), Encoding.UTF8, "application/json");
                        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Basic", "a2FyYWY6a2FyYWY=");
                        var result = await client.PostAsync($"http://{fromController.RemoteIp}:8181/onos/rwdata/communicate/test-ping", content);
                        if (result.IsSuccessStatusCode)
                        {
                            var resBody = await result.Content.ReadAsStringAsync();
                            if (resBody == "True")
                            {
                                pingIsSucess = true;
                            }
                        }
                        else
                        {
                            _logger.LogError($"test ping onos ip: {fromController.RemoteIp} error:\n {await result.Content.ReadAsStringAsync()}");
                        }
                        break;
                    case "Faucet":
                        client = new HttpClient();
                        content = new StringContent(JsonSerializer.Serialize(new { src, dst }), Encoding.UTF8, "application/json");
                        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Basic", "a2FyYWY6a2FyYWY=");
                        result = await client.PostAsync($"http://{fromController.RemoteIp}:8080/faucet/sina/versions/test-ping", content);
                        if (result.IsSuccessStatusCode)
                        {
                            var resBody = await result.Content.ReadAsStringAsync();
                            if (resBody == "True")
                            {
                                pingIsSucess = true;
                            }
                        }
                        else
                        {
                            _logger.LogError($"test ping faucet ip: {fromController.RemoteIp} error:\n {await result.Content.ReadAsStringAsync()}");
                        }
                        break;
                    case "ODL":
                        client = new HttpClient();
                        var str = JsonSerializer.Serialize(new
                        {
                            input = new
                            {
                                data = JsonSerializer.Serialize(new { src, dst })
                            }
                        });
                        content = new StringContent(str, Encoding.UTF8, "application/json");
                        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Basic", "YWRtaW46YWRtaW4=");
                        result = await client.PostAsync($"http://{fromController.RemoteIp}:8181/restconf/operations/sina:testPing", content);
                        if (result.IsSuccessStatusCode)
                        {
                            var resBody = await result.Content.ReadAsStringAsync();
                            var resTestPingODL = JsonSerializer.Deserialize<ResTestPingODL>(resBody);
                            if (resTestPingODL.output.result == "True")
                            {
                                pingIsSucess = true;
                            }
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
                _logger.LogError("call remote ip for test ping error : " + e.Message);
            }
            return pingIsSucess;
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
