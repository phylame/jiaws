package pw.phylame.jiaws;

import java.nio.channels.ByteChannel;

public class BioHttpClientDraft {
    ByteChannel channel;

    public BioHttpClientDraft(ByteChannel channel) {
        this.channel = channel;
    }

    // when connection created, begin the workflow
    void run() {
        // 1. parse the http message from channel
        // 2. do the business logic, eg: cgi, servlet, etc.
        // 3. write response to the channel
        // 4. if keep connection alive then go to step 1, else go to step 5
        // 5. close the connection, release related resources, end workflow.
    }
}
