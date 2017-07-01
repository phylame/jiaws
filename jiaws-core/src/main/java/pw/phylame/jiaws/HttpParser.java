package pw.phylame.jiaws;

import lombok.val;

import java.io.*;
import java.nio.ByteBuffer;

public class HttpParser {
    private InputStream input;
    private OutputStream pipe;

    private Http11Handler request;

    private final Thread ownerThread;
    private boolean initialized = false;

    public HttpParser() {
        ownerThread = Thread.currentThread();
    }

    private void init() throws IOException {
        val pos = new PipedOutputStream();
        input = new PipedInputStream(pos);
        pipe = new BufferedOutputStream(pos);
        request = new Http11Handler();
        input.available();
    }

    public void pushData(ByteBuffer src) throws IOException {
        if (Thread.currentThread() != ownerThread) {
            throw new IllegalStateException("only the owner of this instance can push data");
        }
        if (!initialized) {
            init();
            initialized = true;
        }
        val count = src.remaining();
        if (count > 0) {
            pipe.write(src.array(), src.arrayOffset(), count);
            pipe.flush();
        }
    }
}
