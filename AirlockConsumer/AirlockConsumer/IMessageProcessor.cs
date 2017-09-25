using System;

namespace AirlockConsumer
{
    public interface IMessageProcessor<in T>
    {
        void Process(DateTime timestamp, T message);
    }
}