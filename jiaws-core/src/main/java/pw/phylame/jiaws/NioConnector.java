package pw.phylame.jiaws;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;

@Slf4j
public class NioConnector implements Closeable {
    private final Selector selector;
    private final ByteBuffer buffer;
    private final ServerSocketChannel channel;
    private final ClientManager clientManager;

    public NioConnector(@NonNull ClientManager clientManager) throws IOException {
        selector = Selector.open();
        buffer = ByteBuffer.allocate(8192);
        channel = ServerSocketChannel.open();
        this.clientManager = clientManager;
        init();
    }

    private void init() throws IOException {
        channel.configureBlocking(false);
        channel.bind(new InetSocketAddress(80));
        channel.register(selector, SelectionKey.OP_ACCEPT);
    }

    public void start() {
        try {
            while (!Thread.interrupted()) {
                if (selector.select() == 0) {
                    continue;
                }
                val it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    handleKey(it.next());
                    it.remove();
                }
            }
        } catch (ClosedSelectorException e) { // selector is closed
            log.debug("selector is closed", e);
        } catch (IOException e) {
            log.debug("unhandled io error", e);
        }
        try {
            close();
        } catch (IOException e) {
            log.debug("error when closing connector", e);
        }
    }

    private void handleKey(SelectionKey key) {
        if (key.isReadable()) { // the most frequent event for SocketChannel
            try {
                deliverData(key);
            } catch (IOException e) {
                log.error("cannot deliver data to client", e);
            }
            if (!key.isValid()) { // the socket channel is closed
                return;
            }
        } else if (key.isAcceptable()) { // for ServerSocketChannel
            try {
                acceptClient(key);
            } catch (IOException e) {
                log.error("cannot accept new client", e);
            }
            return;
        }
        if (key.isWritable()) { // for SocketChannel
            // todo notify client to write buffered data
        }
    }

    // accept new client and register for reading events
    private void acceptClient(SelectionKey key) throws IOException {
        val sc = ((ServerSocketChannel) key.channel()).accept();
        assert sc != null : "BUG: since we are non-blocking";
        log.trace("accept new client: {}", sc.getRemoteAddress());
        sc.configureBlocking(false);
        clientManager.newClient(key.selector(), sc);
    }

    // deliver data to corresponding client
    private void deliverData(SelectionKey key) throws IOException {
        val sc = (SocketChannel) key.channel();
        int n;
        try {
            n = sc.read(buffer);
        } catch (IOException e) {
            if (log.isTraceEnabled()) {
                log.trace("connection reset by client: %s" + sc.getRemoteAddress(), e);
            }
            n = -1;
        }
        if (n == -1) { // end of stream
            log.trace("close channel when eof: {}", sc.getRemoteAddress());
            clientManager.closeClient(key);
            buffer.clear();
            return;
        }
        assert n != 0 : "BUG: since we are non-blocking";
        buffer.flip();
        log.trace("read {} bytes data from channel {}", n, sc.getRemoteAddress());
        clientManager.receiveData(key, buffer);
        buffer.clear();
    }

    @Override
    public void close() throws IOException {
        channel.close();
        selector.close();
    }

    public static void main(String[] args) throws IOException {
        val cm = new ClientManager();
        val connector = new NioConnector(cm);
        connector.start();
    }
}
