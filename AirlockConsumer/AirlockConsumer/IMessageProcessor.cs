using System.Collections.Generic;

namespace AirlockConsumer
{
    public interface IMessageProcessor<T>
    {
        void Process(T[] message);
    }
}