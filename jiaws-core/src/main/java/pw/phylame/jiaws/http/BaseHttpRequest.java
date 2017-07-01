package pw.phylame.jiaws.http;

import jclp.util.CollectionMap;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import pw.phylame.jiaws.BadRequestException;

import static pw.phylame.jiaws.http.HttpUtils.isTokenString;

@Slf4j
public abstract class BaseHttpRequest implements HttpConstants {
    @Getter
    private String method;

    @Getter
    private String version;

    @Getter
    private String url;

    @Getter
    private String host;

    @Getter
    private String path;

    @Getter
    private String query;

    @Getter
    private int contentLength = 0;

    @Getter
    private String contentType = null;

    @Getter
    private CollectionMap<String, String> params = HttpUtils.newValuesMap();

    @Getter
    private CollectionMap<String, String> headers = HttpUtils.newValuesMap();

    @Getter
    private boolean endOfHeader = false;

    // private data for parsing
    private String fieldName = null;
    private StringBuilder fieldValue = new StringBuilder();

    // reset this request to reuse
    public void reset() {
        method = version = url = host = path = query = null;
        contentLength = 0;
        contentType = null;
        endOfHeader = false;
        fieldValue.setLength(0);
        fieldName = null;
        params.clear();
        headers.clear();
    }

    protected void onRequestError(int code, String reason) throws HttpException {
        log.debug("error in http message {}, {}", code, reason);
    }

    // no returned method
    protected final void onError400(int line) throws BadRequestException {
        throw new BadRequestException("Bad HTTP message in line: " + line);
    }

    protected final void parseRequestLine(String str, int line) throws BadRequestException {
        int begin = str.indexOf(SP);
        if (begin == -1) {
            log.debug("not found method in line({})", line);
            onError400(line);
        }
        method = str.substring(0, begin);
        if (!isTokenString(method)) {
            log.debug("invalid method in line{})", line);
            onError400(line);
        }
        int end = str.lastIndexOf(SP);
        if (end == begin) {
            log.debug("no version found in line({})", line);
            onError400(line);
        }
        version = str.substring(end + 1);
        if (!"HTTP/1.1".equals(version) && !"HTTP/1.0".equals(version)) {
            log.debug("unsupported version({}) in line({})", version, line);
            onError400(line);
        }
        url = str.substring(begin + 1, end);
        parseRequestURI(url);
    }

    protected final void parseRequestURI(String str) {
        int index = str.indexOf('?');
        if (index != -1) {
            path = str.substring(0, index);
            query = str.substring(index + 1);
            parseQueryString(query);
        } else {
            path = str;
        }
    }

    protected final void parseQueryString(String str) {
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

    protected final void parseHeaderField(String str, int line) throws BadRequestException {
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
        parseLastHeader(line);
        index = str.indexOf(':');
        if (index == -1) {
            log.debug("no name of header field found in line({})", line);
            onError400(line);
        }
        val name = str.substring(0, index);
        if (!isTokenString(name)) {
            log.debug("invalid name({}) of header field in line({})", name, line);
            onError400(line);
        }
        fieldName = name;
        fieldValue.append(str.substring(index + 1).trim());
    }

    protected final void parseLastHeader(int line) throws BadRequestException {
        if (fieldName != null) {
            String name = fieldName.toLowerCase();
            String value = fieldValue.toString();
            fieldValue.setLength(0);
            fieldName = null;
            switch (name) {
                case "host": {
                    host = value;
                }
                break;
                case "content-length": {
                    try {
                        contentLength = Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        onError400(line);
                    }
                }
                break;
                case "content-type": {
                    contentType = value;
                }
                break;
                default: {
                    headers.addOne(name, value);
                }
                break;
            }
        }
    }

    protected final void onHeaderFieldsEnd(int line) throws BadRequestException {
        endOfHeader = true;
        parseLastHeader(line);
    }

    public void debugMessage() {
        System.out.println("Request details:");
        System.out.println("\tmethod\t\t\t\t" + method);
        System.out.println("\tversion\t\t\t\t" + version);
        System.out.println("\thost\t\t\t\t" + host);
        System.out.println("\tpath\t\t\t\t" + path);
        System.out.println("\tquery\t\t\t\t" + query);
        System.out.println("\tcontent length\t\t" + contentLength);
        System.out.println("\tcontent type\t\t" + contentType);
        System.out.println("Parameters:");
        params.forEach((name, values) -> System.out.printf("\t%s=%s\n", name, values));
        System.out.println("Headers:");
        headers.forEach((name, values) -> System.out.printf("\t%s=%s\n", name, values));
    }
}
