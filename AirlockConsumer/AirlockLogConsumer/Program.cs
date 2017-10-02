using System;
using AirlockConsumer;
using Serilog;
using Vostok.Logging;
using Vostok.Logging.Serilog;

namespace AirlockLogConsumer
{
    class Program
    {
        public static ILog Log;

        static void Main(string[] args)
        {
            var logger = new LoggerConfiguration()
                .WriteTo.Console()
                .WriteTo.RollingFile("..\\log\\actions-{Date}.txt")
                .MinimumLevel.Debug()
                .CreateLogger();
            Log = new SerilogLog(logger);
            var settings = Util.ReadYamlSettings<AirlockLogEventSettings>("logConsumer.yaml");
            var consumer = new AirlockLogEventConsumer(settings);
            consumer.Start();
            Console.ReadLine();
            consumer.Stop();
        }
    }
}
