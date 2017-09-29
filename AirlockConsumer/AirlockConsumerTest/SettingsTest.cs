using System;
using System.Collections.Generic;
using System.Diagnostics;
using AirlockConsumer;
using AirlockLogConsumer;
using Xunit;
using Xunit.Abstractions;

namespace AirlockConsumerTest
{
    public class SettingsTests
    {
        private readonly ITestOutputHelper output;

        public SettingsTests(ITestOutputHelper output)
        {
            this.output = output;
        }

        [Fact]
        public void ReadKafkaSettings()
        {
            var settings = Util.ReadYamlSettings<Dictionary<string, object>>("kafka.yaml");
            Assert.True(settings.ContainsKey("fetch.wait.max.ms"));
            Assert.Equal("1000", settings["fetch.wait.max.ms"]);
        }

        [Fact]
        public void ReadLogEventSettings()
        {
            var settings = Util.ReadYamlSettings<AirlockLogEventSettings>("logConsumer.yaml");
            Assert.Equal(1000, settings.BatchSize);
        }
    }
}