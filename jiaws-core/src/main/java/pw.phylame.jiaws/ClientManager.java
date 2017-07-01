package pw.phylame.jiaws;

import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.LinkedList;
import java.util.Queue;

@Slf4j
class ClientManager {
    int clients = 0;
    Queue<NioClient> cache = new LinkedList<>();

    void newClient(SelectionKey key) {
        log.trace("create {}th new client", clients + 1);
        val client = cache.poll(); // get a cached client
        if (client != null) {
            log.debug("got cached client");
            client.reset(key);
            key.attach(client);
        } else {
            key.attach(new NioClient(key));
        }
        ++clients;
    }

    void closeClient(SelectionKey key) {
        log.trace("close one client");
        cache.offer((NioClient) key.attachment()); // recycle the client
        key.attach(null);
        --clients;
    }

    void receiveData(SelectionKey key, ByteBuffer buffer) {
        log.trace("deliver data to actual client");
        ((NioClient) key.attachment()).receiveData(buffer);
    }
}
