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
        out.println("==[ Payload ]===============================================");
        out.println();
        writePayload(out, encode(in.readAllBytes()));
        out.println();
    }

    public void writeDescription(PrintStream out) {
    }

    public void writePayload(PrintStream out, byte[] payload) {
        String payloadText = "<~" + Ascii85.encode(payload) + "~>";
        for (int i = 0; i < payloadText.length(); i += 60) {
            out.println(payloadText.substring(i, Math.min(i + 60, payloadText.length())));
        }
    }

    public byte[] decode(byte[] input) throws IOException {
        throw new UnsupportedOperationException();
    }

    public byte[] encode(byte[] input) throws IOException{
        throw new UnsupportedOperationException();
    }
}
