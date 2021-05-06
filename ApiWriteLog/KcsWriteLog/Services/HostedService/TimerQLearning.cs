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
    public class TimerQLearning : IHostedService, IDisposable
    {
        private readonly ILogger<TimerQLearning> _loggerQlearningRun;
        private readonly IServiceScopeFactory _scopeFactory;
        private Timer _timer;
        private QLearningRun _qLearning;

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

        private double[][] oldRewards;
        private double[][] oldQTable;

        private int t;
        private Dictionary<StateAndAction, int> nPull;

        private List<LogState> _logState = new List<LogState>();

        public TimerQLearning(ILogger<TimerQLearning> logger, ILogger<QLearningRun> loggerQlearningRun, ILogger<QLearning> loggerQlearning, IServiceScopeFactory scopeFactory)
        {
            _loggerQlearningRun = logger;
            _qLearning = new QLearningRun(loggerQlearningRun, loggerQlearning);
            _scopeFactory = scopeFactory;
        }

        public Task StartAsync(CancellationToken stoppingToken)
        {
            _loggerQlearningRun.LogInformation("Timed QLearning Hosted Service running.");
            _timer = new Timer(DoWork, null, TimeSpan.Zero,
                TimeSpan.FromSeconds(10));
            return Task.CompletedTask;
        }

        private void DoWork(object state)
        {
            var scope = _scopeFactory.CreateScope();
            var _context = scope.ServiceProvider.GetRequiredService<KCS_DATAContext>();

            _loggerQlearningRun.LogInformation("==========================================Timed QLearning is working========================================================");
            var rwConfig = _context.Configs.OrderByDescending(o => o.Time).FirstOrDefault();
            if (rwConfig == null)
            {
                _loggerQlearningRun.LogInformation("==========================================================================================================================");
                return;
            }

            if (_context.DataTrainings.Count() == 0)
            {
                _loggerQlearningRun.LogInformation("==========================================================================================================================");
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

            _loggerQlearningRun.LogInformation($"request read: old {oldNumSuccess}/{oldNumRequest}  new {newNumSuccess}/{newNumRequest}");
            //nếu chưa có request hoặc không có request mới thì không chạy
            if (newNumRequest - oldNumRequest == 0)
            {
                _loggerQlearningRun.LogInformation("==========================================================================================================================");
                return;
            }

            int l1 = 0; //trung bình 1 thay đổi được cập nhật
            if (logWriteForState.Count() > 0)
            {
                var sum = logWriteForState.Sum(o => o.StaleMetric.TotalMilliseconds);
                l1 = (int)(sum / logWriteForState.Count()); //trung bình 1 thay đổi được cập nhật
            }

            int l2 = 0; //trung bình 1 request nhận được
            if (logReadForState.Count() > 0)
            {
                var sum = logReadForState.Sum(o => o.ClientMetric.TotalMilliseconds);
                l2 = (int)(sum / logReadForState.Count()); //trung bình 1 request nhận được
            }

            int NOE = (int)(logReadForState.Count(o => !o.IsVersionSuccess) / (double)logReadForState.Count()); //số lần đọc lỗi
            if (NOE < 0)
            {
                NOE = 0;
            }

            _loggerQlearningRun.LogInformation($"l1, l2, NOE = {l1}, {l2}, {NOE}");
            int N = _context.ControllerIps.Where(o => o.IsActive != null && o.IsActive.Value).Count(); //số controller

            //chạy qlearning
            var newValue = _qLearning.Run(rwConfig.R, rwConfig.W, N, oldNumSuccess, oldNumRequest, newNumSuccess, newNumRequest,
                l1, l2, NOE, numSuccessForAction, numRequestForAction, oldRewards, oldQTable, _logState.ToArray(), t, nPull);

            oldRewards = newValue.rewards;
            oldQTable = newValue.qTable;
            t = newValue.t;
            nPull = newValue.nPull;


            var timeRun = DateTime.Now;

            _context.LogQlearningReads.Add(new LogQlearningRead
            {
                NumViolations = logReadForState.Where(o => o.ClientMetric > TimeSpan.FromMilliseconds(8)).Count(),
                TimeRun = timeRun
            });

            _context.LogQlearningWrites.Add(new LogQlearningWrite
            {
                NumViolations = logWriteForState.Where(o => o.StaleMetric > TimeSpan.FromMilliseconds(30)).Count(),
                TimeRun = timeRun
            });

            _context.LogQlearningRatios.Add(new LogQlearningRatio
            {
                Ratio = numSuccessForAction/(double)numRequestForAction,
                TimeRun = timeRun
            });

            if (logWriteForState.Where(o => o.StaleMetric > TimeSpan.FromMilliseconds(30)).Count() >= 5)
            {
                newValue.W -= 2;
            }

            _context.Configs.Add(new Config
            {
                R = newValue.R,
                W = newValue.W,
                Time = DateTime.Now
            });

            _logState.Add(new LogState
            {
                l1 = l1,
                l2 = l2,
                NOE = NOE,
                action = newValue.Action
            });

            _loggerQlearningRun.LogInformation($"new R: {newValue.R}, W: {newValue.W}");
            _loggerQlearningRun.LogInformation("==========================================================================================================================");
            _context.SaveChanges();
        }

        public Task StopAsync(CancellationToken stoppingToken)
        {
            _loggerQlearningRun.LogInformation("Timed QLearning Hosted Service is stopping.");
            _timer?.Change(Timeout.Infinite, 0);
            return Task.CompletedTask;
        }

        public void Dispose()
        {
            _timer?.Dispose();
        }
    }
}