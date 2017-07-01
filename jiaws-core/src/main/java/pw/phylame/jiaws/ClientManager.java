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
    Queue<NioHttpClient> cache = new LinkedList<>();

    void newClient(SelectionKey key) {
        log.trace("create {}th new client", clients + 1);
        NioHttpClient client = cache.poll(); // get a cached client
        if (client == null) {
            client = new NioHttpClient();
        }
        client.init(key);
        key.attach(client);
        ++clients;
    }

    void closeClient(SelectionKey key) {
        val client = (NioHttpClient) key.attachment();
        log.trace("recycle client {}", client);
        key.attach(null);
        cache.offer(client);
        client.cleanup();
        --clients;
    }

    void receiveData(SelectionKey key, ByteBuffer src) {
        log.trace("deliver data to actual client");
        ((NioHttpClient) key.attachment()).receive(src);
    }
}
