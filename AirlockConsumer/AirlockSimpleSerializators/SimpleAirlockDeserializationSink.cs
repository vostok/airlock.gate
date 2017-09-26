using System.IO;
using Vostok.Airlock;
using Vostok.Commons.Binary;

namespace AirlockSimpleSerializators
{
    public class SimpleAirlockDeserializationSink : IAirlockDeserializationSink
    {
        public SimpleAirlockDeserializationSink(Stream stream)
        {
            ReadStream = stream;
            Reader = new SimpleBinaryReader(stream);
        }
        public Stream ReadStream { get; }
        public IBinaryReader Reader { get; }
    }
}