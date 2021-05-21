using Microsoft.EntityFrameworkCore;
using System;
using System.Linq;
using ThongKe.Models;

namespace ThongKe
{
    class Program
    {
        static void StatisticLatencyRead()
        {
            var context = new KCS_DATAContext();
            var list = context.DataTrainings.Where(o => o.ClientMetric != TimeSpan.Zero).Select(o => new
            {
                o.Id,
                o.ClientMetric
            }).ToList();
            context.Database.ExecuteSqlRaw("truncate table Temp");
            int skip = 0;
            int take = 100;
            int id = 1;
            while (true)
            {
                var subList = list.Skip(skip).Take(take);
                if (subList.Count() == 0)
                {
                    break;
                }
                skip += take;
                var test = subList.Count();
                double avg = 0;
                if (subList.Count() > 0)
                {
                    avg = subList.Average(o => o.ClientMetric.TotalMilliseconds);
                }
                var temp = new Temp
                {
                    Id = id++,
                    Num = avg,
                    TimeRun = DateTime.Now
                };
                context.Temps.Add(temp);
            }
            context.SaveChanges();
        }

        static void StatisticLatencyWrite()
        {
            var context = new KCS_DATAContext();
            var list = context.DataTrainings.Where(o => o.StaleMetric != TimeSpan.Zero).Select(o => new
            {
                o.Id,
                o.StaleMetric
            }).ToList();

            context.Database.ExecuteSqlRaw("truncate table Temp");
            int skip = 0;
            int take = 100;
            int id = 1;
            while (true)
            {
                var subList = list.Skip(skip).Take(take);
                if (subList.Count() == 0)
                {
                    break;
                }
                skip += take;
                var test = subList.Count();
                double avg = 0;
                if (subList.Count() > 0)
                {
                    avg = subList.Average(o => o.StaleMetric.TotalMilliseconds);
                }
                var temp = new Temp
                {
                    Id = id++,
                    Num = avg,
                    TimeRun = DateTime.Now
                };
                context.Temps.Add(temp);
            }
            context.SaveChanges();
        }

        static void StatisticRatio()
        {
            var context = new KCS_DATAContext();
            var list = context.DataTrainings.Where(o => o.ClientMetric != TimeSpan.Zero).Select(o => new
            {
                o.Id,
                o.IsVersionSuccess
            });
            context.Database.ExecuteSqlRaw("truncate table Temp");
            int skip = 0;
            int take = 100;
            int id = 1;
            while (true)
            {
                var subList = list.Skip(skip).Take(take);
                if (subList.Count() == 0)
                {
                    break;
                }
                skip += take;
                var test = subList.Count();
                int count = 0;
                if (subList.Count() > 0)
                {
                    count = subList.Count(o => o.IsVersionSuccess);
                }
                var temp = new Temp
                {
                    Id = id++,
                    Num = count,
                    TimeRun = DateTime.Now
                };
                context.Temps.Add(temp);
            }
            context.SaveChanges();
        }

        static void StatisticStalenessVMax()
        {
            var context = new KCS_DATAContext();
            var list = context.DataTrainings.Where(o => o.ClientMetric != TimeSpan.Zero).Select(o => new
            {
                o.Id,
                o.VstalenessMax,
                o.VstalenessMin,
                o.VstalenessAvg,
                o.TstalenessAvg
            });
            context.Database.ExecuteSqlRaw("truncate table Temp");
            int skip = 0;
            int take = 100;
            int id = 1;
            while (true)
            {
                var subList = list.Where(o => o.VstalenessMax != null && o.VstalenessMax < 10).Skip(skip).Take(take);
                if (subList.Count() == 0)
                {
                    break;
                }
                skip += take;
                var test = subList.Count();
                double avg = 0;
                if (subList.Count() > 0)
                {
                    avg = subList.Average(o => o.VstalenessMax.Value);
                }
                var temp = new Temp
                {
                    Id = id++,
                    Num = avg,
                    TimeRun = DateTime.Now
                };
                context.Temps.Add(temp);
            }
            context.SaveChanges();
        }

        static void StatisticStalenessVavg()
        {
            var context = new KCS_DATAContext();
            var list = context.DataTrainings.Where(o => o.ClientMetric != TimeSpan.Zero).Select(o => new
            {
                o.Id,
                o.VstalenessMax,
                o.VstalenessMin,
                o.VstalenessAvg,
                o.TstalenessAvg
            });
            context.Database.ExecuteSqlRaw("truncate table Temp");
            int skip = 0;
            int take = 100;
            int id = 1;
            while (true)
            {
                var subList = list.Where(o => o.VstalenessAvg != null && o.VstalenessMax < 10).Skip(skip).Take(take);
                if (subList.Count() == 0)
                {
                    break;
                }
                skip += take;
                var test = subList.Count();
                double avg = 0;
                if (subList.Count() > 0)
                {
                    avg = subList.Average(o => o.VstalenessAvg.Value);
                }
                var temp = new Temp
                {
                    Id = id++,
                    Num = avg,
                    TimeRun = DateTime.Now
                };
                context.Temps.Add(temp);
            }
            context.SaveChanges();
        }

        static void StatisticStalenessT()
        {
            var context = new KCS_DATAContext();
            var list = context.DataTrainings.Where(o => o.ClientMetric != TimeSpan.Zero).Select(o => new
            {
                o.Id,
                o.VstalenessMax,
                o.VstalenessMin,
                o.VstalenessAvg,
                o.TstalenessAvg
            });
            context.Database.ExecuteSqlRaw("truncate table Temp");
            int skip = 0;
            int take = 100;
            int id = 1;
            while (true)
            {
                var subList = list.Where(o => o.TstalenessAvg != null && o.VstalenessMax < 10).Skip(skip).Take(take);
                if (subList.Count() == 0)
                {
                    break;
                }
                skip += take;
                var test = subList.Count();
                double avg = 0;
                if (subList.Count() > 0)
                {
                    avg = subList.Average(o => o.TstalenessAvg.Value);
                }
                var temp = new Temp
                {
                    Id = id++,
                    Num = avg,
                    TimeRun = DateTime.Now
                };
                context.Temps.Add(temp);
            }
            context.SaveChanges();
        }

        static void StatisticStalenessRange(double rangeSecond)
        {
            var context = new KCS_DATAContext();
            var list = context.DataTrainings.Where(o => o.ClientMetric != TimeSpan.Zero).Select(o => new
            {
                o.Id,
                o.ClientMetric,
                o.Time
            }).ToList();
            context.Database.ExecuteSqlRaw("truncate table Temp");
            int id = 1;
            var endTime = new DateTime(list.First().Time.Ticks + TimeSpan.FromSeconds(rangeSecond).Ticks);
            TimeSpan sum = new TimeSpan(0);
            TimeSpan avg = new TimeSpan(0);
            int count = 0;
            foreach (var item in list)
            {
                if (item.Time <= endTime)
                {
                    sum += item.ClientMetric;
                    count++;
                }
                else
                {
                    count = 1;
                    sum = item.ClientMetric;
                    avg = sum / count;
                    var temp = new Temp
                    {
                        Id = id++,
                        Num = avg.TotalMilliseconds,
                        TimeRun = DateTime.Now
                    };
                    endTime = new DateTime(item.Time.Ticks + TimeSpan.FromSeconds(rangeSecond).Ticks);
                    context.Temps.Add(temp);
                }
            }
            context.SaveChanges();
        }


        #region thống kê theo khoảng thời gian
        static void StatisticLatencyReadRange(double rangeSecond)
        {
            var context = new KCS_DATAContext();
            var list = context.DataTrainings.Where(o => o.ClientMetric != TimeSpan.Zero).Select(o => new
            {
                o.Id,
                o.ClientMetric,
                o.Time
            }).ToList();
            context.Database.ExecuteSqlRaw("truncate table Temp");
            int id = 1;
            var endTime = new DateTime(list.First().Time.Ticks + TimeSpan.FromSeconds(rangeSecond).Ticks);
            TimeSpan sum = new TimeSpan(0);
            TimeSpan avg = new TimeSpan(0);
            int count = 0;
            foreach (var item in list)
            {
                if (item.Time <= endTime)
                {
                    sum += item.ClientMetric;
                    count++;
                }
                else
                {
                    avg = sum / count;
                    count = 1;
                    sum = item.ClientMetric;
                    var temp = new Temp
                    {
                        Id = id++,
                        Num = avg.TotalMilliseconds,
                        TimeRun = DateTime.Now
                    };
                    endTime = new DateTime(item.Time.Ticks + TimeSpan.FromSeconds(rangeSecond).Ticks);
                    context.Temps.Add(temp);
                }
            }
            context.SaveChanges();
        }

        static void StatisticLatencyWriteRange(double rangeSecond)
        {
            var context = new KCS_DATAContext();
            var list = context.DataTrainings.Where(o => o.StaleMetric != TimeSpan.Zero).Select(o => new
            {
                o.Id,
                o.StaleMetric,
                o.Time
            }).ToList();
            context.Database.ExecuteSqlRaw("truncate table Temp");
            int id = 1;
            var endTime = new DateTime(list.First().Time.Ticks + TimeSpan.FromSeconds(rangeSecond).Ticks);
            TimeSpan sum = new TimeSpan(0);
            TimeSpan avg = new TimeSpan(0);
            int count = 0;
            foreach (var item in list)
            {
                if (item.Time <= endTime)
                {
                    sum += item.StaleMetric;
                    count++;
                }
                else
                {
                    avg = sum / count;
                    count = 1;
                    sum = item.StaleMetric;
                    var temp = new Temp
                    {
                        Id = id++,
                        Num = avg.TotalMilliseconds,
                        TimeRun = DateTime.Now
                    };
                    endTime = new DateTime(item.Time.Ticks + TimeSpan.FromSeconds(rangeSecond).Ticks);
                    context.Temps.Add(temp);
                }
            }
            context.SaveChanges();
        }

        static void StatisticStalenessVMaxRange(double rangeSecond)
        {
            var context = new KCS_DATAContext();
            var list = context.DataTrainings.Where(o => o.ClientMetric != TimeSpan.Zero && o.VstalenessMax < 10).Select(o => new
            {
                o.Id,
                o.VstalenessMax,
                o.Time
            }).ToList();
            context.Database.ExecuteSqlRaw("truncate table Temp");
            int id = 1;
            var endTime = new DateTime(list.First().Time.Ticks + TimeSpan.FromSeconds(rangeSecond).Ticks);
            double sum = 0;
            double avg = 0;
            int count = 0;
            foreach (var item in list)
            {
                if (item.Time <= endTime)
                {
                    sum += item.VstalenessMax.Value;
                    count++;
                }
                else
                {
                    avg = sum / count;
                    count = 1;
                    sum = item.VstalenessMax.Value;
                    var temp = new Temp
                    {
                        Id = id++,
                        Num = avg,
                        TimeRun = DateTime.Now
                    };
                    endTime = new DateTime(item.Time.Ticks + TimeSpan.FromSeconds(rangeSecond).Ticks);
                    context.Temps.Add(temp);
                }
            }
            context.SaveChanges();
        }

        static void StatisticStalenessVavgRange(double rangeSecond)
        {
            var context = new KCS_DATAContext();
            var list = context.DataTrainings.Where(o => o.ClientMetric != TimeSpan.Zero && o.VstalenessMax < 10).Select(o => new
            {
                o.Id,
                o.VstalenessAvg,
                o.Time
            }).ToList();
            context.Database.ExecuteSqlRaw("truncate table Temp");
            int id = 1;
            var endTime = new DateTime(list.First().Time.Ticks + TimeSpan.FromSeconds(rangeSecond).Ticks);
            double sum = 0;
            double avg = 0;
            int count = 0;
            foreach (var item in list)
            {
                if (item.Time <= endTime)
                {
                    sum += item.VstalenessAvg.Value;
                    count++;
                }
                else
                {
                    avg = sum / count;
                    count = 1;
                    sum = item.VstalenessAvg.Value;
                    var temp = new Temp
                    {
                        Id = id++,
                        Num = avg,
                        TimeRun = DateTime.Now
                    };
                    endTime = new DateTime(item.Time.Ticks + TimeSpan.FromSeconds(rangeSecond).Ticks);
                    context.Temps.Add(temp);
                }
            }
            context.SaveChanges();
        }

        static void StatisticStalenessTRange(double rangeSecond)
        {
            var context = new KCS_DATAContext();
            var list = context.DataTrainings.Where(o => o.ClientMetric != TimeSpan.Zero && o.VstalenessMax < 10).Select(o => new
            {
                o.Id,
                o.TstalenessAvg,
                o.Time
            }).ToList();
            context.Database.ExecuteSqlRaw("truncate table Temp");
            int id = 1;
            var endTime = new DateTime(list.First().Time.Ticks + TimeSpan.FromSeconds(rangeSecond).Ticks);
            double sum = 0;
            double avg = 0;
            int count = 0;
            foreach (var item in list)
            {
                if (item.Time <= endTime)
                {
                    sum += item.TstalenessAvg.Value;
                    count++;
                }
                else
                {
                    avg = sum / count;
                    count = 1;
                    sum = item.TstalenessAvg.Value;
                    var temp = new Temp
                    {
                        Id = id++,
                        Num = avg,
                        TimeRun = DateTime.Now
                    };
                    endTime = new DateTime(item.Time.Ticks + TimeSpan.FromSeconds(rangeSecond).Ticks);
                    context.Temps.Add(temp);
                }
            }
            context.SaveChanges();
        }
        #endregion

        static void Main(string[] args)
        {
            #region thống kê 100 request
            //StatisticLatencyRead();
            //StatisticLatencyWrite();
            //StatisticRatio();

            //StatisticStalenessVMax();
            //StatisticStalenessVavg();
            //StatisticStalenessT();
            #endregion


            #region thống kê theo khoảng thời gian
           //StatisticLatencyReadRange(120);
            //StatisticLatencyWriteRange(120);

           // StatisticStalenessVMaxRange(120);
           // StatisticStalenessVavgRange(120);
            StatisticStalenessTRange(120);
            #endregion
        }
    }
}
 