using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using Confluent.Kafka;
using Vostok.Airlock;
using Vostok.Logging;

namespace AirlockConsumer
{
    public class AirlockConsumer<T> : IDisposable
    {
        private readonly ILog log;
        private readonly Consumer<byte[], T> consumer;
        private readonly CancellationTokenSource cancellationTokenSource;

        public AirlockConsumer(short eventType, IEnumerable<string> projects, IAirlockDeserializer<T> deserializer, IMessageProcessor<T> messageProcessor, ILog log)
        {
            this.log = log;
            var settings = Util.ReadYamlSettings<Dictionary<string, object>>("kafka.yaml");
            //var settings = kafkaSetting.Select(x => new KeyValuePair<string, object>(x.Key, x.Value));
            var consumerDeserializer = new ConsumerDeserializer<T>(deserializer);
            consumer = new Consumer<byte[], T>(settings, new ByteArrayDeserializer(), consumerDeserializer);
            consumer.OnMessage += (s, e) => messageProcessor.Process(e.Timestamp.UtcDateTime, e.Value);
            consumer.OnError += (s, e) =>
            {
                log.Error(e.Reason);
            };
            consumer.OnConsumeError += (s, e) =>
            {
                log.Error(e.Error.ToString());
            };
            consumer.OnPartitionsAssigned += (s, e) =>
            {
                log.Info($"Assigned partitions: [{string.Join(", ", e)}], member id: {consumer.MemberId}");
                consumer.Assign(e.Select(x => new TopicPartitionOffset(x, Offset.Beginning)));
            };
            consumer.OnPartitionsRevoked += (_, e) =>
            {
                log.Info($"Revoked partitions: [{string.Join(", ", e)}]");
                consumer.Unassign();
            };

            cancellationTokenSource = new CancellationTokenSource();
            var cancellationToken = cancellationTokenSource.Token;
            consumer.Subscribe(projects.Select(x => x + "-" + eventType));
            new Task(() =>
            {
                while (!cancellationToken.IsCancellationRequested)
                {
                    consumer.Poll(100);
                }
            }).Start();
        }

        public void Dispose()
        {
            cancellationTokenSource.Cancel();
            consumer?.Dispose();
        }
    }
}