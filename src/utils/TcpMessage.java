package utils;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class TcpMessage {
    public int phase;
    public int type;
    public int size;
    public String message;

    public TcpMessage(DataInputStream buffer) {
        try {
            byte[] phase = new byte[1];
            int res = buffer.read(phase, 0, 1);
            if (res == -1) System.err.println("error reading message");
            this.phase = phase[0];

            byte[] type = new byte[1];
            res = buffer.read(type, 0, 1);
            if (res == -1) System.err.println("error reading message");
            this.type = type[0];

            byte[] size = new byte[4];
            res = buffer.read(size, 0, 4);
            if (res == -1) System.err.println("error reading message");
            this.size = ByteBuffer.wrap(size).getInt();

            byte[] message = new byte[this.size];
            res = buffer.read(message, 0, this.size);
            if (res == -1) System.err.println("error reading message");
            this.message = new String(message);

        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

}






