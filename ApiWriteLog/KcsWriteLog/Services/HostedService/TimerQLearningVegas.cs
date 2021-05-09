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
    public class TimerQLearningVegas : IHostedService, IDisposable
    {
        private readonly ILogger<TimerQLearningVegas> _loggerQlearningRun;
        private readonly IServiceScopeFactory _scopeFactory;
        private Timer _timer;
        private QLearningVegasRun _qLearning;

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

        private readonly List<LogState> _logState = new List<LogState>();

        private readonly Queue<double> _logCSC = new Queue<double>();

        public TimerQLearningVegas(ILogger<TimerQLearningVegas> logger, ILogger<QLearningVegasRun> loggerQlearningRun, ILogger<QLearningVegas> loggerQlearning, IServiceScopeFactory scopeFactory)
        {
            _loggerQlearningRun = logger;
            _qLearning = new QLearningVegasRun(loggerQlearningRun, loggerQlearning);
            _scopeFactory = scopeFactory;
        }

        public Task StartAsync(CancellationToken stoppingToken)
        {
            _loggerQlearningRun.LogInformation("Timed QLearning Hosted Service running.");
            _timer = new Timer(DoWork, null, TimeSpan.Zero,
                TimeSpan.FromSeconds(10));
            #region load qtable from db
            if (File.Exists(@"C:\Users\84389\Documents\sdn\jsonRewards.json") && File.Exists(@"C:\Users\84389\Documents\sdn\jsonQtable.json"))
            {
                string jsonRewards = File.ReadAllText(@"C:\Users\84389\Documents\sdn\jsonRewards.json");
                string jsonQtable = File.ReadAllText(@"C:\Users\84389\Documents\sdn\jsonRewards.json");
                oldRewards = JsonSerializer.Deserialize<double[][]>(jsonRewards);
                oldQTable = JsonSerializer.Deserialize<double[][]>(jsonQtable);
            }
            else
            {
                _loggerQlearningRun.LogWarning("DB not have Qtable");
            }
            #endregion

            return Task.CompletedTask;
        }

        /// <summary>
        /// Khởi tạo r w cho r cao w nhỏ để request đầu tiên có success
        /// </summary>
        /// <param name="state"></param>
        private void DoWork(object state)
        {
            var scope = _scopeFactory.CreateScope();
            var _context = scope.ServiceProvider.GetRequiredService<KCS_DATAContext>();

            _loggerQlearningRun.LogInformation("==========================================Timed QLearning is working========================================================");
            _loggerQlearningRun.LogInformation("Timed QLearning is working.");
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

            int l1 = 0;
            if (logWriteForState.Count() > 0)
            {
                var sum = logWriteForState.Sum(o => o.StaleMetric.TotalMilliseconds);
                l1 = (int)(sum / logWriteForState.Count()); //trung bình 1 thay đổi được cập nhật
            }

            int l2 = 0;
            if (logReadForState.Count() > 0)
            {
                var sum = logReadForState.Sum(o => o.ClientMetric.TotalMilliseconds);
                l2 = (int)(sum / logReadForState.Count()); //trung bình 1 request nhận được
            }

            //l1 = l2 + 10; //để test
            int NOE = (int)(logReadForState.Count(o => !o.IsVersionSuccess) / (double)logReadForState.Count()); //số lần đọc lỗi
            if (NOE < 0)
            {
                NOE = 0;
            }

            _loggerQlearningRun.LogInformation($"l1, l2, NOE = {l1}, {l2}, {NOE}");
            int N = _context.ControllerIps.Where(o => o.IsActive != null && o.IsActive.Value).Count(); //số controller

            var newValue = _qLearning.Run(rwConfig.R, rwConfig.W, N, oldNumSuccess, oldNumRequest, newNumSuccess, newNumRequest,
                l1, l2, NOE, numSuccessForAction, numRequestForAction, oldRewards, oldQTable, _logState.ToArray(), _logCSC, violateRead, violateWrite);

            oldRewards = newValue.rewards;
            oldQTable = newValue.qTable;

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
