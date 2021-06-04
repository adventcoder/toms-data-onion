package onion;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

public class Tomtel {
    private static final List<String> reg8 = Arrays.asList("a", "b", "c", "d", "e", "f", "(ptr+c)");
    private static final List<String> reg32 = Arrays.asList("la", "lb", "lc", "ld", "ptr", "pc");

    private byte[] code;
    private int size;

    public Tomtel() {
        this.code = new byte[16];
        this.size = 0;
    }

    public Tomtel(byte[] code) {
        this.code = code;
        this.size = code.length;
    }

    public int size() {
        return size;
    }

    public int get8(int i) {
        return Byte.toUnsignedInt(code[i]);
    }

    public int get32(int i) {
        return get8(i) | (get8(i + 1) << 8) | (get8(i + 2) << 16) | (get8(i + 3) << 24);
    }

    public void set8(int i, int value) {
        code[i] = (byte) (value & 0xFF);
    }

    public void set32(int i, int value) {
        set8(i, value);
        set8(i + 1, value >>> 8);
        set8(i + 2, value >>> 16);
        set8(i + 3, value >>> 24);
    }

    public void add8(int value) {
        ensureCapacity(size + 1);
        set8(size, value);
        size += 1;
    }

    public void add32(int value) {
        ensureCapacity(size + 4);
        set32(size, value);
        size += 4;
    }
    
    public void insert(int index, byte[] newCode) {
        ensureCapacity(size + newCode.length);
        System.arraycopy(code, index, code, index + newCode.length, size - index);
        System.arraycopy(newCode, 0, code, index, newCode.length);
        size += newCode.length;
    }

    private void ensureCapacity(int minLength) {
        if (code.length < minLength) {
            int newLength = code.length;
            do {
                newLength *= 2;
            } while (newLength < minLength);
            byte[] newCode = new byte[newLength];
            System.arraycopy(code, 0, newCode, 0, size);
            code = newCode;
        }
    }

    public byte[] toByteArray() {
        return Arrays.copyOfRange(code, 0, size);
    }

    public void run(OutputStream out) throws IOException {
        TomtelProcessor processor = new TomtelProcessor(code, out);
        processor.run();
    }

    public void print(int start, int end, PrintStream out) {
        int i = start;
        while (i < end) {
            int opcode = get8(i);
            switch (opcode) {
            case 0x01:
                out.printf("%04d: HALT\n", i);
                i += 1;
                break;
            case 0x02:
                out.printf("%04d: OUT a\n", i);
                i += 1;
                break;
            case 0x21:
                out.printf("%04d: JEZ %d\n", i, get32(i + 1));
                i += 5;
                break;
            case 0x22:
                out.printf("%04d: JNZ %d\n", i, get32(i + 1));
                i += 5;
                break;
            case 0xC1:
                out.printf("%04d: CMP\n", i);
                i += 1;
                break;
            case 0xC2:
                out.printf("%04d: ADD a <- b\n", i);
                i += 1;
                break;
            case 0xC3:
                out.printf("%04d: SUB a <- b\n", i);
                i += 1;
                break;
            case 0xC4:
                out.printf("%04d: XOR a <- b\n", i);
                i += 1;
                break;
            case 0xE1:
                out.printf("%04d: APTR %d\n", i, get8(i + 1));
                i += 2;
                break;
            default:
                int src = opcode & 7;
                int dest = (opcode >>> 3) & 7;
                if ((opcode & 0xC0) == 0x40) {
                    if (src == 0) {
                        out.printf("%04d: MVI %s <- %d\n", i, reg8.get(dest - 1), get8(i + 1));
                        i += 2;
                    } else {
                        out.printf("%04d: MV %s <- %s\n", i, reg8.get(dest - 1), reg8.get(src - 1));
                        i += 1;
                    }
                } else if ((opcode & 0xC0) == 0x80) {
                    if (src == 0) {
                        out.printf("%04d: MVI32 %s <- %d\n", i, reg32.get(dest - 1), get32(i + 1));
                        i += 5;
                    } else {
                        out.printf("%04d: MV32 %s <- %s\n", i, reg32.get(dest - 1), reg32.get(src - 1));
                        i += 1;
                    }
                } else {
                    throw new IllegalArgumentException("opcode: " + opcode);
                }
                break;
            }
        }
    }
}
