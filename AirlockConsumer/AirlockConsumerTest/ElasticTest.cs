using System;
using System.Collections.Generic;
using System.IO;
using Elasticsearch.Net;
using Xunit;
using Xunit.Abstractions;
using Xunit.Sdk;

namespace AirlockConsumerTest
{
    public class ElasticTest
    {
        private readonly ITestOutputHelper output;

        public ElasticTest(ITestOutputHelper output)
        {
            this.output = output;
        }

        [Fact(Skip = "Manual test")]
        public void IndexData()
        {
            var settings = new ConnectionConfiguration(new Uri("http://devops-consul1.dev.kontur.ru:9200/"))
                .RequestTimeout(TimeSpan.FromMinutes(2));

            var client = new ElasticLowLevelClient(settings);
            IDictionary<string,string> obj = new Dictionary<string, string>
            {
                ["timestamp"] = DateTimeOffset.UtcNow.ToString("O"),
                ["message"] = "Hello world"
            };
            IDictionary<string, string> obj2 = new Dictionary<string, string>
            {
                ["timestamp"] = DateTimeOffset.UtcNow.AddMinutes(5).ToString("O"),
                ["message"] = "Hello world2"
            };
            var response = client.Bulk<byte[]>(new PostData<object>(new object[]
            {
                new { index  = new {  _index = ".kibana", _type = "LogEvent"} }, obj,
                new { index = new { _index = ".kibana", _type = "LogEvent"} }, obj2
            }));
            output.WriteLine($"code = {response.HttpStatusCode}, {response.ServerError?.Error?.Reason }");
        }
    }
}