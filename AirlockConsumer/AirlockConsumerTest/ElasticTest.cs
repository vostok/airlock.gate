using System;
using System.Collections.Generic;
using Elasticsearch.Net;
using Xunit;

namespace AirlockConsumerTest
{
    public class ElasticTest
    {
        [Fact]
        public void IndexData()
        {
            var settings = new ConnectionConfiguration(new Uri("http://devops-consul1.dev.kontur.ru:9200/"))
                .RequestTimeout(TimeSpan.FromMinutes(2));

            var client = new ElasticLowLevelClient(settings);
            var obj = new Dictionary<string, string>
            {
                ["timestamp"] = DateTimeOffset.UtcNow.ToString("O"),
                ["message"] = "Hello world"
            };
            client.Index<byte[]>(".kibana", "LogEvent", new PostData<object>(obj));
        }
    }
}