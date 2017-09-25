using System;
using System.Collections.Generic;
using System.Diagnostics;
using AirlockConsumer;
using Xunit;
using Xunit.Abstractions;

namespace AirlockConsumerTest
{
    public class Settings_Tests
    {
        private readonly ITestOutputHelper output;

        public Settings_Tests(ITestOutputHelper output)
        {
            this.output = output;
        }
        [Fact]
        public void Read()
        {
            var settings = Util.ReadYamlSettings<Dictionary<string, object>>("kafka.yaml");
            foreach (var kv in settings)
            {
                output.WriteLine($"{kv.Key}: {kv.Value} {kv.Value.GetType().Name}");
            }
        }
    }
}