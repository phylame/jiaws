package pw.phylame.jiaws.http;


import pw.phylame.jiaws.ProtocolException;

public class HttpException extends ProtocolException {
    public HttpException() {
    }

    public HttpException(String message) {
        super(message);
    }

    public HttpException(String message, Throwable cause) {
        super(message, cause);
    }

    public HttpException(Throwable cause) {
        super(cause);
    }
}
