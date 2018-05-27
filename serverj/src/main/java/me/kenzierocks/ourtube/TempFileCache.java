package me.kenzierocks.ourtube;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jsoup.UncheckedIOException;

import com.google.common.io.ByteStreams;

public class TempFileCache {

    private static final Map<String, TempFile> tempFiles = new ConcurrentHashMap<>();

    private static final class TempFile {

        public final InputStreamProvider data;
        private Path location;

        public TempFile(InputStreamProvider data) {
            this.data = data;
        }

        public synchronized Path getPath() {
            while (location == null || !Files.exists(location)) {
                saveData();
            }
            return location;
        }

        private void saveData() {
            try {
                location = Files.createTempFile("ourtube-cache", ".dat");
                try (OutputStream to = Files.newOutputStream(location)) {
                    ByteStreams.copy(data.provide(), to);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

    }

    @FunctionalInterface
    public interface InputStreamProvider {

        InputStream provide() throws IOException;
    }

    public static void cacheData(String identifier, InputStreamProvider data) {
        tempFiles.put(identifier, new TempFile(data));
    }

    public static Path getCachedData(String identifier) {
        TempFile file = tempFiles.get(identifier);
        checkNotNull(file, "No cached data for %s", identifier);
        return file.getPath();
    }

}
