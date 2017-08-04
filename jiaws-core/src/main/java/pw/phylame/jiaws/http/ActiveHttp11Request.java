package pw.phylame.jiaws.http;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import pw.phylame.jiaws.ActiveProtocolParser;
import pw.phylame.jiaws.ProtocolException;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import static pw.phylame.jiaws.http.HttpUtils.isNumericChar;
import static pw.phylame.jiaws.http.HttpUtils.valueOfHexchar;

@Slf4j
public class ActiveHttp11Request extends BaseHttpRequest implements ActiveProtocolParser {

    @Override
    public void parseData(InputStream in) throws ProtocolException {
        if (!in.markSupported()) {
            throw new IllegalArgumentException("Stream is not mark supported");
        }
        try {
            parseInput(in);
        } catch (IOException e) {
            onError400(-1);
        }
    }

    private void parseInput(InputStream in) throws IOException, ProtocolException {
        int b, line = 1;
        boolean hasCR = false;
        val buffer = new ByteArrayOutputStream(DEFAULT_LINE_LIMIT);
        loop:
        while ((b = in.read()) != -1) {
            switch (b) {
                case PE: {
                    if (getMethod() != null) {
                        buffer.write(PE);
                    } else {// try decode quoted string in request line
                        in.mark(1);
                        b = in.read();
                        if (b == -1) {
                            throw new EOFException();
                        } else if (b == PE) { // %%
                            buffer.write(PE);
                        } else if (isNumericChar(b)) { // second is [0-9|a-f|A-F]
                            in.mark(1);
                            int e = in.read();
                            if (e == -1) {
                                throw new EOFException();
                            } else if (!isNumericChar(e)) { // third is not [0-9|a-f|A-F]
                                buffer.write(PE);
                                buffer.write(b);
                                in.reset();
                            } else {
                                buffer.write((valueOfHexchar(b) << 4) + valueOfHexchar(e));
                            }
                        } else { // others
                            buffer.write(PE);
                            in.reset();
                        }
                    }
                }
                break;
                case CR: {
                    hasCR = true;
                }
                break;
                case LF: {
                    if (!hasCR) {
                        log.debug("found LF but no CR in line({})", line);
                        onError400(line);
                    }
                    if (buffer.size() == 0) {
                        if (getMethod() != null) { // end of header
                            onHeaderFieldsEnd(line);
                            break loop;
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
                    if (getMethod() == null) {
                        if (buffer.size() > DEFAULT_URL_LIMIT) {
                            log.debug("request line({}) is too long, max length is {}", line, DEFAULT_URL_LIMIT);
                            onRequestError(STATUS_TOO_LONG_URI, "Request URI Too Long");
                            return;
                        }
                    } else {
                        if (buffer.size() > DEFAULT_LINE_LIMIT) {
                            log.debug("header line({}) is too long, max length is {}", line, DEFAULT_LINE_LIMIT);
                            onError400(line);
                        }
                    }
                    buffer.write(b);
                }
                break;
            }
        }
    }
}
