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
