package dto;

import java.io.*;

public abstract class BinarySerializable {
    public abstract void write(OutputStream stream) throws IOException;
    public abstract void read(InputStream stream) throws IOException;

    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        write(outputStream);
        return outputStream.toByteArray();
    }

    public static <T extends BinarySerializable> T fromByteArray(byte[] array, Class<T> classOf) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(array);
        T obj;
        try {
            obj = classOf.newInstance();
        } catch (Exception e) {
            throw new IOException("Could not create instance", e);
        }
        obj.read(inputStream);
        return obj;
    }
}