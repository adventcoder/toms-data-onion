package onion;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class Layer6 extends Layer {
    public static void main(String[] args) throws IOException {
        Layer6 layer6 = new Layer6();
        if (true) {
            InputStream in = new FileInputStream("layers/core.txt");
            PrintStream out = new PrintStream(new FileOutputStream("layers/6-prime.txt"), true);
            layer6.unpeel(in, out);
        } else {
            Reader in = new InputStreamReader(new FileInputStream("layers/6-prime.txt"), StandardCharsets.UTF_8);
            try (OutputStream out = new FileOutputStream("layers/core-prime.txt")) {
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
        //  143 ... 2911: 173 16-byte blocks of encrypted text
        code.print(2911, 2987, System.out);
        // 2987 ... 3243: 256 byte substitution table
        code.print(3243, 3253, System.out);
        // 3253 ... 3269: 16 byte key
        code.print(3269, 3278, System.out);
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
