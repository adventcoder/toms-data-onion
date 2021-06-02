package onion;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class Layer2 extends Layer {
    public static void main(String[] args) throws IOException {
        Layer2 layer2 = new Layer2();
        Reader in = new InputStreamReader(new FileInputStream("layers/2.txt"), StandardCharsets.UTF_8);
        try (OutputStream out = new FileOutputStream("layers/3.txt")) {
            layer2.peel(in, out);
        }
    }

    @Override
    public byte[] decode(byte[] input) {
        byte[] output = new byte[input.length * 7 / 8];
        int j = 0;
        int bits = 0;
        int bitCount = 0;
        for (int i = 0; i < input.length; i++) {
            int b = Byte.toUnsignedInt(input[i]);
            if (parity(b) == 0) {
                bits = (bits << 7) | (b >>> 1);
                bitCount += 7;
                if (bitCount >= 8) {
                    bitCount -= 8;
                    output[j++] = (byte) ((bits >>> bitCount) & 0xFF);
                }
            }
        }
        return output;
    }

    @Override
    public byte[] encode(byte[] input) {
        byte[] output = new byte[input.length * 8 / 7];
        int j = 0;
        int bits = 0;
        int bitCount = 0;
        for (int i = 0; i < input.length; i++) {
            int b = Byte.toUnsignedInt(input[i]);
            bits = (bits << 8) | b;
            bitCount += 8;
            while (bitCount >= 7) {
                bitCount -= 7;
                output[j++] = (byte) ((((bits >>> bitCount) & 0x7F) << 1) | parity(b));
            }
        }
        return output;
    }

    private int parity(int b) {
        b ^= (b >>> 1);
        b ^= (b >>> 2);
        b ^= (b >>> 4);
        return b & 1;
    }
}
