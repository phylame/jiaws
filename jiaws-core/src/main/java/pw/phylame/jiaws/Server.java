package pw.phylame.jiaws;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class Server {
    private static ExecutorService pool = Executors.newFixedThreadPool(8);
    private static ByteBuffer buffer = ByteBuffer.allocate(8192);

    public static void main(String[] args) throws IOException {
        Selector selector = Selector.open();

        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.bind(new InetSocketAddress(80));
        ssc.configureBlocking(false);
        ssc.register(selector, SelectionKey.OP_ACCEPT);

        while (!Thread.interrupted()) {
            if (selector.select() == 0) {
                continue;
            }
            Iterator<SelectionKey> it = selector.selectedKeys().iterator();
            while (it.hasNext()) {
                handleKey(selector, it.next());
                it.remove();
            }
        }

        ssc.close();
        selector.close();
    }

    private static void handleKey(Selector selector, SelectionKey key) throws IOException {
        if (key.isAcceptable()) {
            acceptClient(selector, key);
        } else if (key.isReadable()) {
            receiveData(key);
        } else if (key.isWritable()) {
            writeData(key);
        }
    }

    private static void acceptClient(Selector selector, SelectionKey key) throws IOException {
        SocketChannel client = ((ServerSocketChannel) key.channel()).accept();
        client.configureBlocking(false);
        SelectionKey readKey = client.register(selector, SelectionKey.OP_READ);
        log.debug("accept new client {} of key {}", client, readKey);
        readKey.attach(new Client(readKey, pool));
    }

    private static void receiveData(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        int count = channel.read(buffer);
        log.debug("received {} bytes data from {}", count, key);
        if (count > 0) {
            buffer.flip();
            ((Client) key.attachment()).received(buffer);
        } else if (count < 0) {
            channel.close();
        }
        buffer.clear();
    }

    private static void writeData(SelectionKey key) {
        ((Client) key.attachment()).flush();
    }

    @Slf4j
    private static class Client implements Runnable {
        private final SelectionKey readKey;
        private final ExecutorService pool;

        private InputStream socketInput;
        private OutputStream inboundPipe;
        private boolean dataReady = false;

        private OutputStream socketOutput;
        private InputStream outboundPipe;

        Client(SelectionKey readKey, ExecutorService queue) throws IOException {
            this.readKey = readKey;
            this.pool = queue;

            PipedOutputStream pos = new PipedOutputStream();
            socketInput = new PipedInputStream(pos);
            inboundPipe = new BufferedOutputStream(pos);

            pos = new PipedOutputStream();
            outboundPipe = new PipedInputStream(pos);
            socketOutput = new BufferedOutputStream(pos);
        }

        private void received(ByteBuffer buffer) throws IOException {
            log.debug("client {} received {} bytes data from buffer", this, buffer.remaining());
            inboundPipe.write(buffer.array(), 0, buffer.remaining());
            inboundPipe.flush();
            if (!dataReady) {
                dataReady = true;
                pool.submit(this);
            }
        }

        private void flush() {

        }

        @Override
        public void run() {
            log.debug("client is running");
        }
    }

    static class ClientOutputStream extends OutputStream {
        private AutoFlushedBuffer buffer;

        @Override
        public void write(int b) throws IOException {
            buffer.put((byte) b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            buffer.put(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            buffer.put(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            buffer.flush();
        }

        @Override
        public void close() throws IOException {
            buffer.close();
        }
    }
}
