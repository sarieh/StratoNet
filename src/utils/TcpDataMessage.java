package utils;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class TcpDataMessage {
    public int phase;
    public int type;
    public int hashSize;
    public String hash;
    public int dataSize;
    public byte[] data;

    public TcpDataMessage(DataInputStream buffer) {
        try {
            byte[] phase = new byte[1];
            int res = buffer.read(phase, 0, 1);
            if (res == -1) System.err.println("error reading message");
            this.phase = phase[0];

            byte[] type = new byte[1];
            res = buffer.read(type, 0, 1);
            if (res == -1) System.err.println("error reading message");
            this.type = type[0];

            byte[] imgSize = new byte[4];
            res = buffer.read(imgSize, 0, 4);
            if (res == -1) System.err.println("error reading message");
            this.dataSize = ByteBuffer.wrap(imgSize).getInt();

            byte[] hashSize = new byte[4];
            res = buffer.read(hashSize, 0, 4);
            if (res == -1) System.err.println("error reading message");
            this.hashSize = ByteBuffer.wrap(hashSize).getInt();

            byte[] hash = new byte[this.hashSize];
            res = buffer.read(hash, 0, this.hashSize);
            if (res == -1) System.err.println("error reading message");
            this.hash = new String(hash);

            byte[] data = new byte[this.dataSize];
            res = buffer.read(data, 0, this.dataSize);
            if (res == -1) System.err.println("error reading message");
            this.data = data;

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean validHash(String hash) {
        return hash.equals(this.hash);
    }


}