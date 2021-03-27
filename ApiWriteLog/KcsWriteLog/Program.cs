using Microsoft.AspNetCore.Hosting;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace KcsWriteLog
{
    public class Program
    {
        public static void Main(string[] args)
        {
            CreateHostBuilder(args).Build().Run();
        }

        public static IHostBuilder CreateHostBuilder(string[] args)
        {
            var config = new ConfigurationBuilder()
                .AddJsonFile("appsettings.json", true)
                .Build();

            var useUrlsSection = config.GetSection("UseUrls");
            var useUrls = useUrlsSection
                .AsEnumerable()
                .Where(item => item.Value != null)
                .Select(item => item.Value)
                .ToArray();

            return Host.CreateDefaultBuilder(args)
                .ConfigureWebHostDefaults(webBuilder =>
                {
                    webBuilder.UseStartup<Startup>();
                    webBuilder.UseUrls(useUrls);
                });
        }
    }
}
