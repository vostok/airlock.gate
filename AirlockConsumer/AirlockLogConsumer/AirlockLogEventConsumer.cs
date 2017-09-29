using System;
using System.Collections;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using AirlockConsumer;
using Elasticsearch.Net;
using Vostok.Airlock;
using Vostok.Logging;

namespace AirlockLogConsumer
{
    internal class LogEventMessageProcessor : IMessageProcessor<LogEventData>
    {
        private readonly ElasticLowLevelClient elasticClient;
        private readonly ILog log;

        public LogEventMessageProcessor(IEnumerable<string> elasticUriList)
        {
            log = Program.Log.With(this);
            var connectionPool = new StickyConnectionPool(elasticUriList.Select(x => new Uri(x)), null);
            var elasticConfig = new ConnectionConfiguration(connectionPool);
            elasticClient = new ElasticLowLevelClient(elasticConfig);
        }
        public void Process(IEnumerable<ConsumerEvent<LogEventData>> events)
        {
            log.Info("Process events");
            var elasticRecords = new List<object>();
            foreach (var consumerEvent in events)
            {
                elasticRecords.Add(new { index = new
                {
                    _index = ".kibana",
                    _type = "LogEvent"
                } });
                var logEventData = consumerEvent.Event;
                logEventData.Properties["@timestamp"] = DateTimeOffset.FromUnixTimeMilliseconds(consumerEvent.Timestamp).ToString("O");
                log.Debug("LogEvent: " + string.Join(", ", logEventData.Properties.Select(x => $"{x.Key} : {x.Value}")));
                elasticRecords.Add(logEventData.Properties);
            }
            log.Info($"Send {elasticRecords.Count/2} events");
            var response = elasticClient.Bulk<byte[]>(new PostData<object>(elasticRecords));
            if (response.HttpStatusCode != (int)HttpStatusCode.OK)
            {
                log.Error($"Elasic error. code= {response.HttpStatusCode}, reason: {response.ServerError?.Error?.Reason}");
            }
        }
    }

    public class AirlockLogEventConsumer : AirlockConsumer<LogEventData>
    {
        public AirlockLogEventConsumer(AirlockLogEventSettings settings) : 
            base(AirlockEventTypes.Logging, settings.BatchSize, new LogEventDataAirlockDeserializer(), new LogEventMessageProcessor(settings.ElasticUriList), 
                Program.Log.With<AirlockLogEventConsumer>())
        {
        }
    }
}