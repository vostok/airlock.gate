using System;
using Serilog;

namespace AirlockLogConsumer
{
    class Program
    {
        static void Main(string[] args)
        {
            var log = new LoggerConfiguration()
                .WriteTo.Console()
                .WriteTo.RollingFile("..\\log\\actions-{Date}.txt")
                .CreateLogger();
        }
    }
}
