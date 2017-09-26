using System;
using System.IO;
using System.Text;
using Vostok.Commons.Binary;

namespace AirlockSimpleSerializators
{
    public class SimpleBinaryWriter : IBinaryWriter
    {
        private readonly Stream stream;

        public SimpleBinaryWriter(Stream stream)
        {
            this.stream = stream;
        }

        public void Write(int value)
        {
            stream.Write(value);
        }

        public void Write(long value)
        {
            stream.Write(value);
        }

        public void Write(short value)
        {
            stream.Write(value);
        }

        public void Write(uint value)
        {
            throw new NotImplementedException();
        }

        public void Write(ulong value)
        {
            throw new NotImplementedException();
        }

        public void Write(ushort value)
        {
            throw new NotImplementedException();
        }

        public void Write(byte value)
        {
            stream.WriteByte(value);
        }

        public void Write(bool value)
        {
            throw new NotImplementedException();
        }

        public void Write(float value)
        {
            throw new NotImplementedException();
        }

        public void Write(double value)
        {
            throw new NotImplementedException();
        }

        public void Write(Guid value)
        {
            throw new NotImplementedException();
        }

        public void Write(string value, Encoding encoding)
        {
            Write(encoding.GetBytes(value));
        }

        public void WriteWithoutLengthPrefix(string value, Encoding encoding)
        {
            WriteWithoutLengthPrefix(encoding.GetBytes(value));
        }

        public void Write(byte[] value)
        {
            if (value == null)
            {
                Write(0);
                return;
            }
            Write(value.Length);
            stream.Write(value, 0, value.Length);
        }

        public void Write(byte[] value, int offset, int length)
        {
            Write(length);
            stream.Write(value, offset, length);
        }

        public void WriteWithoutLengthPrefix(byte[] value)
        {
            stream.Write(value, 0, value.Length);
        }

        public void WriteWithoutLengthPrefix(byte[] value, int offset, int length)
        {
            stream.Write(value, offset, length);
        }

        public long Position
        {
            get => stream.Position;
            set => stream.Position = value;
        }
    }
}