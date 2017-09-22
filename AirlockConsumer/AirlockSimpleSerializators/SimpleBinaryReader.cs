using System;
using System.IO;
using System.Text;
using Vostok.Commons.Binary;

namespace AirlockSimpleSerializators
{
    public class SimpleBinaryReader : IBinaryReader
    {
        private readonly Stream stream;

        public SimpleBinaryReader(Stream stream)
        {
            this.stream = stream;
        }

        public int ReadInt32()
        {
            return stream.ReadInt();
        }

        public long ReadInt64()
        {
            return stream.ReadLong();
        }

        public short ReadInt16()
        {
            return stream.ReadShort();
        }

        public uint ReadUInt32()
        {
            throw new NotImplementedException();
        }

        public ulong ReadUInt64()
        {
            throw new NotImplementedException();
        }

        public ushort ReadUInt16()
        {
            throw new NotImplementedException();
        }

        public Guid ReadGuid()
        {
            throw new NotImplementedException();
        }

        public byte ReadByte()
        {
            return (byte)stream.ReadByte();
        }

        public bool ReadBool()
        {
            throw new NotImplementedException();
        }

        public float ReadFloat()
        {
            throw new NotImplementedException();
        }

        public double ReadDouble()
        {
            throw new NotImplementedException();
        }

        public string ReadString(Encoding encoding)
        {
            var bytes = ReadByteArray();
            return encoding.GetString(bytes);
        }

        public string ReadString(Encoding encoding, int length)
        {
            var bytes = ReadByteArray(length);
            return encoding.GetString(bytes);
        }

        public byte[] ReadByteArray()
        {
            var length = ReadInt32();
            return ReadByteArray(length);
        }

        public byte[] ReadByteArray(int length)
        {
            var bytes = new byte[length];
            var actualLength = stream.Read(bytes, 0, length);
            if (actualLength < length)
                throw new Exception($"Could not read {length} bytes");
            return bytes;
        }

        public long Position
        {
            get => stream.Position;
            set => stream.Position = value;
        }
    }
}