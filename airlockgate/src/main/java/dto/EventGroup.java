package dto;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class EventGroup implements BinarySerializable
{
    public short eventType;
    public List<EventRecord> eventRecords;

    @Override
    public void write(OutputStream stream) throws IOException {
        SerializationHelper.writeShort(stream, eventType);
        SerializationHelper.writeList(stream, eventRecords);
    }

    @Override
    public void read(InputStream stream) throws IOException {
        eventType = SerializationHelper.readShort(stream);
        eventRecords = SerializationHelper.readList(stream, EventRecord.class);
    }
}
