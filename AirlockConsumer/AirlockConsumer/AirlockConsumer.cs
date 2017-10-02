using System;
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
    public class ConsumerEvent<T>
    {
        public T Event { get; set; }
        public long Timestamp { get; set; }
        public string Project { get; set; }
    }

    public class AirlockConsumer<T> : IDisposable
    {
        private readonly int batchSize;
        private readonly IMessageProcessor<T> messageProcessor;
        private readonly ILog log;
        private readonly Consumer<byte[], T> consumer;
        private readonly CancellationTokenSource cancellationTokenSource;
        private readonly List<ConsumerEvent<T>> events = new List<ConsumerEvent<T>>();
        private readonly Dictionary<TopicPartition, long> lastOffsets = new Dictionary<TopicPartition, long>();
        private Task polltask;

        protected AirlockConsumer(int eventType, int batchSize, IAirlockDeserializer<T> deserializer,
            IMessageProcessor<T> messageProcessor, ILog log, string settingsFileName = null)
        {
            this.batchSize = batchSize;
            this.messageProcessor = messageProcessor;
            this.log = log;
            var settings = Util.ReadYamlSettings<Dictionary<string, object>>(settingsFileName ?? "kafka.yaml");
            settings["client.id"] = Dns.GetHostName();
            settings["enable.auto.commit"] = "false";
            var eventConsumers = Util.ReadYamlSettings<Dictionary<int, List<string>>>("eventConsumers.yaml");
            if (!eventConsumers.TryGetValue(eventType, out var projects))
            {
                throw new InvalidDataException("Could not find projects for eventType " + eventType);
            }
            events.Capacity = batchSize;
            //var settings = kafkaSetting.Select(x => new KeyValuePair<string, object>(x.Key, x.Value));
            var consumerDeserializer = new ConsumerDeserializer<T>(deserializer);
            consumer = new Consumer<byte[], T>(settings, new ByteArrayDeserializer(), consumerDeserializer);
            consumer.OnMessage += (s, e) => OnMessage(e);
            consumer.OnError += (s, e) => { log.Error(e.Reason); };
            consumer.OnConsumeError += (s, e) => { log.Error(e.Error.ToString()); };
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

            consumer.Subscribe(projects.Select(x => x + "-" + eventType));
        }

        public void Start()
        {
            var cancellationToken = cancellationTokenSource.Token;
            if (polltask != null)
                throw new InvalidOperationException("already started");
            polltask = new Task(() =>
            {
                while (!cancellationToken.IsCancellationRequested)
                {
                    bool wasError = false;
                    lock (this)
                    {
                        if (events.Count > 0)
                            wasError = !ProcessEvents();
                    }
                    if (!wasError)
                        consumer.Poll(100);
                }
            });
            polltask.Start();
        }

        public void Stop()
        {
            cancellationTokenSource.Cancel();
            polltask.Wait();
            polltask = null;
            lock (this)
            {
                ProcessEvents();
            }
        }

        private void OnMessage(Message<byte[], T> message)
        {
            var topic = message.Topic;
            log.Debug($"Got message, topic: '{topic}', ts: '{message.Timestamp.UtcDateTime:O}'");
            var dashPos = topic.LastIndexOf("-", StringComparison.Ordinal);
            string project;
            if (dashPos > 0)
                project = topic.Substring(0, dashPos);
            else
            {
                log.Error("Invalid topic name: '" + topic + "'");
                return;
            }
            var topicPartition = message.TopicPartition;
            lock (this)
            {
                if (lastOffsets.TryGetValue(message.TopicPartition, out var offset))
                {
                    lastOffsets[topicPartition] = Math.Max(offset, message.Offset.Value);
                }
                else
                {
                    lastOffsets[topicPartition] = message.Offset.Value;
                }
                events.Add(new ConsumerEvent<T>
                {
                    Event = message.Value,
                    Timestamp = message.Timestamp.UnixTimestampMs,
                    Project = project
                });
                if (events.Count >= batchSize)
                {
                    ProcessEvents();
                }
            }
        }

        private bool ProcessEvents()
        {
            try
            {
                if (events.Count == 0)
                    return true;
                messageProcessor.Process(events.ToArray());
                try
                {
                    var committedOffsets = consumer.CommitAsync(lastOffsets.Select(x => new TopicPartitionOffset(x.Key, x.Value+1))).Result;
                    if (committedOffsets.Error != null && committedOffsets.Error.HasError)
                    {
                        log.Error("Commit error: " + committedOffsets.Error);
                    }
                }
                catch (Exception e)
                {
                    log.Error(e, "Commit error");
                }
                lastOffsets.Clear();
                return true;
            }
            catch (Exception ex)
            {
                log.Error(ex, $"Error during processing {events.Count} events");
                return false;
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