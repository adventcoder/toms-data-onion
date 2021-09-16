package onion;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Layer6 extends Layer {
    public static void main(String[] args) throws IOException {
        Layer6 layer6 = new Layer6();
        if (false) {
            InputStream in = new FileInputStream("layers/core.txt");
            try (Writer out = new OutputStreamWriter(new FileOutputStream("layers/6-prime.txt"), StandardCharsets.UTF_8)) {
                layer6.unpeel(in, out);
            }
        } else {
            Reader in = new InputStreamReader(new FileInputStream("layers/6.txt"), StandardCharsets.UTF_8);
            try (OutputStream out = new FileOutputStream("layers/core.txt")) {
                layer6.peel(in, out);
            }
        }
    }

    private final Random random = new Random();

    @Override
    public byte[] decode(byte[] input) {
        disassemble(input);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TomtelProcessor processor = new TomtelProcessor(input, out);
        processor.run();
        return out.toByteArray();
    }

    private void disassemble(byte[] code) {
        Tomtel.disassemble(code, 0, 143, System.out);
        // variable number of 16-byte blocks of cipher text
        Tomtel.disassemble(code, code.length - 367, code.length - 291, System.out);
        // 256 byte substitution table
        Tomtel.disassemble(code, code.length - 35, code.length - 25, System.out);
        // 16 byte key
        Tomtel.disassemble(code, code.length - 9, code.length, System.out);
    }

    @Override
    public byte[] encode(byte[] input) throws IOException {
        byte[] key = randomKey();
        byte[] table = randomTable();
        byte[] ciphertext = encrypt(input, key, table);
        return assemble(ciphertext, table, key);
    }

    private byte[] randomKey() {
        byte[] key = new byte[16];
        random.nextBytes(key);
        return key;
    }

    private byte[] randomTable() {
        byte[] table = new byte[256];
        for (int i = 0; i < 256; i++) {
            table[i] = (byte) i;
        }
        for (int i = 0; i < 256; i++) {
            int j = i + random.nextInt(256 - i);
            byte temp = table[i];
            table[i] = table[j];
            table[j] = temp;
        }
        return table;
    }

    private byte[] encrypt(byte[] input, byte[] key, byte[] table) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TomCipher cipher = new TomCipher(key, table);
        for (int i = 0; i < input.length; i += 15) {
            byte[] block = new byte[16];
            block[0] = (byte) Math.min(input.length - i, 15);
            System.arraycopy(input, i, block, 1, block[0]);
            out.write(cipher.update(block));
        }
        out.write(cipher.update(new byte[16]));
        return out.toByteArray();
    }

    private byte[] assemble(byte[] ciphertext, byte[] table, byte[] key) {
        Map<String, byte[]> data = new HashMap<>();
        data.put("KEY", key);
        data.put("TABLE", table);
        data.put("CIPHERTEXT", ciphertext);
        return Tomtel.assemble(getClass().getResourceAsStream("Layer6.tom"), data);
    }
}
