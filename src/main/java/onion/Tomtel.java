package onion;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.*;

public class Tomtel {
    private static final List<String> reg8 = Arrays.asList("a", "b", "c", "d", "e", "f", "(ptr+c)");
    private static final List<String> reg32 = Arrays.asList("la", "lb", "lc", "ld", "ptr", "pc");

    public static int get8(byte[] code, int i) {
        return Byte.toUnsignedInt(code[i]);
    }

    public static void set8(byte[] code, int i, int value) {
        code[i] = (byte) (value & 0xFF);
    }

    public static int get32(byte[] code, int i) {
        return get8(code, i) | (get8(code, i + 1) << 8) | (get8(code, i + 2) << 16) | (get8(code, i + 3) << 24);
    }

    public static void set32(byte[] code, int i, int value) {
        set8(code, i, value);
        set8(code, i + 1, value >>> 8);
        set8(code, i + 2, value >>> 16);
        set8(code, i + 3, value >>> 24);
    }

    public static void disassemble(byte[] code, int start, int end, PrintStream out) {
        int i = start;
        while (i < end) {
            int opcode = get8(code, i);
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
                out.printf("%04d: JEZ %d\n", i, get32(code, i + 1));
                i += 5;
                break;
            case 0x22:
                out.printf("%04d: JNZ %d\n", i, get32(code, i + 1));
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
                out.printf("%04d: APTR %d\n", i, get8(code, i + 1));
                i += 2;
                break;
            default:
                int src = opcode & 7;
                int dest = (opcode >>> 3) & 7;
                if ((opcode & 0xC0) == 0x40) {
                    if (src == 0) {
                        out.printf("%04d: MVI %s <- %d\n", i, reg8.get(dest - 1), get8(code, i + 1));
                        i += 2;
                    } else {
                        out.printf("%04d: MV %s <- %s\n", i, reg8.get(dest - 1), reg8.get(src - 1));
                        i += 1;
                    }
                } else if ((opcode & 0xC0) == 0x80) {
                    if (src == 0) {
                        out.printf("%04d: MVI32 %s <- %d\n", i, reg32.get(dest - 1), get32(code, i + 1));
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

    public static byte[] assemble(InputStream in, Map<String, byte[]> data) {
        // Split into n labels and n+1 chunks (of tokens)
        List<String> labels = new ArrayList<>();
        List<List<String>> chunks = new ArrayList<>();
        chunks.add(new ArrayList<>());
        Scanner scanner = new Scanner(in, "UTF-8");
        while (scanner.hasNext()) {
            if (scanner.hasNext(".*:")) {
                String token = scanner.next();
                labels.add(token.substring(0, token.length() - 1));
                chunks.add(new ArrayList<>());
            } else {
                chunks.get(chunks.size() - 1).add(scanner.next());
            }
        }

        // Calculate label offsets and total length
        int totalLength = calculateCodeLength(chunks.get(0));
        Map<String, Integer> labelOffsets = new HashMap<>();
        for (int i = 0; i < labels.size(); i++) {
            String label = labels.get(i);
            labelOffsets.put(label, totalLength);
            if (data.containsKey(label)) {
                assert calculateCodeLength(chunks.get(i + 1)) == 0;
                totalLength += data.get(label).length;
            } else {
                totalLength += calculateCodeLength(chunks.get(i + 1));
            }
        }

        // Assemble chunks
        byte[] code = new byte[totalLength];
        int codeLength = appendCode(chunks.get(0), labelOffsets, code, 0);
        for (int i = 0; i < labels.size(); i++) {
            String label = labels.get(i);
            if (data.containsKey(label)) {
                byte[] src = data.get(label);
                System.arraycopy(src, 0, code, codeLength, src.length);
                codeLength += src.length;
            } else {
                codeLength = appendCode(chunks.get(i + 1), labelOffsets, code, codeLength);
            }
        }

        return code;
    }

    private static int calculateCodeLength(List<String> chunk) {
        int codeLength = 0;
        Iterator<String> tokens = chunk.iterator();
        while (tokens.hasNext()) {
            String op = tokens.next().toUpperCase();
            switch (op) {
            case "HALT":
            case "CMP":
                codeLength += 1;
                break;
            case "OUT":
                tokens.next();
                codeLength += 1;
                break;
            case "APTR":
                tokens.next();
                codeLength += 2;
                break;
            case "JEZ":
            case "JNZ":
                tokens.next();
                codeLength += 5;
                break;
            case "ADD":
            case "SUB":
            case "XOR":
            case "MV":
            case "MV32":
                tokens.next();
                tokens.next();
                tokens.next();
                codeLength += 1;
                break;
            case "MVI":
                tokens.next();
                tokens.next();
                tokens.next();
                codeLength += 2;
                break;
            case "MVI32":
                tokens.next();
                tokens.next();
                tokens.next();
                codeLength += 5;
                break;
            default:
                throw new IllegalArgumentException("op: " + op);
            }
        }
        return codeLength;
    }

    private static int appendCode(List<String> chunk, Map<String, Integer> labelOffsets, byte[] code, int i) {
        Iterator<String> tokens = chunk.iterator();
        while (tokens.hasNext()) {
            String op = tokens.next().toUpperCase();
            switch (op) {
            case "HALT":
                set8(code, i, 0x01);
                i += 1;
                break;
            case "OUT":
                if (!tokens.next().equals("a")) throw new InputMismatchException();
                set8(code, i, 0x02);
                i += 1;
                break;
            case "JEZ":
                set8(code, i, 0x21);
                set32(code, i + 1, nextImm32(tokens, labelOffsets));
                i += 5;
                break;
            case "JNZ":
                set8(code, i, 0x22);
                set32(code, i + 1, nextImm32(tokens, labelOffsets));
                i += 5;
                break;
            case "CMP":
                set8(code, i, 0xC1);
                i += 1;
                break;
            case "ADD":
                if (!tokens.next().equals("a")) throw new InputMismatchException();
                if (!tokens.next().equals("<-")) throw new InputMismatchException();
                if (!tokens.next().equals("b")) throw new InputMismatchException();
                set8(code, i, 0xC2);
                i += 1;
                break;
            case "SUB":
                if (!tokens.next().equals("a")) throw new InputMismatchException();
                if (!tokens.next().equals("<-")) throw new InputMismatchException();
                if (!tokens.next().equals("b")) throw new InputMismatchException();
                set8(code, i, 0xC3);
                i += 1;
                break;
            case "XOR":
                if (!tokens.next().equals("a")) throw new InputMismatchException();
                if (!tokens.next().equals("<-")) throw new InputMismatchException();
                if (!tokens.next().equals("b")) throw new InputMismatchException();
                set8(code, i, 0xC4);
                i += 1;
                break;
            case "APTR":
                set8(code, i, 0xE1);
                set8(code, i + 1, nextImm8(tokens));
                i += 2;
                break;
            case "MV":
                int dest = nextReg8(tokens);
                if (!tokens.next().equals("<-")) throw new InputMismatchException();
                int src = nextReg8(tokens);
                set8(code, i, 0x40 | (dest << 3) | src);
                i += 1;
                break;
            case "MVI":
                dest = nextReg8(tokens) + 1;
                if (!tokens.next().equals("<-")) throw new InputMismatchException();
                set8(code, i, 0x40 | (dest << 3));
                set8(code, i + 1, nextImm8(tokens));
                i += 2;
                break;
            case "MV32":
                dest = nextReg32(tokens);
                if (!tokens.next().equals("<-")) throw new InputMismatchException();
                src = nextReg32(tokens);
                set8(code, i, 0x80 | (dest << 3) | src);
                i += 1;
                break;
            case "MVI32":
                dest = nextReg32(tokens);
                if (!tokens.next().equals("<-")) throw new InputMismatchException();
                set8(code, i, 0x80 | (dest << 3));
                set32(code, i + 1, nextImm32(tokens, labelOffsets));
                i += 5;
                break;
            default:
                throw new IllegalArgumentException("op: " + op);
            }
        }
        return i;
    }

    private static int nextImm8(Iterator<String> tokens) {
        return Integer.parseInt(tokens.next());
    }

    private static int nextImm32(Iterator<String> tokens, Map<String, Integer> labelOffsets) {
        String token = tokens.next();
        if (token.startsWith(":")) {
            return labelOffsets.get(token.substring(1));
        } else {
            return Integer.parseInt(token);
        }
    }

    private static int nextReg8(Iterator<String> tokens) {
        return reg8.indexOf(tokens.next()) + 1;
    }

    private static int nextReg32(Iterator<String> tokens) {
        return reg32.indexOf(tokens.next()) + 1;
    }
}
