using System;
using System.IO;
using JetBrains.Annotations;
using YamlDotNet.Serialization;
using YamlDotNet.Serialization.NamingConventions;

namespace AirlockConsumer
{
    public static class Util
    {
        public static T ReadYamlSettings<T>(string fileName)
        {
            var filePath = PatchFilename(Path.Combine("settings", fileName));
            var input = new StringReader(File.ReadAllText(filePath));
            var deserializer = new DeserializerBuilder().WithNamingConvention(new PascalCaseNamingConvention()).Build();
            return deserializer.Deserialize<T>(input);
        }

        [NotNull]
        public static string PatchFilename([NotNull] string filename, string baseDirectoryPath = null)
        {
            return Path.IsPathRooted(filename) ? filename : WalkDirectoryTree(filename, File.Exists, baseDirectoryPath);
        }

        [NotNull]
        public static string PatchDirectoryName([NotNull] string dirName, string baseDirectoryPath = null)
        {
            return Path.IsPathRooted(dirName) ? dirName : WalkDirectoryTree(dirName, Directory.Exists, baseDirectoryPath);
        }

        [NotNull]
        private static string WalkDirectoryTree([NotNull] string filename, Func<string, bool> fileSystemObjectExists, string baseDirectoryPath = null)
        {
            if (baseDirectoryPath == null)
                baseDirectoryPath = AppDomain.CurrentDomain.GetBinDirectory();
            var baseDirectory = new DirectoryInfo(baseDirectoryPath);
            while (baseDirectory != null)
            {
                var candidateFilename = Path.Combine(baseDirectory.FullName, filename);
                if (fileSystemObjectExists(candidateFilename))
                    return candidateFilename;
                baseDirectory = baseDirectory.Parent;
            }
            return filename;
        }

        private static string GetBinDirectory([NotNull] this AppDomain domain)
        {
            return string.IsNullOrEmpty(domain.RelativeSearchPath) ? domain.BaseDirectory : domain.RelativeSearchPath;
        }
    }
}