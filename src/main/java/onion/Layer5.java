package onion;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

public class Layer5 extends Layer {
    public static void main(String[] args) throws IOException {
        Layer5 layer5 = new Layer5();
        if (false) {
            InputStream in = new FileInputStream("layers/6-prime.txt");
            try (Writer out = new OutputStreamWriter(new FileOutputStream("layers/5-prime.txt"), StandardCharsets.UTF_8)) {
                layer5.unpeel(in, out);
            }
        } else {
            Reader in = new InputStreamReader(new FileInputStream("layers/5.txt"), StandardCharsets.UTF_8);
            try (OutputStream out = new FileOutputStream("layers/6.txt")) {
                layer5.peel(in, out);
            }
        }
    }

    private final Random random = new Random();

    @Override
    public byte[] decode(byte[] input) {
        byte[] kek = Arrays.copyOfRange(input, 0, 32);
        byte[] kiv = Arrays.copyOfRange(input, 32, 40);
        byte[] wrappedKey = Arrays.copyOfRange(input, 40, 80);
        byte[] key = unwrapKey(kek, kiv, wrappedKey);
        byte[] iv = Arrays.copyOfRange(input, 80, 96);
        return encrypt(key, iv, input, 96);
    }

    @Override
    public byte[] encode(byte[] input) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] kek = randomBytes(32);
        out.write(kek);
        byte[] kiv = randomBytes(8);
        out.write(kiv);
        byte[] key = randomBytes(32);
        out.write(wrapKey(kek, kiv, key));
        byte[] iv = randomBytes(16);
        out.write(iv);
        out.write(encrypt(key, iv, input, 0));
        return out.toByteArray();
    }

    private byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return bytes;
    }

    private byte[] unwrapKey(byte[] kek, byte[] expectedKiv, byte[] wrappedKey) {
        Cipher cipher = createBlockCipher(kek, Cipher.DECRYPT_MODE);
        byte[] kiv = Arrays.copyOfRange(wrappedKey, 0, 8);
        byte[] key = Arrays.copyOfRange(wrappedKey, 8, 40);
        for (int n = 5; n >= 0; n--) {
            for (int i = 3; i >= 0; i--) {
                int counter = n * 4 + i + 1;
                byte[] block = new byte[16];
                System.arraycopy(kiv, 0, block, 0, 8);
                System.arraycopy(key, i * 8, block, 8, 8);
                block[7] ^= counter;
                block = cipher.update(block);
                System.arraycopy(block, 0, kiv, 0, 8);
                System.arraycopy(block, 8, key, i * 8, 8);
            }
        }
        if (!Arrays.equals(kiv, expectedKiv)) {
            throw new RuntimeException("Expected initial value does not match actual initial value");
        }
        return key;
    }

    private byte[] wrapKey(byte[] kek, byte[] kiv, byte[] key) {
        Cipher cipher = createBlockCipher(kek, Cipher.ENCRYPT_MODE);
        byte[] wrappedKey = new byte[40];
        System.arraycopy(kiv, 0, wrappedKey, 0, 8);
        System.arraycopy(key, 0, wrappedKey, 8, 32);
        for (int n = 0; n <= 5; n++) {
            for (int i = 1; i <= 4; i++) {
                int counter = n * 4 + i;
                byte[] block = new byte[16];
                System.arraycopy(wrappedKey, 0, block, 0, 8);
                System.arraycopy(wrappedKey, i * 8, block, 8, 8);
                block = cipher.update(block);
                block[7] ^= counter;
                System.arraycopy(block, 0, wrappedKey, 0, 8);
                System.arraycopy(block, 8, wrappedKey, i * 8, 8);
            }
        }
        return wrappedKey;
    }

    private byte[] encrypt(byte[] key, byte[] counter, byte[] input, int offset) {
        byte[] output = new byte[input.length - offset];
        Cipher cipher = createBlockCipher(key, Cipher.ENCRYPT_MODE);
        for (int i = offset; i < input.length; i += 16) {
            byte[] keyBlock = cipher.update(counter);
            for (int j = 0; j < 16 && i + j < input.length; j++) {
                output[i + j - offset] = (byte) (input[i + j] ^ keyBlock[j]);
            }
            increment(counter);
        }
        return output;
    }

    private void increment(byte[] counter) {
        int i = counter.length - 1;
        while (Byte.toUnsignedInt(counter[i]) == 0xFF) {
            counter[i--] = 0;
        }
        counter[i]++;
    }

    private Cipher createBlockCipher(byte[] key, int mode) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(mode, new SecretKeySpec(key, "AES"));
            return cipher;
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate cipher", e);
        }
    }
}
