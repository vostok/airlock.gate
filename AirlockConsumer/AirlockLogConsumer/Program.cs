using System;
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
                .CreateLogger();
            Log = new SerilogLog(logger);
        }
    }
}
