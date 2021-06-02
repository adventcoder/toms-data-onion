package onion;

import java.io.IOException;
import java.io.OutputStream;

public class TomtelProcessor {
    public final byte[] mem;
    public final OutputStream out;
    public final byte[] reg8 = new byte[6];
    public final int[] reg32 = new int[6];

    public TomtelProcessor(byte[] mem, OutputStream out) {
        this.mem = mem;
        this.out = out;
    }

    public void run() throws IOException {
        boolean halted = false;
        while (!halted) {
            int opcode = read8();
            switch (opcode) {
            case 0x01: // HALT
                halted = true;
                break;
            case 0x02: // OUT a
                out.write(Byte.toUnsignedInt(reg8[0]));
                break;
            case 0x21: // JEZ imm32
                int dest = read32();
                if (reg8[5] == 0) reg32[5] = dest;
                break;
            case 0x22: // JNZ imm32
                dest = read32();
                if (reg8[5] != 0) reg32[5] = dest;
                break;
            case 0xC1: // CMP
                reg8[5] = (byte) (reg8[0] == reg8[1] ? 0 : 1);
                break;
            case 0xC2: // ADD a <- b
                reg8[0] += reg8[1];
                break;
            case 0xC3: // SUB a <- b
                reg8[0] -= reg8[1];
                break;
            case 0xC4: // XOR a <- b
                reg8[0] ^= reg8[1];
                break;
            case 0xE1: // APTR imm8
                reg32[4] += read8();
                break;
            default:
                int src = opcode & 7;
                dest = (opcode >>> 3) & 7;
                if ((opcode >>> 6) == 1) { // MV[i] {dest} <- src
                    set8(dest, get8(src));
                } else if ((opcode >>> 6) == 2) { // MV[i]32 {dest} <- src
                    set32(dest, get32(src));
                } else {
                    throw new RuntimeException("opcode: " + opcode);
                }
                break;
            }
        }
    }

    public int read8() {
        return Byte.toUnsignedInt(mem[reg32[5]++]);
    }

    public int read32() {
        int a = read8();
        int b = read8();
        int c = read8();
        int d = read8();
        return a | (b << 8) | (c << 16) | (d << 24);
    }

    public int get8(int pos) {
        if (pos == 0) {
            return read8();
        } else if (pos >= 1 && pos <= 6) {
            return Byte.toUnsignedInt(reg8[pos - 1]);
        } else if (pos == 7) {
            int ptr = reg32[4] + Byte.toUnsignedInt(reg8[2]);
            return Byte.toUnsignedInt(mem[ptr]);
        } else {
            throw new IllegalArgumentException("pos: " + pos);
        }
    }

    public void set8(int pos, int value) {
        if (pos >= 1 && pos <= 6) {
            reg8[pos - 1] = (byte) value;
        } else if (pos == 7) {
            int ptr = reg32[4] + Byte.toUnsignedInt(reg8[2]);
            mem[ptr] = (byte) value;
        } else {
            throw new IllegalArgumentException("pos: " + pos);
        }
    }

    public int get32(int pos) {
        if (pos == 0) {
            return read32();
        } else if (pos >= 1 && pos <= 6) {
            return reg32[pos - 1];
        } else {
            throw new IllegalArgumentException("pos: " + pos);
        }
    }

    public void set32(int pos, int value) {
        if (pos >= 1 && pos <= 6) {
            reg32[pos - 1] = value;
        } else {
            throw new IllegalArgumentException("pos: " + pos);
        }
    }
}
