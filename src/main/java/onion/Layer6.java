package onion;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class Layer6 extends Layer {
    public static void main(String[] args) throws IOException {
        Layer6 layer6 = new Layer6();
        Reader in = new InputStreamReader(new FileInputStream("layers/6.txt"), StandardCharsets.UTF_8);
        try (OutputStream out = new FileOutputStream("layers/core.txt")) {
            layer6.peel(in, out);
        }
    }

    @Override
    public byte[] decode(byte[] input) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TomtelProcessor processor = new TomtelProcessor(input, out);
        processor.run();
        return out.toByteArray();
    }
}
