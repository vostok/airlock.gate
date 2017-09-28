using System;
using System.Collections.Generic;
using System.Diagnostics;
using AirlockConsumer;
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
        public void Read()
        {
            var settings = Util.ReadYamlSettings<Dictionary<string, object>>("kafka.yaml");
            Assert.True(settings.ContainsKey("fetch.wait.max.ms"));
            Assert.Equal("1000", settings["fetch.wait.max.ms"]);
        }
    }
}