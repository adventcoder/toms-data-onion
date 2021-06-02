package onion;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class Layer4 extends Layer {
    public static void main(String[] args) throws IOException {
        Layer4 layer4 = new Layer4();
        Reader in = new InputStreamReader(new FileInputStream("layers/4.txt"), StandardCharsets.UTF_8);
        try (OutputStream out = new FileOutputStream("layers/5.txt")) {
            layer4.peel(in, out);
        }
    }

    private final Random random = new Random();

    @Override
    public byte[] decode(byte[] input) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BinaryObject packet = new BinaryObject(input);
        while (packet.offset < input.length) {
            packet.length = packet.getWord(2);
            if (isValid(packet)) {
                packet.getBytes(28, packet.length - 28, out);
            }
            packet.offset += packet.length;
        }
        return out.toByteArray();
    }

    private boolean isValid(BinaryObject packet) {
        return computeIPChecksum(packet) == 0 &&
                computeUDPChecksum(packet) == 0 &&
                packet.getIPAddress(12).equals("10.1.1.10") &&
                packet.getIPAddress(16).equals("10.1.1.200") &&
                packet.getWord(22) == 42069;
    }

    @Override
    public byte[] encode(byte[] input) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int start = 0;
        while (start < input.length) {
            int i = start + 1;
            while (i < input.length && input[i - 1] != '\n') {
                i++;
            }
            BinaryObject packet = new BinaryObject(20 + 8 + (i - start));
            packet.setByte(0, (4 << 4) | 5);
            packet.setWord(2, packet.length);
            packet.setByte(6, 1 << 7);
            packet.setByte(8, 32 + random.nextInt(33));
            packet.setByte(9, 17);
            packet.setIPAddress(12, "10.1.1.10");
            packet.setIPAddress(16, "10.1.1.200");
            packet.setByte(10, computeIPChecksum(packet));
            packet.setWord(20, random.nextInt(1 << 16));
            packet.setWord(22, 42069);
            packet.setWord(24, packet.length - 20);
            packet.setBytes(28, input, start, i - start);
            packet.setWord(26, computeUDPChecksum(packet));
            out.write(packet.bytes, packet.offset, packet.length);
            start = i;
        }
        return out.toByteArray();
    }

    private int computeIPChecksum(BinaryObject packet) {
        int sum = 0;
        for (int i = 0; i < 20; i+= 2) {
            sum = addOnesComplement(sum, packet.getWord(i));
        }
        return negateOnesComplement(sum);
    }

    private int computeUDPChecksum(BinaryObject packet) {
        int sum = 0;
        sum = addOnesComplement(sum, packet.getWord(12));
        sum = addOnesComplement(sum, packet.getWord(14));
        sum = addOnesComplement(sum, packet.getWord(16));
        sum = addOnesComplement(sum, packet.getWord(18));
        sum = addOnesComplement(sum, packet.getByte(9));
        sum = addOnesComplement(sum, packet.getWord(2) - 20);
        for (int i = 20; i < packet.length; i+= 2) {
            if (i + 1 < packet.length) {
                sum = addOnesComplement(sum, packet.getWord(i));
            } else {
                sum = addOnesComplement(sum, packet.getByte(i) << 8);
            }
        }
        return negateOnesComplement(sum);
    }

    private int addOnesComplement(int a, int b) {
        int c = a + b;
        return (c & 0xFFFF) + (c >>> 16);
    }

    private int negateOnesComplement(int x) {
        return (~x) & 0xFFFF;
    }
}
