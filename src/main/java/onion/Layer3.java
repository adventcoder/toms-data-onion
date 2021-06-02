package onion;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class Layer3 extends Layer {
    public static void main(String[] args) throws IOException {
        Layer3 layer3 = new Layer3();
        Reader in = new InputStreamReader(new FileInputStream("layers/3.txt"), StandardCharsets.UTF_8);
        try (OutputStream out = new FileOutputStream("layers/4.txt")) {
            layer3.peel(in, out);
        }
    }

    private static final byte[] knownOutput = "==[ Payload ]===============================================\n\n<~".getBytes(StandardCharsets.UTF_8);

    private final Random random = new Random();

    @Override
    public byte[] decode(byte[] input) {
        return encrypt(input, findKey(input));
    }

    @Override
    public byte[] encode(byte[] input) {
        return encrypt(input, generateKey());
    }

    private byte[] encrypt(byte[] input, byte[] key) {
        byte[] output = new byte[input.length];
        for (int i = 0; i < input.length; i++) {
            output[i] = (byte) (input[i] ^ key[i % key.length]);
        }
        return output;
    }

    private byte[] generateKey() {
        byte[] key = new byte[32];
        random.nextBytes(key);
        return key;
    }

    private byte[] findKey(byte[] input) {
        int i = 0;
        while (!knownOutputAt(input, i)) {
            i++;
        }
        byte[] key = new byte[32];
        for (int j = 0; j < 32; j++) {
            key[(i + j) % 32] = (byte) (input[i + j] ^ knownOutput[j]);
        }
        return key;
    }

    private boolean knownOutputAt(byte[] input, int i) {
        for (int j = 0; j < 32; j++) {
            if ((input[i + j] ^ knownOutput[j]) != (input[i + j + 32] ^ knownOutput[j + 32])) {
                return false;
            }
        }
        return true;
    }
}
