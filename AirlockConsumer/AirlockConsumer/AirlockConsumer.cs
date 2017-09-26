using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net;
using System.Threading;
using System.Threading.Tasks;
using Confluent.Kafka;
using Vostok.Airlock;
using Vostok.Logging;

namespace AirlockConsumer
{
    public class AirlockConsumer<T> : IDisposable
    {
        private readonly int batchSize;
        private readonly IMessageProcessor<T> messageProcessor;
        private readonly ILog log;
        private readonly Consumer<byte[], T> consumer;
        private readonly CancellationTokenSource cancellationTokenSource;
        private readonly List<T> events = new List<T>();

        public bool IsAssigned { get; set; }

        public AirlockConsumer(int eventType, int batchSize, IAirlockDeserializer<T> deserializer, IMessageProcessor<T> messageProcessor, ILog log, string settingsFileName = null)
        {
            this.batchSize = batchSize;
            this.messageProcessor = messageProcessor;
            this.log = log;
            var settings = Util.ReadYamlSettings<Dictionary<string, object>>(settingsFileName ?? "kafka.yaml");
            settings["client.id"] = Dns.GetHostName();
            var eventConsumers = Util.ReadYamlSettings<Dictionary<int, List<string>>>("eventConsumers.yaml");
            if (!eventConsumers.TryGetValue(eventType, out var projects))
            {
                throw new InvalidDataException("Could not find projects for eventType " + eventType);
            }
            events.Capacity = batchSize;
            //var settings = kafkaSetting.Select(x => new KeyValuePair<string, object>(x.Key, x.Value));
            var consumerDeserializer = new ConsumerDeserializer<T>(deserializer);
            consumer = new Consumer<byte[], T>(settings, new ByteArrayDeserializer(), consumerDeserializer);
            consumer.OnMessage += (s, e) => OnMessage(e.Value);
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
                IsAssigned = true;
            };
            consumer.OnPartitionsRevoked += (_, e) =>
            {
                log.Info($"Revoked partitions: [{string.Join(", ", e)}]");
                consumer.Unassign();
                IsAssigned = false;
            };
            cancellationTokenSource = new CancellationTokenSource();

            consumer.Subscribe(projects.Select(x => x + "-" + eventType));
        }

        public void Start()
        {
            var cancellationToken = cancellationTokenSource.Token;
            new Task(() =>
            {
                while (!cancellationToken.IsCancellationRequested)
                {
                    consumer.Poll(100);
                    lock (this)
                    {
                        if (events.Count > 0)
                            ProcessEvents();
                    }
                }
            }).Start();
        }

        public void Stop()
        {
            cancellationTokenSource.Cancel();
        }

        private void OnMessage(T data)
        {
            lock (this)
            {
                events.Add(data);
                if (events.Count >= batchSize)
                {
                    ProcessEvents();
                }
            }
        }

        private void ProcessEvents()
        {
            try
            {
                messageProcessor.Process(events.ToArray());
            }
            catch (Exception ex)
            {
                log.Error(ex, $"Error during message processing, {events.Count} messages lost");
            }
            finally
            {
                events.Clear();
                events.Capacity = batchSize;
            }
        }

        public void Dispose()
        {
            cancellationTokenSource.Cancel();
            consumer?.Dispose();
        }
    }
}