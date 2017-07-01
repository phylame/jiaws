package pw.phylame.jiaws;

import java.io.PipedInputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;

public class NioHttpClientDraft {
    PipedInputStream input;
    SelectionKey key;

    // notify this client, data reached
    void recv(ByteBuffer buffer) {
        // 1. if parsing work is beginning then go to step 3, else go to step 2
        // 2. begin the parsing work(method run), eg: current thread, thread pool, etc.
        // 3. append new data to blocking-pipe
    }

    // notify this client, you can write data, invoked after step 6 in parsing working
    void sync() {
        // 1. write at most unwritten data, if all done then go to step 2, else go to step 3
        // 2. unregister write listener, end.
        // 3. end.
    }

    // the active parsing work, started when new data reached
    void activeParse() {
        // 1. parsing http message from pipe
        // 2. do the business logic, eg: cgi, servlet, etc.
        // 3. write response to channel in selection key, if all written then goto step 4, else go to 6
        // 4. if keep connection alive then go to step 1, else go to step 5
        // 5. close the connection, release related resources, end workflow.
        // 6. register write listener for writing event(channel is writable), next
        // 7. if keep connection alive then go to step 1, else end workflow.
    }

    // the passive parsing work
    void passiveParse(ByteBuffer buffer) {

    }
}
