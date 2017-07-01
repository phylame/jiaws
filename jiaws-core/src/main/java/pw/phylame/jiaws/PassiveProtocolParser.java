package pw.phylame.jiaws;

import java.nio.ByteBuffer;

public interface PassiveProtocolParser {
    boolean isFinished();

    void receiveData(ByteBuffer src) throws ProtocolException;
}
