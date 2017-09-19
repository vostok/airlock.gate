package dto;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class AirlockMessage extends BinarySerializable {
    public static final short version = 1;
    public List<EventGroup> eventGroups = new ArrayList<>();

    public void write(OutputStream stream) throws IOException {
        SerializationHelper.writeShort(stream, version);
        SerializationHelper.writeList(stream, eventGroups);
    }

    public void read(InputStream stream) throws IOException {
        short dataVersion = SerializationHelper.readShort(stream);
        if (dataVersion != version)
            throw new IOException("Invalid data version");
        eventGroups = SerializationHelper.readList(stream, EventGroup.class);
    }
}

