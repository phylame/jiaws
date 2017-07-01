package pw.phylame.jiaws.http;

public interface HttpConstants {
    char SP = ' ';
    char HT = '\t';
    char CR = '\r';
    char LF = '\n';
    char PE = '%';

    String DEFAULT_CHARSET = "UTF-8";

    int DEFAULT_LINE_LIMIT = 4096;
    int DEFAULT_URL_LIMIT = 2048;

    int STATUS_BAD_REQUEST = 400;
    int STATUS_TOO_LONG_URI = 414;
}
