using Microsoft.EntityFrameworkCore;
using System;
using System.Linq;
using ThongKe.Models;

namespace ThongKe
{
    class Program
    {
        static void StatisticStaleness()
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
            list.Take(100);
            while (true)
            {
                var subList = list.Skip(skip).Take(take);
                if (subList.Count() == 0)
                {
                    break;
                }
                skip += take;
                var test = subList.Count();
                var listAvg = subList.Where(o => o.TstalenessAvg != null).ToList();
                double avg = 0;
                if (listAvg.Count() > 0)
                {
                    avg = listAvg.Average(o => o.TstalenessAvg.Value);
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

        static void Main(string[] args)
        {
            double[][] q = new double[10][];
            q[0] = new double[10];
            var qValueMax = q[0].Max();
            var action = q[0].ToList().IndexOf(qValueMax);
        }
    }
}
 