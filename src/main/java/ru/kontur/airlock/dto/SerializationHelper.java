package ru.kontur.airlock.dto;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class SerializationHelper {

    public static final void writeInt(OutputStream stream, int value) throws IOException {
        stream.write(ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array());
        ;
    }

    public static final void writeLong(OutputStream stream, long value) throws IOException {
        stream.write(ByteBuffer.allocate(Long.BYTES).order(ByteOrder.LITTLE_ENDIAN).putLong(value).array());
    }

    public static final void writeShort(OutputStream stream, short value) throws IOException {
        stream.write(ByteBuffer.allocate(Short.BYTES).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array());
    }

    public static final void writeBytes(OutputStream stream, byte[] value) throws IOException {
        writeInt(stream, value.length);
        stream.write(value);
    }

    public static final void writeString(OutputStream stream, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeBytes(stream, bytes);
    }

    public static final <T extends BinarySerializable> void writeList(OutputStream stream, List<T> list) throws IOException {
        if (list == null || list.size() == 0) {
            writeInt(stream, 0);
            return;
        }
        writeInt(stream, list.size());
        for (T item : list) {
            item.write(stream);
        }
    }

    public static final int readInt(InputStream stream) throws IOException {
        byte[] arr = new byte[Integer.BYTES];
        readFixedBytes(stream, arr);
        return ByteBuffer.wrap(arr).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    public static final long readLong(InputStream stream) throws IOException {
        byte[] arr = new byte[Long.BYTES];
        readFixedBytes(stream, arr);
        return ByteBuffer.wrap(arr).order(ByteOrder.LITTLE_ENDIAN).getLong();
    }

    public static final short readShort(InputStream stream) throws IOException {
        byte[] arr = new byte[Short.BYTES];
        readFixedBytes(stream, arr);
        return ByteBuffer.wrap(arr).order(ByteOrder.LITTLE_ENDIAN).getShort();
    }

    public static final byte[] readBytes(InputStream stream) throws IOException {
        int size = readInt(stream);
        byte[] arr = new byte[size];
        readFixedBytes(stream, arr);
        return arr;
    }

    public static final String readString(InputStream stream) throws IOException {
        byte[] bytes = readBytes(stream);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static final void readFixedBytes(InputStream stream, byte[] arr) throws IOException {
        int size = stream.read(arr);
        if (size < arr.length)
            throw new IOException("Could not read " + arr.length + " bytes. Was " + size + " only.");
    }

    public static final <T extends BinarySerializable> List<T> readList(InputStream stream, Class<T> itemClass) throws IOException {
        int size = readInt(stream);
        ArrayList<T> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            T item;
            try {
                item = itemClass.newInstance();
            } catch (Exception e) {
                throw new IOException("Could not create instance", e);
            }
            item.read(stream);
            list.add(item);
        }
        return list;
    }
}
