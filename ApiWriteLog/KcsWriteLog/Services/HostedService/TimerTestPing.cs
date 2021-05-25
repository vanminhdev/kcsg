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
            _timer = new Timer(DoWorkAsync, null, TimeSpan.Zero, TimeSpan.FromSeconds(1));
            return Task.CompletedTask;
        }

        private async void DoWorkAsync(object state)
        {
            var scope = _scopeFactory.CreateScope();
            var _context = scope.ServiceProvider.GetRequiredService<KCS_DATAContext>();

            var random = new Random();

            try
            {
                var ranTF = random.NextDouble() > 0.09; //sx thành công

                var config = _context.Configs.OrderByDescending(o => o.Time).FirstOrDefault();
                //if (config.W < 4)
                //{
                //    ranTF = false;
                //}
                //else
                //{
                //    ranTF = true;
                //}

                //fake log read
                var logRead = new DataTraining
                {
                    ClientMetric = TimeSpan.FromMilliseconds(random.NextDouble() * 10),
                    StaleMetric = TimeSpan.Zero,
                    Overhead = 0,
                    Time = DateTime.Now,
                    IsVersionSuccess = ranTF
                };

                //fake log write
                var logWrite = new DataTraining
                {
                    ClientMetric = TimeSpan.Zero,
                    StaleMetric = TimeSpan.FromMilliseconds(random.NextDouble() * 10),
                    Overhead = 0,
                    Time = DateTime.Now,
                    IsVersionSuccess = true
                };
                _context.DataTrainings.Add(logRead);
                _context.DataTrainings.Add(logWrite);

                _context.SaveChanges();
            }
            catch (Exception ex)
            {
                _logger.LogError(ex.Message);
            }
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
