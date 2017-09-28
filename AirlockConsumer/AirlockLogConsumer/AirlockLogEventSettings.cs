namespace AirlockLogConsumer
{
    public class AirlockLogEventSettings
    {
        public int BatchSize { get; set; } = 1000;
        public string[] ElasticUriList { get; set; }
    }
}