package onion;

public class TomCipher {
    private final byte[] key;
    private final byte[] table;
    private final byte[] inverseTable;

    public TomCipher(byte[] key, byte[] table) {
        if (key.length != 16) {
            throw new IllegalArgumentException("key must be 16 bytes in length, was " + key.length);
        }
        if (table.length != 256) {
            throw new IllegalArgumentException("table must be 256 bytes in length, was " + table.length);
        }
        this.key = key.clone();
        this.table = table;
        inverseTable = new byte[256];
        for (int i = 0; i < 256; i++) {
            inverseTable[Byte.toUnsignedInt(table[i])] = (byte) i;
        }
    }

    public byte[] update(byte[] input) {
        byte[] output = input.clone();
        substitute(output, inverseTable);
        transpose(output);
        xor(output, key);
        reverse(key);
        xor(key, input);
        substitute(key, table);
        return output;
    }

    private void xor(byte[] block, byte[] key) {
        for (int i = 0; i < 16; i++) {
            block[i] ^= key[i];
        }
    }

    private void transpose(byte[] block) {
        byte a = block[0x1];
        byte b = block[0x5];
        byte c = block[0x9];
        byte d = block[0xD];
        block[0x1] = d;
        block[0x5] = a;
        block[0x9] = b;
        block[0xD] = c;
        a = block[0x2];
        b = block[0x6];
        c = block[0xA];
        d = block[0xE];
        block[0x2] = c;
        block[0x6] = d;
        block[0xA] = a;
        block[0xE] = b;
        a = block[0x3];
        b = block[0x7];
        c = block[0xB];
        d = block[0xF];
        block[0x3] = b;
        block[0x7] = c;
        block[0xB] = d;
        block[0xF] = a;
    }

    private void substitute(byte[] block, byte[] table) {
        for (int i = 0; i < 16; i++) {
            block[i] = table[Byte.toUnsignedInt(block[i])];
        }
    }

    private void reverse(byte[] block) {
        for (int i = 0; i <= 7; i++) {
            byte temp = block[i];
            block[i] = block[15 - i];
            block[15 - i] = temp;
        }
    }
}
