/*
 * This file is part of OurTube-serverj, licensed under the MIT License (MIT).
 *
 * Copyright (c) Kenzie Togami (kenzierocks) <https://kenzierocks.me>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package me.kenzierocks.ourtube;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
