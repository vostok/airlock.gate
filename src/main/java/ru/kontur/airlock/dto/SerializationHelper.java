package ru.kontur.airlock.dto;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class SerializationHelper {
    private SerializationHelper() {}

    private static void writeInt(OutputStream stream, int value) throws IOException {
        stream.write(ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array());
    }

    static void writeLong(OutputStream stream, long value) throws IOException {
        stream.write(ByteBuffer.allocate(Long.BYTES).order(ByteOrder.LITTLE_ENDIAN).putLong(value).array());
    }

    static void writeShort(OutputStream stream, short value) throws IOException {
        stream.write(ByteBuffer.allocate(Short.BYTES).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array());
    }

    static void writeBytes(OutputStream stream, byte[] value) throws IOException {
        writeInt(stream, value.length);
        stream.write(value);
    }

    static void writeString(OutputStream stream, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeBytes(stream, bytes);
    }

    static <T extends BinarySerializable> void writeList(OutputStream stream, List<T> list) throws IOException {
        if (list == null || list.size() == 0) {
            writeInt(stream, 0);
            return;
        }
        writeInt(stream, list.size());
        for (T item : list) {
            item.write(stream);
        }
    }

    private static int readInt(InputStream stream) throws IOException {
        byte[] arr = new byte[Integer.BYTES];
        readFixedBytes(stream, arr);
        return ByteBuffer.wrap(arr).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    static long readLong(InputStream stream) throws IOException {
        byte[] arr = new byte[Long.BYTES];
        readFixedBytes(stream, arr);
        return ByteBuffer.wrap(arr).order(ByteOrder.LITTLE_ENDIAN).getLong();
    }

    static short readShort(InputStream stream) throws IOException {
        byte[] arr = new byte[Short.BYTES];
        readFixedBytes(stream, arr);
        return ByteBuffer.wrap(arr).order(ByteOrder.LITTLE_ENDIAN).getShort();
    }

    static byte[] readBytes(InputStream stream) throws IOException {
        int size = readInt(stream);
        byte[] arr = new byte[size];
        readFixedBytes(stream, arr);
        return arr;
    }

    static String readString(InputStream stream) throws IOException {
        byte[] bytes = readBytes(stream);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static void readFixedBytes(InputStream stream, byte[] arr) throws IOException {
        int size = stream.read(arr);
        if (size < arr.length)
            throw new IOException("Could not read " + arr.length + " bytes. Was " + size + " only.");
    }

    static <T extends BinarySerializable> List<T> readList(InputStream stream, Class<T> clazz) throws IOException {
        int size = readInt(stream);
        ArrayList<T> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            T item = read(stream, clazz);
            list.add(item);
        }
        return list;
    }

    static <T extends BinarySerializable> T read(InputStream stream, Class<T> clazz) throws IOException {
        T item;

        try {
            item = clazz.newInstance();
        } catch (Exception e) {
            throw new IOException("Could not create instance", e);
        }
        item.read(stream);

        return item;
    }
}
