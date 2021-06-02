package onion;

import java.io.ByteArrayOutputStream;

public class BinaryObject {
    public byte[] bytes;
    public int offset;
    public int length;

    public BinaryObject(byte[] bytes, int offset, int length) {
        this.bytes = bytes;
        this.offset = offset;
        this.length = length;
    }

    public BinaryObject(byte[] bytes) {
        this(bytes, 0, bytes.length);
    }

    public BinaryObject(int length) {
        this(new byte[length]);
    }

    public int getByte(int i) {
        return Byte.toUnsignedInt(bytes[offset + i]);
    }

    public void setByte(int i, int b) {
        bytes[offset + i] = (byte) b;
    }

    public int getWord(int i) {
        return (getByte(i) << 8) | getByte(i + 1);
    }

    public void setWord(int i, int word) {
        setByte(i, word >>> 8);
        setByte(i + 1, word);
    }

    public String getIPAddress(int i) {
        String[] parts = new String[4];
        for (int j = 0; j < 4; j++) {
            parts[j] = String.valueOf(getByte(i + j));
        }
        return String.join(".", parts);
    }

    public void setIPAddress(int i, String addr) {
        String[] parts = addr.split("\\.");
        for (int j = 0; j < parts.length; j++) {
            setByte(i + j, Integer.parseInt(parts[j]));
        }
    }

    public void getBytes(int offset, int length, ByteArrayOutputStream out) {
        out.write(bytes, this.offset + offset, length);
    }

    public void setBytes(int offset, byte[] source, int sourceOffset, int sourceLength) {
        System.arraycopy(source, sourceOffset, bytes, this.offset + offset, sourceLength);
    }
}
