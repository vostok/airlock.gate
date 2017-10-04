import org.junit.Ignore;
import ru.kontur.airlock.dto.AirlockMessage;
import ru.kontur.airlock.dto.BinarySerializable;
import ru.kontur.airlock.dto.EventGroup;
import ru.kontur.airlock.dto.EventRecord;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.junit.Test;
import org.unitils.reflectionassert.ReflectionAssert;

public class SerializationTest {
    @Test
    public void Serialization() throws Exception {
        AirlockMessage message = getAirlockMessage();
        TestObj(message, AirlockMessage.class);
    }

    @Test
    @Ignore
    public void PrepareAmmo() throws Exception {
        InternalPrepareAmmo(10);
        InternalPrepareAmmo(100);
        InternalPrepareAmmo(1000);
    }

    private void InternalPrepareAmmo(int eventSize) throws IOException {
        AirlockMessage message = getAirlockMessage(1,100,eventSize);
        byte[] body = message.toByteArray();
        FileOutputStream fos = new FileOutputStream("ammo" + eventSize);
        byte[] headers =
            ("POST /send HTTP/1.0\r\n" +
            "Content-Length: "+ body.length +"\r\n" +
            "Host: icat-test04:8888\r\n" +
            "apikey: 8bb9d519-ae52-4c17-ad7a-d871dbd665fe\r\n" +
                    "\r\n").getBytes(StandardCharsets.US_ASCII);
        int requestSize = headers.length + body.length;
        fos.write(("" + requestSize + "\r\n").getBytes(StandardCharsets.US_ASCII));
        fos.write(headers);
        fos.write(body);
        fos.close();
    }

    public static AirlockMessage getAirlockMessage() {
        return getAirlockMessage(3,10,10);
    }

    public static AirlockMessage getAirlockMessage(int eventTypeCount, int eventRecordCount, int eventSize) {
        long ts = System.currentTimeMillis();
        AirlockMessage message = new AirlockMessage();
        for (int i = 1; i <= eventTypeCount; i++) {
            EventGroup eventGroup = new EventGroup();
            eventGroup.eventRoutingKey = "routing-key-" + Integer.toString(i);
            eventGroup.eventRecords = new ArrayList<>();
            for (int j = 0; j < eventRecordCount; j++) {
                EventRecord eventRecord = new EventRecord();
                eventRecord.timestamp = ts++;
                eventRecord.data = new byte[eventSize];
                for (int k = 0; k < eventSize; k++) {
                    eventRecord.data[k] = (byte)((i+j+k) % 256);
                }
                eventGroup.eventRecords.add(eventRecord);
            }
            message.eventGroups.add(eventGroup);
        }
        return message;
    }

    private void TestObj(BinarySerializable obj, Class classOf) throws Exception {
        byte[] bytes = obj.toByteArray();
        BinarySerializable obj2 = BinarySerializable.fromByteArray(bytes, classOf);
        ReflectionAssert.assertReflectionEquals(obj, obj2);
    }
}
