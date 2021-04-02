using KcsWriteLog.Models;
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
        private readonly ILogger<TimerQLearning> _logger;
        private readonly IServiceScopeFactory _scopeFactory;
        private Timer _timer;
        private QLearningRun _qLearning;

        private int oldNumSuccess = 0;
        private int oldNumRequest = 0;
        private int newNumSuccess = 0;
        private int newNumRequest = 0;

        private double[][] oldRewards;
        private double[][] oldQTable;

        private int t;
        private Dictionary<StateAndAction, int> nPull;

        private List<LogState> _logState = new List<LogState>();

        public TimerQLearning(ILogger<TimerQLearning> logger, ILogger<QLearningRun> loggerQlearning, IServiceScopeFactory scopeFactory)
        {
            _logger = logger;
            _qLearning = new QLearningRun(loggerQlearning);
            _scopeFactory = scopeFactory;
        }

        public Task StartAsync(CancellationToken stoppingToken)
        {
            _logger.LogInformation("Timed QLearning Hosted Service running.");
            _timer = new Timer(DoWork, null, TimeSpan.Zero,
                TimeSpan.FromSeconds(10));
            return Task.CompletedTask;
        }

        private void DoWork(object state)
        {
            var scope = _scopeFactory.CreateScope();
            var _context = scope.ServiceProvider.GetRequiredService<KCS_DATAContext>();

            _logger.LogInformation("Timed QLearning is working.");
            var rwConfig = _context.Configs.OrderByDescending(o => o.Time).FirstOrDefault();
            if (rwConfig == null)
                return;

            //range
            var rangeEnd = DateTime.Now;
            var rangeStart = rangeEnd.AddSeconds(-10);
            var logRead = _context.DataTrainings.Where(o => o.Time >= rangeStart && o.Time <= rangeEnd && o.ClientMetric != TimeSpan.Zero).ToList();

            oldNumSuccess = newNumSuccess;
            oldNumRequest = newNumRequest;

            newNumSuccess += logRead.Where(o => o.IsSuccess).Count();
            newNumRequest += logRead.Count();

            var rangeStartForAction = rangeEnd.AddSeconds(-10);
            var logReadForAction = _context.DataTrainings.Where(o => o.Time >= rangeStartForAction && o.Time <= rangeEnd && o.ClientMetric != TimeSpan.Zero).ToList();
            //để tính r/R
            var numSuccessForAction = logReadForAction.Where(o => o.IsSuccess).Count();
            var numRequestForAction = logReadForAction.Count();

            var rangeStartForState = rangeEnd.AddSeconds(-10);
            var logReadForState = _context.DataTrainings.Where(o => o.Time >= rangeStartForState && o.Time <= rangeEnd
                && o.ClientMetric != TimeSpan.Zero && o.ClientMetric <= TimeSpan.FromMilliseconds(100)).ToList();
            var logWriteForState = _context.DataTrainings.Where(o => o.Time >= rangeStartForState && o.Time <= rangeEnd
                && o.StaleMetric != TimeSpan.Zero && o.StaleMetric <= TimeSpan.FromMilliseconds(100)).ToList();

            if (newNumSuccess == 0 || newNumRequest == 0 || oldNumRequest == 0 || oldNumSuccess == 0 || (newNumRequest - oldNumRequest) == 0)
            {
                return;
            }

            int l1 = 0;
            if (logWriteForState.Count() > 0)
            {
                l1 = (int)(logWriteForState.Sum(o => o.StaleMetric.TotalMilliseconds) / logWriteForState.Count()); //trung bình 1 thay đổi được cập nhật
            }

            int l2 = 0;
            if (logReadForState.Count() > 0)
            {
                l2 = (int)(logReadForState.Sum(o => o.ClientMetric.TotalMilliseconds) / logReadForState.Count()); //trung bình 1 request nhận được
            }

            //l1 = l2 + 10; //để test
            int NOE = (int)(logReadForState.Count(o => !o.IsSuccess) / (double)logReadForState.Count()); //số lần đọc lỗi

            int N = _context.ControllerIps.Where(o => o.IsActive != null && o.IsActive.Value).Count(); //số controller

            _logger.LogInformation($"new l1:{l1}, l2:{l2}, NOE:{NOE}");

            var newValue = _qLearning.Run(rwConfig.R, rwConfig.W, N, oldNumSuccess, oldNumRequest, newNumSuccess, newNumRequest,
                l1, l2, NOE, numSuccessForAction, numRequestForAction, oldRewards, oldQTable, _logState.ToArray(), t, nPull);

            oldRewards = newValue.rewards;
            oldQTable = newValue.qTable;
            t = newValue.t;
            nPull = newValue.nPull;

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

            _logger.LogInformation($"new R: {newValue.R}, W: {newValue.W}");
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
