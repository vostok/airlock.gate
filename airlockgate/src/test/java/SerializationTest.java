import org.junit.Ignore;
import ru.kontur.airlock.dto.AirlockMessage;
import ru.kontur.airlock.dto.BinarySerializable;
import ru.kontur.airlock.dto.EventGroup;
import ru.kontur.airlock.dto.EventRecord;

import java.io.FileOutputStream;
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
        AirlockMessage message = getAirlockMessage();
        byte[] body = message.toByteArray();
        FileOutputStream fos = new FileOutputStream("d:\\downloads\\ammo");
        //s.getBytes(StandardCharsets.UTF_8)
        byte[] headers =
            ("POST /send HTTP/1.0\r\n" +
            "Content-Length: "+ body.length +"\r\n" +
            "Host: icat-test04:8888\r\n\r\n").getBytes(StandardCharsets.US_ASCII);
        int requestSize = headers.length + body.length;
        fos.write(("" + requestSize + "\r\n").getBytes(StandardCharsets.US_ASCII));
        fos.write(headers);
        fos.write(body);
        fos.close();
    }

    public static AirlockMessage getAirlockMessage() {
//        EventRecord event = new EventRecord();
//        event.timestamp = System.currentTimeMillis();
//        event.data = new byte[10];
//        for (int k = 0; k < 10; k++) {
//            event.data[k] = (byte)((k) % 256);
//        }
        //TestObj(event, EventRecord.class);

        long ts = System.currentTimeMillis();
        AirlockMessage message = new AirlockMessage();
        for (int i = 1; i <= 3; i++) {
            EventGroup eventGroup = new EventGroup();
            eventGroup.eventType = (short)(i);
            eventGroup.eventRecords = new ArrayList<>();
            for (int j = 0; j < 10; j++) {
                EventRecord eventRecord = new EventRecord();
                eventRecord.timestamp = ts++;
                eventRecord.data = new byte[10];
                for (int k = 0; k < 10; k++) {
                    eventRecord.data[k] = (byte)((i+j+k) % 256);
                }
                //TestObj(eventRecord, EventRecord.class);
                eventGroup.eventRecords.add(eventRecord);
            }
            //TestObj(eventGroup, EventGroup.class);
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
