using System;
using NSubstitute;
using Serilog;
using Vostok.Logging;
using Vostok.Logging.Serilog;
using Xunit.Abstractions;

namespace AirlockConsumerTest
{
    internal class TestOutputLog : ILog
    {
        private readonly ITestOutputHelper outputHelper;

        public TestOutputLog(ITestOutputHelper outputHelper)
        {
            this.outputHelper = outputHelper;
        }

        public void Log(LogEvent logEvent)
        {
            outputHelper.WriteLine($"{DateTime.Now:T} {logEvent.Level} {string.Format(logEvent.MessageTemplate, logEvent.MessageParameters)} {logEvent.Exception}");
        }

        public bool IsEnabledFor(LogLevel level)
        {
            return true;
        }
    }

    public static class TestSetup
    {
        public static ILog GetLogMock(ITestOutputHelper outputHelper)
        {
            var logMock = Substitute.For<ILog>();
            logMock
                .When(l => l.Log(Arg.Any<LogEvent>()))
                .Do(info => new TestOutputLog(outputHelper).Log(info.Arg<LogEvent>()));
            return logMock;
        }
    }
}