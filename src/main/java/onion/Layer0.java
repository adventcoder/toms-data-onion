package onion;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class Layer0 extends Layer {
    public static void main(String[] args) throws IOException {
        Layer0 layer0 = new Layer0();
        if (false) {
            InputStream in = new FileInputStream("layers/1-prime.txt");
            try (PrintStream out = new PrintStream(new FileOutputStream("layers/0-prime.txt"))) {
                layer0.unpeel(in, out);
            }
        } else {
            File file = new File("layers/0.txt");
            if (!file.exists()) {
                File layersDir = file.getParentFile();
                if (!layersDir.exists() && !layersDir.mkdir()) {
                    throw new IOException("Failed to create directory: " + layersDir);
                }
                try (PrintStream out = new PrintStream(new FileOutputStream(file))) {
                    fetch(out);
                }
            }
            Reader in = new InputStreamReader(new FileInputStream("layers/0.txt"), StandardCharsets.UTF_8);
            try (OutputStream out = new FileOutputStream("layers/1.txt")) {
                layer0.peel(in, out);
            }
        }
    }

    public static void fetch(PrintStream out) throws IOException {
        URL url = new URL("https://www.tomdalling.com/toms-data-onion/");
        try (BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
            String line = in.readLine();
            while (!line.contains("<pre>")) {
                line = in.readLine();
            }
            line = in.readLine();
            while (!line.contains("</pre>")) {
                unescapeHTMLEntities(line, out);
                out.println();
                line = in.readLine();
            }
        }
    }

    private static void unescapeHTMLEntities(String string, PrintStream out) throws IOException {
        int start = 0;
        while (start < string.length()) {
            int i = string.indexOf("&", start);
            if (i == -1) {
                out.print(string.substring(start));
                start = string.length();
            } else {
                out.print(string.substring(start, i));
                String entity = string.substring(i, string.indexOf(';', i) + 1);
                if (entity.equals("&lt;")) {
                    out.print('<');
                } else if (entity.equals("&gt;")) {
                    out.print('>');
                } else if (entity.equals("&amp;")) {
                    out.print('&');
                } else if (entity.equals("&quot;")) {
                    out.print('"');
                } else if (entity.equals("&apos;")) {
                    out.print('\'');
                } else if (entity.matches("&#([0-9]+);")) {
                    out.print((char) Integer.parseInt(entity.substring(2, entity.length() - 1)));
                } else {
                    throw new IOException("Unrecognised HTML entity: " + entity);
                }
                start = i + entity.length();
            }
        }
    }

    @Override
    public byte[] encode(byte[] input) {
        return input;
    }

    @Override
    public byte[] decode(byte[] input) {
        return input;
    }
}
