import org.junit.Test;
import org.unitils.reflectionassert.ReflectionAssert;
import ru.kontur.airlock.dto.AirlockMessage;
import ru.kontur.airlock.dto.BinarySerializable;

public class SerializationTest {
    @Test
    public void Serialization() throws Exception {
        AirlockMessage message = AirlockMessageGenerator.generateAirlockMessage();

        byte[] bytes = message.toByteArray();
        BinarySerializable obj2 = BinarySerializable.fromByteArray(bytes, AirlockMessage.class);
        ReflectionAssert.assertReflectionEquals(message, obj2);
    }
}
