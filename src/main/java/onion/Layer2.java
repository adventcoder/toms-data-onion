package onion;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class Layer2 extends Layer {
    public static void main(String[] args) throws IOException {
        Layer2 layer2 = new Layer2();
        if (false) {
            InputStream in = new FileInputStream("layers/3-prime.txt");
            try (PrintStream out = new PrintStream(new FileOutputStream("layers/2-prime.txt"))) {
                layer2.unpeel(in, out);
            }
        } else {
            Reader in = new InputStreamReader(new FileInputStream("layers/2.txt"), StandardCharsets.UTF_8);
            try (OutputStream out = new FileOutputStream("layers/3.txt")) {
                layer2.peel(in, out);
            }
        }
    }

    @Override
    public byte[] decode(byte[] input) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int bits = 0;
        int bitCount = 0;
        for (int i = 0; i < input.length; i++) {
            int b = Byte.toUnsignedInt(input[i]);
            if (parity(b) == 0) {
                bits = (bits << 7) | (b >>> 1);
                bitCount += 7;
                if (bitCount >= 8) {
                    bitCount -= 8;
                    out.write(bits >>> bitCount);
                }
            }
        }
        return out.toByteArray();
    }

    @Override
    public byte[] encode(byte[] input) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int bits = 0;
        int bitCount = 0;
        for (int i = 0; i < input.length; i++) {
            bits = (bits << 8) | Byte.toUnsignedInt(input[i]);
            bitCount += 8;
            while (bitCount >= 7) {
                bitCount -= 7;
                int b = (bits >>> bitCount) & 0x7F;
                out.write((b << 1) | parity(b));
            }
        }
        return out.toByteArray();
    }

    private int parity(int b) {
        b ^= (b >>> 1);
        b ^= (b >>> 2);
        b ^= (b >>> 4);
        return b & 1;
    }
}
