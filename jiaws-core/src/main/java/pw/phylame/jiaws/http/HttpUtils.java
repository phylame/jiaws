package pw.phylame.jiaws.http;

import jclp.util.CollectionMap;

import java.util.LinkedHashMap;
import java.util.LinkedList;

public final class HttpUtils {
    private HttpUtils() {
    }

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
        SEPARATORS[HttpConstants.SP] = 1;
        SEPARATORS[HttpConstants.HT] = 1;
    }

    public static boolean isTokenString(String str) {
        char ch;
        for (int i = 0, end = str.length(); i != end; ++i) {
            ch = str.charAt(i);
            if (!isTokenChar(ch)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isTokenChar(int b) {
        return !(b < 32 || b == 127 || SEPARATORS[b] == 1);
    }

    public static boolean isNumericChar(int b) {
        return b >= '0' && b <= '9' || b >= 'a' && b <= 'f' || b >= 'A' && b <= 'F';
    }

    public static int valueOfHexchar(int ch) {
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

    public static CollectionMap<String, String> newValuesMap() {
        return new CollectionMap<>(new LinkedHashMap<>(), LinkedList.class);
    }
}
