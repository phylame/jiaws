package pw.phylame.jiaws.http;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import pw.phylame.jiaws.PassiveProtocolParser;
import pw.phylame.jiaws.ProtocolException;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import static pw.phylame.jiaws.http.HttpUtils.isNumericChar;
import static pw.phylame.jiaws.http.HttpUtils.valueOfHexchar;

@Slf4j
public class PassiveHttpRequest extends BaseHttpRequest implements PassiveProtocolParser {
    // private data for parsing
    private int line = 1;
    private byte firstChar = -1;
    private boolean foundCr = false;
    private boolean foundPe = false;
    private ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    @Override
    public boolean isFinished() {
        return isEndOfHeader();
    }

    @Override
    @SneakyThrows(UnsupportedEncodingException.class)
    public void receiveData(ByteBuffer src) throws ProtocolException {
        byte b;
        while (src.hasRemaining()) {
            switch (b = src.get()) {
                case PE: {
                    if (getMethod() != null) {
                        buffer.write(PE);
                    } else if (foundPe) { // %%
                        buffer.write(PE);
                        foundPe = false;
                    } else {
                        foundPe = true;
                    }
                }
                break;
                case CR: {
                    foundCr = true;
                }
                break;
                case LF: {
                    if (!foundCr) {
                        log.debug("found LF but no CR in line({})", line);
                        buffer.reset();
                        onError400(line);
                    }
                    if (buffer.size() == 0) {
                        if (getMethod() != null) { // end of header
                            onHeaderFieldsEnd(line);
                            return;
                        }
                    } else if (getMethod() == null) {
                        parseRequestLine(buffer.toString(DEFAULT_CHARSET), line);
                    } else {
                        parseHeaderField(buffer.toString(DEFAULT_CHARSET), line);
                    }
                    ++line;
                    buffer.reset();
                }
                break;
                default: {
                    if (foundPe) { // after %
                        if (firstChar == -1) { // the first char after PE
                            if (isNumericChar(b)) {
                                firstChar = b;
                                continue;
                            } else {
                                buffer.write(PE);
                                foundPe = false;
                            }
                        } else { // the second char after PE
                            if (isNumericChar(b)) {
                                buffer.write((valueOfHexchar(firstChar) << 4) + valueOfHexchar(b));
                                firstChar = -1;
                                foundPe = false;
                                continue;
                            } else {
                                buffer.write(PE);
                                buffer.write(firstChar);
                            }
                            firstChar = -1;
                            foundPe = false;
                        }
                    }
                    if (getMethod() == null) {
                        if (buffer.size() > DEFAULT_URL_LIMIT) {
                            log.debug("request line({}) is too long, max length is {}", line, DEFAULT_URL_LIMIT);
                            onRequestError(STATUS_TOO_LONG_URI, "Request URI Too Long");
                            buffer.reset();
                            return;
                        }
                    } else {
                        if (buffer.size() > DEFAULT_LINE_LIMIT) {
                            log.debug("header line({}) is too long, max length is {}", line, DEFAULT_LINE_LIMIT);
                            buffer.reset();
                            onError400(line);
                        }
                    }
                    buffer.write(b);
                }
                break;
            }
        }
    }

    @Override
    public void reset() {
        super.reset();
        line = 1;
        firstChar = -1;
        foundCr = foundPe = false;
        buffer.reset();
    }
}
