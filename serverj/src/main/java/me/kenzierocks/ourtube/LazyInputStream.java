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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * {@link InputStream} that creates on first read.
 */
public class LazyInputStream extends InputStream {

    @FunctionalInterface
    public interface InputStreamConstructor {

        InputStream construct() throws IOException;

    }

    private final Lock lock = new ReentrantLock();
    private InputStreamConstructor constructor;
    private InputStream stream;

    public LazyInputStream(InputStreamConstructor constructor) {
        this.constructor = constructor;
    }

    private InputStream stream() throws IOException {
        if (stream == null) {
            lock.lock();
            try {
                if (stream == null) {
                    stream = constructor.construct();
                    constructor = null;
                }
            } finally {
                lock.unlock();
            }
        }
        return stream;
    }

    @Override
    public int read() throws IOException {
        return stream().read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return stream().read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return stream().read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return stream().skip(n);
    }

    @Override
    public int available() throws IOException {
        return stream().available();
    }

    @Override
    public void close() throws IOException {
        stream().close();
    }

    @Override
    public void mark(int readlimit) {
        try {
            stream().mark(readlimit);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void reset() throws IOException {
        stream().reset();
    }

    @Override
    public boolean markSupported() {
        try {
            return stream().markSupported();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
