package pw.phylame.jiaws;

import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;

@Slf4j
class ClientManager {
    int clients = 0;
    Queue<NioHttpClient> cache = new LinkedList<>();

    void newClient(Selector selector, SocketChannel channel) {
        log.trace("create {}th new client", clients + 1);
        try {
            val key = channel.register(selector, SelectionKey.OP_READ);
            NioHttpClient client = cache.poll(); // get a cached client
            if (client == null) {
                client = new NioHttpClient();
            }
            client.init(key, this);
            key.attach(client);
            ++clients;
        } catch (Exception e) {
            log.error("cannot register for reading event", e);
        }
    }

    void closeClient(SelectionKey key) {
        val client = (NioHttpClient) key.attachment();
        log.trace("recycle client {}", client);
        key.attach(null);
        key.cancel();
        try {
            key.channel().close();
        } catch (IOException e) {
            log.error("cannot close channel", e);
        }
        cache.offer(client);
        client.cleanup();
        --clients;
    }

    void receiveData(SelectionKey key, ByteBuffer src) {
        log.trace("deliver data to actual client");
        ((NioHttpClient) key.attachment()).receive(src);
    }
}
