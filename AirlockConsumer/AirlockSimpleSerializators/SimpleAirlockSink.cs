using System.IO;
using Vostok.Airlock;
using Vostok.Commons.Binary;

namespace AirlockSimpleSerializators
{
    public class SimpleAirlockSink : IAirlockSink
    {
        public SimpleAirlockSink(Stream stream)
        {
            WriteStream = stream;
            Writer = new SimpleBinaryWriter(stream);
        }

        public Stream WriteStream { get; }
        public IBinaryWriter Writer { get; }
    }

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