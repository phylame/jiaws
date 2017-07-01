package pw.phylame.jiaws;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

public class NioHttpClient {
    private SelectionKey key;

    void init(SelectionKey key) {
        this.key = key;
    }

    void receive(ByteBuffer src) {

    }

    void cleanup() {
        key = null;
    }
}
