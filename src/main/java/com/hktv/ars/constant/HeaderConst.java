package com.hktv.ars.constant;

public final class HeaderConst {
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_AUTHORIZATION_PREFIX = "Bearer";

    private HeaderConst() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
