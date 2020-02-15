package slate4j;

import java.io.*;
import java.nio.file.Path;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.isRegularFile;

public enum IO {;

    public static String resourceToString(final String resource) throws IOException {
        try (final InputStream in = IO.class.getResourceAsStream(resource)) {
            final StringBuilder out = new StringBuilder();
            byte[] buffer = new byte[1024]; int read;
            while ((read = in.read(buffer)) != -1) {
                out.append(new String(buffer, 0, read, UTF_8));
            }
            return out.toString();
        }
    }

    private static final Base64.Encoder encoder = Base64.getEncoder();
    public static String encodeBase64(final byte[] data) {
        return encoder.encodeToString(data);
    }

    public static boolean existsFile(final Path file) {
        return file != null && isRegularFile(file);
    }

}
