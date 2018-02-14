package ru.kontur.airlock.dto;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public final class EventGroup implements BinarySerializable {

    public String eventRoutingKey;
    public List<EventRecord> eventRecords = new ArrayList<>();

    @Override
    public void write(OutputStream stream) throws IOException {
        SerializationHelper.writeString(stream, eventRoutingKey);
        SerializationHelper.writeList(stream, eventRecords);
    }

    @Override
    public void read(InputStream stream) throws IOException {
        eventRoutingKey = SerializationHelper.readString(stream);
        eventRecords = SerializationHelper.readList(stream, EventRecord.class);
    }
}
