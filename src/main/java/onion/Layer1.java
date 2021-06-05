package onion;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class Layer1 extends Layer {
    public static void main(String[] args) throws IOException {
        Layer1 layer1 = new Layer1();
        if (false) {
            InputStream in = new FileInputStream("layers/2-prime.txt");
            try (PrintStream out = new PrintStream(new FileOutputStream("layers/1-prime.txt"))) {
                layer1.unpeel(in, out);
            }
        } else {
            Reader in = new InputStreamReader(new FileInputStream("layers/1.txt"), StandardCharsets.UTF_8);
            try (OutputStream out = new FileOutputStream("layers/2.txt")) {
                layer1.peel(in, out);
            }
        }
    }

    @Override
    public byte[] decode(byte[] input) {
        byte[] output = new byte[input.length];
        for (int i = 0; i < input.length; i++) {
            int b = Byte.toUnsignedInt(input[i]);
            b ^= 0x55;
            b = ((b >> 1) | (b << 7)) & 0xFF;
            output[i] = (byte) b;
        }
        return output;
    }

    @Override
    public byte[] encode(byte[] input) {
        byte[] output = new byte[input.length];
        for (int i = 0; i < input.length; i++) {
            int b = Byte.toUnsignedInt(input[i]);
            b = ((b >> 7) | (b << 1)) & 0xFF;
            b ^= 0x55;
            output[i] = (byte) b;
        }
        return output;
    }
}
