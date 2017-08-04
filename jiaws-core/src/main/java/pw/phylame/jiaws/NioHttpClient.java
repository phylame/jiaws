package pw.phylame.jiaws;

import lombok.extern.slf4j.Slf4j;
import pw.phylame.jiaws.http.PassiveHttp11Request;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

@Slf4j
public class NioHttpClient {
    private SelectionKey key;
    private PassiveHttp11Request request = new PassiveHttp11Request();
    private WeakReference<ClientManager> clientManager;
    private boolean working = false;

    void init(SelectionKey key, ClientManager cm) {
        this.key = key;
        request.reset();
        working = false;
        clientManager = new WeakReference<>(cm);
    }

    void receive(ByteBuffer src) {
        if (!working) {
            // todo add to workflow
        }
        if (!request.isFinished()) {
            try {
                request.receiveData(src);
            } catch (ProtocolException e) {
                log.error("cannot parse protocol", e);
                clientManager.get().closeClient(key);
            }
        } else {
            request.debugMessage();
        }
    }

    void cleanup() {
        key = null;
    }
}
