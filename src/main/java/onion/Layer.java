package onion;

import java.io.*;

public abstract class Layer {
    public void peel(Reader in, OutputStream out) throws IOException {
        out.write(decode(readPayload(in)));
    }

    public byte[] readPayload(Reader in) throws IOException {
        BufferedReader reader = new BufferedReader(in);
        String line = reader.readLine();
        while (!line.contains("<~")) {
            line = reader.readLine();
        }
        StringBuilder payload = new StringBuilder();
        payload.append(line, line.indexOf("<~") + 2, line.length());
        line = reader.readLine();
        while (!line.contains("~>")) {
            payload.append(line);
            line = reader.readLine();
        }
        payload.append(line, 0, line.indexOf("~>"));
        return Ascii85.decode(payload.toString().replaceAll("\\s+", ""));
    }

    public void unpeel(InputStream in, PrintStream out) throws IOException {
        writeDescription(out);
        writePayload(out, encode(in.readAllBytes()));
    }

    public void writeDescription(PrintStream out) {
    }

    public void writePayload(PrintStream out, byte[] payload) {
        out.println("==[ Payload ]===============================================");
        out.println();
        writeText(out, "<~" + Ascii85.encode(payload) + "~>");
    }

    public void writeText(PrintStream out, String text) {
        for (String line : text.split("\n")) {
            for (int start = 0; start < line.length(); start += 60) {
                out.println(line.substring(start, Math.min(start + 60, line.length())));
            }
        }
    }

    public byte[] decode(byte[] input) throws IOException {
        throw new UnsupportedOperationException();
    }

    public byte[] encode(byte[] input) throws IOException{
        throw new UnsupportedOperationException();
    }
}
