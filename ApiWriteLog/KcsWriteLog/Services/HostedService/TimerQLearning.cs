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
using System.IO;
using System.Linq;
using System.Text.Json;
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
            #region load qtable from db
            //if (File.Exists(@"C:\Users\84389\Documents\sdn\jsonRewards.json") && File.Exists(@"C:\Users\84389\Documents\sdn\jsonQtable.json"))
            //{
            //    string jsonRewards = File.ReadAllText(@"C:\Users\84389\Documents\sdn\jsonRewards.json");
            //    string jsonQtable = File.ReadAllText(@"C:\Users\84389\Documents\sdn\jsonQtable.json");
            //    oldRewards = JsonSerializer.Deserialize<double[][]>(jsonRewards);
            //    oldQTable = JsonSerializer.Deserialize<double[][]>(jsonQtable);
            //}
            //else
            //{
            //    _loggerQlearningRun.LogWarning("DB not have Qtable");
            //}
            #endregion
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

            #region num success / num request, l1 l2 NOE
            //range: 10s trước ->  hiện tại
            //Client metric là log read
            //Stale metric là log write
            var rangeEnd = DateTime.Now;
            var rangeStart = rangeEnd.AddSeconds(-10);
            var logRead = _context.DataTrainings.Where(o => o.Time >= rangeStart && o.Time <= rangeEnd && o.ClientMetric != TimeSpan.Zero)
                .ToList()
                .Where(o => o.ClientMetric.TotalMilliseconds < 300)
                .ToList();
            var logWrite = _context.DataTrainings.Where(o => o.Time >= rangeStart && o.Time <= rangeEnd && o.StaleMetric != TimeSpan.Zero)
                .ToList()
                .Where(o => o.StaleMetric.TotalMilliseconds < 300)
                .ToList();

            //để tính r/R
            var numSuccess = logRead.Where(o => o.IsVersionSuccess).Count();
            var numRequest = logRead.Count();

            _loggerQlearningRun.LogInformation($"request read: {numRequest}");
            //nếu chưa có request hoặc không có request mới thì không chạy
            if (numRequest == 0)
            {
                _loggerQlearningRun.LogInformation("==========================================================================================================================");
                return;
            }

            #region latency
            double thresholdRead = 20;
            double thresholdWrite = 30;

            bool violateRead = false;
            bool violateWrite = false;
            TimeSpan LatencyReadAvg = TimeSpan.FromMilliseconds(0);
            TimeSpan LatencyWriteAvg = TimeSpan.FromMilliseconds(0);
            if (logRead.Count > 0)
            {
                LatencyReadAvg = TimeSpan.FromMilliseconds(logRead.Average(o => o.ClientMetric.TotalMilliseconds));
                violateRead = LatencyReadAvg > TimeSpan.FromMilliseconds(thresholdRead);
            }

            if (logWrite.Count > 0)
            {
                LatencyWriteAvg = TimeSpan.FromMilliseconds(logWrite.Average(o => o.StaleMetric.TotalMilliseconds));
                violateWrite = LatencyWriteAvg > TimeSpan.FromMilliseconds(thresholdWrite);
            }
            #endregion

            int l1 = 0; //trung bình 1 thay đổi được cập nhật
            if (logWrite.Count() > 0)
            {
                var sum = logWrite.Sum(o => o.StaleMetric.TotalMilliseconds);
                l1 = (int)(sum / logWrite.Count()); //trung bình 1 thay đổi được cập nhật
            }

            int l2 = 0; //trung bình 1 request nhận được
            if (logRead.Count() > 0)
            {
                var sum = logRead.Sum(o => o.ClientMetric.TotalMilliseconds);
                l2 = (int)(sum / logRead.Count()); //trung bình 1 request nhận được
            }

            int NOE = logRead.Count(o => !o.IsVersionSuccess); //số lần đọc lỗi

            _loggerQlearningRun.LogInformation($"l2, avgRead, l1, avgWrite, NOE, R, W = {l1}, {LatencyReadAvg}, {l2}, {LatencyWriteAvg}, {NOE}, {rwConfig.R}, {rwConfig.W}");
            int N = _context.ControllerIps.Where(o => o.IsActive != null && o.IsActive.Value).Count(); //số controller
            #endregion

            //chạy qlearning
            var newValue = _qLearning.Run(rwConfig.R, rwConfig.W, N, l1, l2, NOE, numSuccess, numRequest,
                oldRewards, oldQTable, _logState, t, nPull, violateRead, violateWrite);
            oldRewards = newValue.rewards;
            oldQTable = newValue.qTable;
            t = newValue.t;
            nPull = newValue.nPull;

            #region log ve bieu do
            var timeRun = DateTime.Now;

            _context.LogQlearningReads.Add(new LogQlearningRead
            {
                NumViolations = logRead.Where(o => o.ClientMetric > TimeSpan.FromMilliseconds(thresholdRead)).Count(),
                TimeRun = timeRun
            });

            _context.LogQlearningWrites.Add(new LogQlearningWrite
            {
                NumViolations = logWrite.Where(o => o.StaleMetric > TimeSpan.FromMilliseconds(thresholdWrite)).Count(),
                TimeRun = timeRun
            });

            _context.LogQlearningRatios.Add(new LogQlearningRatio
            {
                Ratio = numSuccess / (double)numRequest,
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

            _context.Configs.Add(new Config
            {
                R = newValue.R,
                W = newValue.W,
                Time = DateTime.Now
            });

            _loggerQlearningRun.LogInformation($"new R: {newValue.R}, W: {newValue.W}");
            _loggerQlearningRun.LogInformation("==========================================================================================================================");
            _context.SaveChanges();

            #region save q table to text
            //var jsonRewards = JsonSerializer.Serialize(oldRewards);
            //var jsonQtable = JsonSerializer.Serialize(oldQTable);
            //File.WriteAllText(@"C:\Users\84389\Documents\sdn\jsonRewards.json", jsonRewards);
            //File.WriteAllText(@"C:\Users\84389\Documents\sdn\jsonQtable.json", jsonQtable);
            #endregion
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