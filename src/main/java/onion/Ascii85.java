package onion;

import java.io.ByteArrayOutputStream;

public class Ascii85 {
    public static String encode(byte[] input) {
        StringBuilder output = new StringBuilder();
        int i = 0;
        while (i < input.length) {
            long num = 0;
            int byteCount = 0;
            while (i < input.length && byteCount < 4) {
                num = (num << 8) | input[i++];
                byteCount++;
            }
            if (byteCount == 4 && num == 0) {
                output.append('z');
            } else {
                int paddingLength = 4 - byteCount;
                while (byteCount < 4) {
                    num <<= 8;
                    byteCount++;
                }
                int digitCount = 5 - paddingLength;
                while (digitCount > 0) {
                    output.append((char) (num / 52200625 % 85 + 33));
                    num = num * 85 % 4437053125L;
                    digitCount--;
                }
            }
        }
        return output.toString();
    }

    public static byte[] decode(String input) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int i = 0;
        while (i < input.length()) {
            if (input.charAt(i) == 'z') {
                output.write(0);
                output.write(0);
                output.write(0);
                output.write(0);
                i++;
            } else {
                int num = 0;
                int digitCount = 0;
                while (i < input.length() && digitCount < 5) {
                    num = num * 85 + input.charAt(i++) - 33;
                    digitCount++;
                }
                int paddingLength = 5 - digitCount;
                while (digitCount < 5) {
                    num = num * 85 + 84;
                    digitCount++;
                }
                int byteCount = 4 - paddingLength;
                while (byteCount > 0) {
                    output.write((num >>> 24) & 0xFF);
                    num <<= 8;
                    byteCount--;
                }
            }
        }
        return output.toByteArray();
    }
}
