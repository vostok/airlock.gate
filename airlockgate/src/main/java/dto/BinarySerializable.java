package dto;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface BinarySerializable {
    void write(OutputStream stream) throws IOException;
    void read(InputStream stream) throws IOException;
}
