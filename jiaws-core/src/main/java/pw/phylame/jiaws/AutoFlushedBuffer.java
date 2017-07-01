package pw.phylame.jiaws;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.WritableByteChannel;

public class AutoFlushedBuffer implements Closeable {
    private final ByteBuffer buffer;
    private final WritableByteChannel channel;


    public AutoFlushedBuffer(int bufferSize, WritableByteChannel channel) {
        buffer = ByteBuffer.allocate(bufferSize);
        this.channel = channel;
    }

    public void put(byte b) throws IOException {
        flushIfNeed(1);
        buffer.put(b);
    }

    public void put(ByteBuffer src) throws IOException {
        flushIfNeed(src.remaining());
        buffer.put(src);
    }

    public void put(byte[] src, int offset, int length) throws IOException {
        flushIfNeed(length);
        buffer.put(src, offset, length);
    }

    public void put(byte[] src) throws IOException {
        flushIfNeed(src.length);
        buffer.put(src);
    }

    public void flush() throws IOException {
        buffer.flip();

        while (buffer.hasRemaining()) {
            int len = channel.write(buffer);

        }

        buffer.clear();
    }

    @Override
    public void close() throws IOException {
        flush();
    }

    private void flushIfNeed(int count) throws IOException {
        if (buffer.remaining() < count) {
            flush();
        }
    }
}
