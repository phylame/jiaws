package pw.phylame.jiaws;

import java.io.InputStream;

public interface ActiveProtocolParser {
    void parseData(InputStream in) throws ProtocolException;
}
