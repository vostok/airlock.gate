import org.junit.Assert;
import org.junit.Test;
import org.unitils.reflectionassert.ReflectionAssert;
import ru.kontur.airlock.dto.AirlockMessage;
import ru.kontur.airlock.dto.BinarySerializable;

import java.util.Base64;

public class SerializationTest {
    @Test
    public void serialization() throws Exception {
        AirlockMessage message = AirlockMessageGenerator.generateAirlockMessage();

        byte[] bytes = message.toByteArray();
        BinarySerializable obj2 = BinarySerializable.fromByteArray(bytes, AirlockMessage.class);
        ReflectionAssert.assertReflectionEquals(message, obj2);
    }

    @Test
    public void csharpGeneratedBinaryDeserialization() throws Exception {
        byte[] bytes = Base64.getDecoder().decode("AQAIAAAADwAAAGlsb2t0aW9ub3YtdGVzdAAAAAAPAAAAaWxva3Rpb25vdi10ZXN0AAAAAA8AAABpbG9rdGlvbm92LXRlc3QAAAAADwAAAGlsb2t0aW9ub3YtdGVzdAAAAAAPAAAAaWxva3Rpb25vdi10ZXN0AQAAAO1AkudeAQAAKAAAACQAAABhMjkwNWI1YS05MTYzLTQ0NDktYmYyNS1hM2EyOGZjNTIwM2YPAAAAaWxva3Rpb25vdi10ZXN0AQAAAOdBkudeAQAAKAAAACQAAABlNjE1M2U1MS00ZTI1LTQwNjMtYmE1NS0yMTU4ZmZmNGFhYmUPAAAAaWxva3Rpb25vdi10ZXN0AQAAAOJCkudeAQAAKAAAACQAAAA0NTAxNGY2YS01ZmUzLTQ0YWItODM3Yy1iYzU0OTA5ZWExNWMPAAAAaWxva3Rpb25vdi10ZXN0AQAAANxDkudeAQAAKAAAACQAAABlYzQ2ODNiZS0yYWNmLTQ1YjctYjg3YS01NDRjNDg0NTJjOGM=");
        AirlockMessage csharpMessage = BinarySerializable.fromByteArray(bytes, AirlockMessage.class);
        Assert.assertEquals(8, csharpMessage.eventGroups.size());
    }
}
