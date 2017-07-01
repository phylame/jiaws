package pw.phylame.jiaws;

import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

@Slf4j
class NioClient {
    SelectionKey key;
    Thread ownerThread;
    Http11Handler handler;

    NioClient(SelectionKey key, Http11Handler handler) {
        reset(key, handler);
    }

    void reset(SelectionKey key, Http11Handler handler) {
        this.key = key;
        ownerThread = Thread.currentThread();
    }

    void receiveData(ByteBuffer buffer) {
        val invokerThread = Thread.currentThread();
        if (invokerThread != ownerThread) {
            throw new IllegalStateException(String.format("Only the owner(%s) of this instance can push data: %s", ownerThread.getName(), invokerThread.getName()));
        }
        int n = buffer.remaining();
        if (n == 0) {
            return;
        }
        log.trace("received {} bytes data", n);
    }
}
