using KcsWriteLog.Models;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using QLearningProject.MachineLearning;
using QLearningProject.Run;
using QLearningProject.Run.Models;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;

namespace KcsWriteLog.Services.HostedService
{
    public class TimerNotQLearning : IHostedService, IDisposable
    {
        private readonly ILogger<TimerNotQLearning> _logger;
        private readonly IServiceScopeFactory _scopeFactory;
        private Timer _timer;

        /// <summary>
        /// số lần đọc đúng
        /// </summary>
        private int oldNumSuccess = 0;
        /// <summary>
        /// số lần request đọc
        /// </summary>
        private int oldNumRequest = 0;
        private int newNumSuccess = 0;
        private int newNumRequest = 0;

        public TimerNotQLearning(ILogger<TimerNotQLearning> logger, IServiceScopeFactory scopeFactory)
        {
            _logger = logger;
            _scopeFactory = scopeFactory;
        }

        public Task StartAsync(CancellationToken stoppingToken)
        {
            _logger.LogInformation("Timed Not QLearning Hosted Service running.");
            _timer = new Timer(DoWork, null, TimeSpan.Zero,
                TimeSpan.FromSeconds(10));
            return Task.CompletedTask;
        }

        private void DoWork(object state)
        {
            var scope = _scopeFactory.CreateScope();
            var _context = scope.ServiceProvider.GetRequiredService<KCS_DATAContext>();

            _logger.LogInformation("==========================================Timed Not QLearning is working========================================================");
            var rwConfig = _context.Configs.OrderByDescending(o => o.Time).FirstOrDefault();
            if (rwConfig == null)
            {
                _logger.LogInformation("==========================================================================================================================");
                return;
            }

            if (_context.DataTrainings.Count() == 0)
            {
                _logger.LogInformation("==========================================================================================================================");
                return;
            }

            //range: 10s trước ->  hiện tại
            //Client metric là log read
            //Stale metric là log write
            var rangeEnd = DateTime.Now;
            var rangeStart = rangeEnd.AddSeconds(-10);
            var logRead = _context.DataTrainings.Where(o => o.Time >= rangeStart && o.Time <= rangeEnd && o.ClientMetric != TimeSpan.Zero).ToList();

            oldNumSuccess = newNumSuccess;
            oldNumRequest = newNumRequest;

            newNumSuccess += logRead.Where(o => o.IsVersionSuccess).Count();
            newNumRequest += logRead.Count();

            var rangeStartForAction = rangeEnd.AddSeconds(-10);
            var logReadForAction = _context.DataTrainings.Where(o => o.Time >= rangeStartForAction && o.Time <= rangeEnd && o.ClientMetric != TimeSpan.Zero).ToList();
            //để tính r/R
            var numSuccessForAction = logReadForAction.Where(o => o.IsVersionSuccess).Count();
            var numRequestForAction = logReadForAction.Count();

            //request read hợp lệ
            var rangeStartForState = rangeEnd.AddSeconds(-10);
            var logReadForState = _context.DataTrainings.Where(o => o.Time >= rangeStartForState && o.Time <= rangeEnd
                && o.ClientMetric != TimeSpan.Zero && o.ClientMetric <= TimeSpan.FromMilliseconds(100)).ToList();
            var logWriteForState = _context.DataTrainings.Where(o => o.Time >= rangeStartForState && o.Time <= rangeEnd
                && o.StaleMetric != TimeSpan.Zero && o.StaleMetric <= TimeSpan.FromMilliseconds(100)).ToList();

            _logger.LogInformation($"request read: old {oldNumSuccess}/{oldNumRequest}  new {newNumSuccess}/{newNumRequest}");
            //nếu chưa có request hoặc không có request mới thì không chạy
            if (newNumRequest - oldNumRequest == 0)
            {
                _logger.LogInformation("==========================================================================================================================");
                return;
            }

            #region latency
            double thresholdRead = 8;
            double thresholdWrite = 86;

            bool violateRead = false;
            bool violateWrite = false;
            TimeSpan LatencyReadAvg = TimeSpan.FromMilliseconds(0);
            TimeSpan LatencyWriteAvg = TimeSpan.FromMilliseconds(0);
            if (logReadForState.Count > 0)
            {
                LatencyReadAvg = TimeSpan.FromMilliseconds(logReadForState.Average(o => o.ClientMetric.TotalMilliseconds));
                violateRead = LatencyReadAvg > TimeSpan.FromMilliseconds(thresholdRead);
            }

            if (logWriteForState.Count > 0)
            {
                LatencyWriteAvg = TimeSpan.FromMilliseconds(logWriteForState.Average(o => o.StaleMetric.TotalMilliseconds));
                violateWrite = LatencyWriteAvg > TimeSpan.FromMilliseconds(thresholdWrite);
            }
            #endregion

            #region log ve bieu do
            var timeRun = DateTime.Now;

            _context.LogQlearningReads.Add(new LogQlearningRead
            {
                NumViolations = logReadForState.Where(o => o.ClientMetric > TimeSpan.FromMilliseconds(thresholdRead)).Count(),
                TimeRun = timeRun
            });

            _context.LogQlearningWrites.Add(new LogQlearningWrite
            {
                NumViolations = logWriteForState.Where(o => o.StaleMetric > TimeSpan.FromMilliseconds(thresholdWrite)).Count(),
                TimeRun = timeRun
            });

            _context.LogQlearningRatios.Add(new LogQlearningRatio
            {
                Ratio = numSuccessForAction / (double)numRequestForAction,
                TimeRun = timeRun
            });

            _context.LogLatencyReads.Add(new LogLatencyRead
            {
                Latency = LatencyReadAvg.TotalMilliseconds,
                TimeRun = timeRun
            });

            _context.LogLatencyWrites.Add(new LogLatencyWrite
            {
                Latency = LatencyWriteAvg.TotalMilliseconds,
                TimeRun = timeRun
            });
            #endregion

            _logger.LogInformation("==========================================================================================================================");
            _context.SaveChanges();
        }

        public Task StopAsync(CancellationToken stoppingToken)
        {
            _logger.LogInformation("Timed QLearning Hosted Service is stopping.");
            _timer?.Change(Timeout.Infinite, 0);
            return Task.CompletedTask;
        }

        public void Dispose()
        {
            _timer?.Dispose();
        }
    }
}