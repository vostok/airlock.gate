using System;
using System.Collections.Generic;
using AirlockConsumer;
using Vostok.Airlock;
using Vostok.Logging;

namespace AirlockLogConsumer
{
    internal class LogEventMessageProcessor : IMessageProcessor<LogEventData>
    {
        public void Process(LogEventData[] events)
        {
        }
    }

    public class AirlockLogEventConsumer : AirlockConsumer<LogEventData>
    {
        public AirlockLogEventConsumer() : 
            base(AirlockEventTypes.Logging, 1000, new LogEventDataAirlockDeserializer(), new LogEventMessageProcessor(), 
                Program.Log.With<AirlockLogEventConsumer>())
        {
        }
    }
}