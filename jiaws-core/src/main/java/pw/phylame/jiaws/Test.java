package pw.phylame.jiaws;

import pw.phylame.jiaws.http.PassiveHttp11Request;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class Test {
    public static void main(String[] args) throws IOException {
        FileInputStream fis = new FileInputStream("E:/tmp/1.txt");
        FileChannel channel = fis.getChannel();
        PassiveHttp11Request request = new PassiveHttp11Request();
//        ActiveHttpRequest request = new ActiveHttpRequest();
//        ServerSocket serverSocket = new ServerSocket(80);
//        InputStream sis = serverSocket.accept().getInputStream();
//        BufferedInputStream bis = new BufferedInputStream(fis);
//        request.parseMessage(bis);
        ByteBuffer buffer = ByteBuffer.allocate(512);
        buffer.clear();
        while (!request.isEndOfHeader() && channel.read(buffer) != -1) {
            buffer.flip();
            try {
                request.receiveData(buffer);
            } catch (ProtocolException e) {
                e.printStackTrace();
            }
            buffer.compact();
        }
        request.debugMessage();
        buffer.flip();
        while (buffer.hasRemaining()) {
            System.out.print((char) buffer.get());
        }
        buffer.clear();
        channel.read(buffer);
        buffer.flip();
        while (buffer.hasRemaining()) {
            System.out.print((char) buffer.get());
        }
    }
}
