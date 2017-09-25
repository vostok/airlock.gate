using Confluent.Kafka.Serialization;

namespace AirlockConsumer
{
    public class ByteArrayDeserializer : IDeserializer<byte[]>
    {
        public byte[] Deserialize(byte[] data)
        {
            return data;
        }
    }
}