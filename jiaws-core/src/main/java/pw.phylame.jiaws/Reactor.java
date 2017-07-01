package pw.phylame.jiaws;


import jclp.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

@Slf4j
public class Reactor {
    public static void main(String[] args) throws IOException {
        val selector = Selector.open();

        val ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        ssc.bind(new InetSocketAddress(80));
        ssc.register(selector, SelectionKey.OP_ACCEPT);

        while (!Thread.interrupted()) {
            if (selector.select() == 0) {
                log.debug("not found any events");
                continue;
            }
            val it = selector.selectedKeys().iterator();
            while (it.hasNext()) {
                handleKey(it.next());
                it.remove();
            }
        }

        ssc.close();
        selector.close();
    }

    private static void handleKey(SelectionKey key) throws IOException {
        if (key.isReadable()) { // SocketChannel
            receiveData(key);
            if (!key.isValid()) { // channel is closed
                return;
            }
        }
        if (key.isAcceptable()) { // ServerSocketChannel
            acceptClient(key);
            return;
        }
        if (key.isWritable()) { // SocketChannel
            System.out.println("write");
        }
    }

    private static void acceptClient(SelectionKey key) throws IOException {
        val sc = ((ServerSocketChannel) key.channel()).accept();
        log.debug("accept new client {}", sc.getRemoteAddress());
        sc.configureBlocking(false);
        val readKey = sc.register(key.selector(), SelectionKey.OP_READ);
        log.debug("register {} for reading event", sc.getRemoteAddress());
        readKey.attach(new Client(readKey));
    }

    private static ByteBuffer buffer = ByteBuffer.allocate(8192);

    private static void receiveData(SelectionKey key) throws IOException {
        val sc = (SocketChannel) key.channel();
        int n;
        try {
            n = sc.read(buffer);
        } catch (IOException e) {
            log.debug("client reset the connection {}", sc);
            n = -1;
        }
        if (n == EOF) { // end of stream
            log.debug("close channel for EOF {}", sc.getRemoteAddress());
            buffer.clear();
            sc.close();
            return;
        } else if (n == 0) { // that may be bug, since invoked when key is readable
            throw new InternalError();
        }
        int width = 156;
        log.debug("received {} bytes from {}", n, sc.getRemoteAddress());
        String tip = "------------------------begin received data";
        System.out.println(tip + StringUtils.duplicated('-', width - tip.length()));
        buffer.flip();
        while (buffer.hasRemaining()) {
            System.out.print((char) buffer.get());
        }
        tip = "------------------------end received data";
        System.out.println(tip + StringUtils.duplicated('-', width - tip.length()));
        buffer.clear();
    }

    @RequiredArgsConstructor
    private static class Client {
        private final SelectionKey key;
    }

    private static final int EOF = -1;
}
