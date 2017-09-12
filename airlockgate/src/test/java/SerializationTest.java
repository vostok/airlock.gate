import dto.AirlockMessage;
import dto.BinarySerializable;
import dto.EventGroup;
import dto.EventRecord;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;
import org.unitils.reflectionassert.ReflectionAssert;
import org.unitils.reflectionassert.comparator.Comparator;

public class SerializationTest {
    @Test
    public void Serialization() throws Exception {
        EventRecord event = new EventRecord();
        event.timestamp = System.currentTimeMillis();
        event.data = new byte[10];
        for (int k = 0; k < 10; k++) {
            event.data[k] = (byte)((k) % 256);
        }
        //TestObj(event, EventRecord.class);

        AirlockMessage message = new AirlockMessage();
        for (int i = 0; i < 3; i++) {
            EventGroup eventGroup = new EventGroup();
            eventGroup.eventType = (short)(i*5);
            eventGroup.eventRecords = new ArrayList<>();
            for (int j = 0; j < 10; j++) {
                EventRecord eventRecord = new EventRecord();
                eventRecord.timestamp = System.currentTimeMillis();
                eventRecord.data = new byte[10];
                for (int k = 0; k < 10; k++) {
                    eventRecord.data[k] = (byte)((i+j+k) % 256);
                }
                TestObj(eventRecord, EventRecord.class);
                eventGroup.eventRecords.add(eventRecord);
            }
            TestObj(eventGroup, EventGroup.class);
            message.eventGroups.add(eventGroup);
        }
        TestObj(message, AirlockMessage.class);
    }

    private void TestObj(BinarySerializable obj, Class classOf) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        obj.write(outputStream);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        BinarySerializable obj2 = (BinarySerializable)classOf.newInstance();
        obj2.read(inputStream);
        ReflectionAssert.assertReflectionEquals(obj, obj2);
        Assert.assertTrue(inputStream.read() <= 0 );
    }
}
