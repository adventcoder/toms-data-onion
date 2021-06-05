package onion;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class Layer6 extends Layer {
    public static void main(String[] args) throws IOException {
        Layer6 layer6 = new Layer6();
        if (false) {
            InputStream in = new FileInputStream("layers/core-prime.txt");
            PrintStream out = new PrintStream(new FileOutputStream("layers/6-prime.txt"), true);
            layer6.unpeel(in, out);
        } else {
            Reader in = new InputStreamReader(new FileInputStream("layers/6.txt"), StandardCharsets.UTF_8);
            try (OutputStream out = new FileOutputStream("layers/core.txt")) {
                layer6.peel(in, out);
            }
        }
    }

    private final Random random = new Random();
    private final boolean debug = true;

    @Override
    public byte[] decode(byte[] input) throws IOException {
        Tomtel code = new Tomtel(input);
        if (debug) {
            print(code);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        code.run(out);
        return out.toByteArray();
    }

    private void print(Tomtel code) {
        code.print(0, 143, System.out);
        // 16-byte blocks of cipher text
        code.print(code.size() - 367, code.size() - 291, System.out);
        // 256 byte substitution table
        code.print(code.size() - 35, code.size() - 25, System.out);
        // 16 byte key
        code.print(code.size() - 9, code.size(), System.out);
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
        TomtelAssembler assembler = new TomtelAssembler(getClass().getResourceAsStream("Layer6.tom"));
        assembler.insert("KEY", key);
        assembler.insert("TABLE", table);
        assembler.insert("CIPHERTEXT", ciphertext);
        return assembler.assemble().toByteArray();
    }
}
