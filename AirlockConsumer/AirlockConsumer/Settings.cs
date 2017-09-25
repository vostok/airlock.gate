using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.IO;
using System.Linq;
using YamlDotNet.Serialization;
using YamlDotNet.Serialization.NamingConventions;

namespace AirlockConsumer
{
    public class Settings
    {
        private static readonly Lazy<Settings> instance = new Lazy<Settings>(CreateInstance);
        public static Settings Instance => instance.Value;

        public static Settings CreateInstance()
        {
            return Util.ReadYamlSettings<Settings>("settings.yaml");
        }

        public Dictionary<string,string> KafkaSettings { get; set; }
    }

}