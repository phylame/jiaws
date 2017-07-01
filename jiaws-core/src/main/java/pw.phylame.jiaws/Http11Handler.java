package pw.phylame.jiaws;

import jclp.io.IOUtils;
import jclp.util.CollectionMap;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;

@Slf4j
public class Http11Handler {
    private InputStream input;
    private PrintStream output;

    @SneakyThrows(UnsupportedEncodingException.class)
    public void init(InputStream input, OutputStream output) {
        this.input = input;
        this.output = new PrintStream(output, false, ENCODING);
    }

    private void writeResponseHeaders() {
        writeHeaderField("Server", "Jiaws/1.0");
    }

    // meta data of the entity body
    private void writeEntityHeaders() {
    }

    // extra header fields by user
    private void writeUserHeaders() {
    }

    private void writeStatusLine(int code, String reason) {
        output.append(version)
                .append(SP)
                .append(Integer.toString(code))
                .append(SP)
                .append(reason)
                .append(CR)
                .append(LF);
    }

    private void writeHeaderField(String name, String value) {
        output.append(name)
                .append(": ")
                .append(value)
                .append(CR)
                .append(LF);
    }

    private void writeEntityBody(InputStream input) throws IOException {
        IOUtils.copy(input, output, -1);
    }

    private void writeEntityBody(ByteBuffer buffer) throws IOException {
        byte[] buf = new byte[buffer.remaining()];
        buffer.get(buf);
        output.write(buf);
    }

    private void writeEntityBody(byte[] b) throws IOException {
        output.write(b);
    }

    private void writeEntityBody(byte[] b, int off, int len) throws IOException {
        output.write(b, off, len);
    }

    public void sendError(int code, String reason) {
        writeStatusLine(code, reason);
        writeResponseHeaders();
        writeUserHeaders();
        output.append(CR)
                .append(LF)
                .flush();
    }

    private void send400() {
        sendError(400, "Bad Request");
    }

    private void parseMessage(InputStream in) throws IOException {
        int b, line = 1;
        boolean hasCR = false;
        val buffer = new ByteArrayOutputStream(MAX_LINE_LENGTH);
        loop:
        while ((b = in.read()) != -1) {
            switch (b) {
                case PE: {
                    if (method != null) {
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
                        send400();
                        return;
                    }
                    if (buffer.size() == 0) {
                        if (method != null) { // end of header
                            break loop;
                        }
                    } else if (method == null) {
                        parseRequestLine(buffer.toString(ENCODING), line);
                    } else {
                        parseHeaderField(buffer.toString(ENCODING), line);
                    }
                    ++line;
                    buffer.reset();
                }
                break;
                default: {
                    if (method == null) {
                        if (buffer.size() > MAX_URL_LENGTH) {
                            log.debug("request line({}) is too long, max length is {}", line, MAX_LINE_LENGTH);
                            sendError(414, "Request URI Too Long");
                            return;
                        }
                    } else {
                        if (buffer.size() > MAX_LINE_LENGTH) {
                            log.debug("header line({}) is too long, max length is {}", line, MAX_LINE_LENGTH);
                            send400();
                            return;
                        }
                    }
                    buffer.write(b);
                }
                break;
            }
        }
    }

    private void parseRequestLine(String str, int line) throws IOException {
        int begin = str.indexOf(SP);
        if (begin == -1) {
            log.debug("not found method in line({})", line);
            send400();
            return;
        }
        method = str.substring(0, begin);
        if (!isTokenString(method)) {
            log.debug("invalid method in line{})", line);
            send400();
            return;
        }
        int end = str.lastIndexOf(SP);
        if (end == begin) {
            log.debug("no version found in line({})", line);
            send400();
            return;
        }
        version = str.substring(end + 1);
        if (!"HTTP/1.1".equals(version) && !"HTTP/1.0".equals(version)) {
            log.debug("unsupported version({}) in line({})", version, line);
            send400();
            return;
        }
        url = str.substring(begin + 1, end);
        parseRequestURI(url);
    }

    private void parseRequestURI(String str) throws IOException {
        int index = str.indexOf('?');
        if (index != -1) {
            path = str.substring(0, index);
            query = str.substring(index + 1);
            parseQueryString(query);
        } else {
            path = str;
        }
    }

    private void parseQueryString(String str) throws IOException {
        int begin = 0, end = str.length(), index, pos;
        while (true) {
            index = str.indexOf('&', begin);
            if (index == -1) {
                index = end;
            }
            if (index == begin) {
                break;
            }
            pos = str.indexOf('=', begin);
            if (pos > index) {
                params.addOne(str.substring(begin, index), "");
                break;
            }
            if (pos == -1) {
                params.addOne(str.substring(begin, index), "");
            } else {
                params.addOne(str.substring(begin, pos), str.substring(pos + 1, index));
            }
            begin = index + 1;
            if (index == end) {
                break;
            }
        }
    }

    private String fieldName = null;
    private StringBuilder fieldValue = new StringBuilder();

    private void parseHeaderField(String str, int line) throws IOException {
        int index = str.indexOf(SP);
        if (index == 0) {
            fieldValue.append(str.trim());
            return;
        }
        index = str.indexOf(HT);
        if (index == 0) {
            fieldValue.append(str.trim());
            return;
        }
        if (fieldName != null) {
            headers.addOne(fieldName.toLowerCase(), fieldValue.toString());
            fieldValue.setLength(0);
            fieldName = null;
        }
        index = str.indexOf(':');
        if (index == -1) {
            log.debug("no name of header field found in line({})", line);
            send400();
            return;
        }
        String name = str.substring(0, index);
        if (!isTokenString(name)) {
            log.debug("invalid name({}) of header field in line({})", name, line);
            send400();
            return;
        }
        fieldName = name;
        fieldValue.append(str.substring(index + 1).trim());
    }

    private static boolean isTokenString(String str) {
        char ch;
        for (int i = 0, end = str.length(); i != end; ++i) {
            ch = str.charAt(i);
            if (!isTokenChar(ch)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isTokenChar(int b) {
        return !(b < 32 || b == 127 || SEPARATORS[b] == 1);
    }

    private static boolean isNumericChar(int b) {
        return b >= '0' && b <= '9' || b >= 'a' && b <= 'f' || b >= 'A' && b <= 'F';
    }

    private static int valueOfHexchar(int ch) {
        if (ch >= '0' && ch <= '9') {
            return ch - '0';
        }
        if (ch >= 'a' && ch <= 'f') {
            return ch - 'a' + 10;
        }
        if (ch >= 'A' && ch <= 'F') {
            return ch - 'A' + 10;
        }
        throw new IllegalArgumentException("invalid char: " + ch);
    }

    private static final char PE = '%';
    private static final char SP = ' ';
    private static final char HT = '\t';
    private static final char CR = '\r';
    private static final char LF = '\n';

    private static final int MAX_LINE_LENGTH = 4096;
    private static final int MAX_URL_LENGTH = 2048;
    private static final String ENCODING = "utf-8";

    private static final byte[] SEPARATORS = new byte[127];

    static {
        SEPARATORS['('] = 1;
        SEPARATORS[')'] = 1;
        SEPARATORS['<'] = 1;
        SEPARATORS['>'] = 1;
        SEPARATORS['@'] = 1;
        SEPARATORS[','] = 1;
        SEPARATORS[';'] = 1;
        SEPARATORS[':'] = 1;
        SEPARATORS['\''] = 1;
        SEPARATORS['"'] = 1;
        SEPARATORS['/'] = 1;
        SEPARATORS['['] = 1;
        SEPARATORS[']'] = 1;
        SEPARATORS['?'] = 1;
        SEPARATORS['='] = 1;
        SEPARATORS['{'] = 1;
        SEPARATORS['}'] = 1;
        SEPARATORS[SP] = 1;
        SEPARATORS[HT] = 1;
    }

    @Getter
    private CollectionMap<String, String> params = new CollectionMap<>(new LinkedHashMap<>());
    @Getter
    private CollectionMap<String, String> headers = new CollectionMap<>(new LinkedHashMap<>());
    @Getter
    private String method;
    @Getter
    private String version;
    @Getter
    private String url;
    @Getter
    private String path;
    @Getter
    private String query;
}
