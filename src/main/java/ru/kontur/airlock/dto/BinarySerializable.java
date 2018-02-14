package ru.kontur.airlock.dto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface BinarySerializable {

    void write(OutputStream stream) throws IOException;

    void read(InputStream stream) throws IOException;

    default byte[] toByteArray() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        write(outputStream);
        return outputStream.toByteArray();
    }

    static <T extends BinarySerializable> T fromByteArray(byte[] array, Class<T> clazz)
            throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(array);
        T obj = SerializationHelper.read(inputStream, clazz);
        if (inputStream.read() >= 0) {
            throw new IOException("Some additional data found after reading");
        }
        return obj;
    }
}
