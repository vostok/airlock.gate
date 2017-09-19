package ru.kontur.airlock.dto;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class EventRecord extends BinarySerializable {
    public long timestamp;
    public byte[] data;

    @Override
    public void write(OutputStream stream) throws IOException {
        SerializationHelper.writeLong(stream, timestamp);
        SerializationHelper.writeBytes(stream, data);
    }

    @Override
    public void read(InputStream stream) throws IOException {
        timestamp = SerializationHelper.readLong(stream);
        data = SerializationHelper.readBytes(stream);
    }
}

