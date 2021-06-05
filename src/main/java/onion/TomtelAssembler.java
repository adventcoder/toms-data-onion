package onion;

import java.io.InputStream;
import java.util.*;

public class TomtelAssembler {
    private static final Map<String, Integer> reg8 = arrayInverse("a", "b", "c", "d", "e", "f", "(ptr+c)");
    private static final Map<String, Integer> reg32 = arrayInverse("la", "lb", "lc", "ld", "ptr", "pc");

    private static Map<String, Integer> arrayInverse(String... array) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < array.length; i++) {
            map.put(array[i], i);
        }
        return map;
    }

    private final Tomtel code = new Tomtel();
    private final HashMap<String, Integer> labelIndexes = new HashMap<>();
    private final NavigableMap<Integer, String> refs = new TreeMap<>();
    
    public TomtelAssembler(InputStream source) {
        scan(source);
    }

    public void scan(InputStream source) {
        Scanner scanner = new Scanner(source, "UTF-8");
        while (scanner.hasNext()) {
            scanLabels(scanner);
            scanInstruction(scanner);
        }
    }

    private void scanLabels(Scanner scanner) {
        while (scanner.hasNext(".*:")) {
            String token = scanner.next();
            String label = token.substring(0, token.length() - 1);
            labelIndexes.put(label, code.size());
        }
    }

    private void scanInstruction(Scanner scanner) {
        String op = scanner.next().toUpperCase();
        switch (op) {
        case "HALT":
            code.add8(0x01);
            break;
        case "OUT":
            scanner.next("a");
            code.add8(0x02);
            break;
        case "JEZ":
            code.add8(0x21);
            scanImm32(scanner);
            break;
        case "JNZ":
            code.add8(0x22);
            scanImm32(scanner);
            break;
        case "CMP":
            code.add8(0xC1);
            break;
        case "ADD":
            scanner.next("a");
            scanner.next("<-");
            scanner.next("b");
            code.add8(0xC2);
            break;
        case "SUB":
            scanner.next("a");
            scanner.next("<-");
            scanner.next("b");
            code.add8(0xC3);
            break;
        case "XOR":
            scanner.next("a");
            scanner.next("<-");
            scanner.next("b");
            code.add8(0xC4);
            break;
        case "APTR":
            code.add8(0xE1);
            code.add8(scanner.nextInt());
            break;
        case "MV":
            int dest = reg8.get(scanner.next()) + 1;
            scanner.next("<-");
            int src = reg8.get(scanner.next()) + 1;
            code.add8(0x40 | (dest << 3) | src);
            break;
        case "MVI":
            dest = reg8.get(scanner.next()) + 1;
            scanner.next("<-");
            code.add8(0x40 | (dest << 3));
            code.add8(scanner.nextInt());
            break;
        case "MV32":
            dest = reg32.get(scanner.next()) + 1;
            scanner.next("<-");
            src = reg32.get(scanner.next()) + 1;
            code.add8(0x80 | (dest << 3) | src);
            break;
        case "MVI32":
            dest = reg32.get(scanner.next()) + 1;
            scanner.next("<-");
            code.add8(0x80 | (dest << 3));
            scanImm32(scanner);
            break;
        default:
            throw new IllegalArgumentException("op: " + op);
        }
    }

    private void scanImm32(Scanner scanner) {
        if (scanner.hasNext(":.*")) {
            String label = scanner.next().substring(1);
            refs.put(code.size(), label);
            code.add32(0);
        } else {
            code.add32(scanner.nextInt());
        }
    }

    public void insert(String label, byte[] newCode) {
        int labelIndex = labelIndexes.get(label);
        code.insert(labelIndex, newCode);
        for (String otherLabel : labelIndexes.keySet()) {
            int otherLabelIndex = labelIndexes.get(otherLabel);
            if (otherLabelIndex >= labelIndex && !otherLabel.equals(label)) {
                labelIndexes.put(otherLabel, otherLabelIndex + newCode.length);
            }
        }
        for (int refIndex : new ArrayList<>(refs.descendingKeySet())) {
            if (refIndex < labelIndex) break;
            refs.put(refIndex + newCode.length, refs.remove(refIndex));
        }
    }

    public Tomtel assemble() {
        Tomtel newCode = new Tomtel(code.toByteArray());
        for (int index : refs.keySet()) {
            int targetIndex = labelIndexes.get(refs.get(index));
            newCode.set32(index, targetIndex);
        }
        return newCode;
    }
}
