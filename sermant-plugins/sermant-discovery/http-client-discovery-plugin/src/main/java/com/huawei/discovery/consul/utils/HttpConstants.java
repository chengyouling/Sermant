package com.huawei.discovery.consul.utils;

public class HttpConstants {

    public static final String HTTP_REALM_CN = ".cn";

    public static final String HTTP_REALM_COM = ".com";

    public static final String HTTP_REALM_NET = ".net";

    public static final String HTTP_URI_HOST = "host";

    public static final String HTTP_URI_PATH = "path";

    public static final String HTTP_URI_PORT = "path";

    public static final String HTTP_GET = "GET";

    public static final String HTTP_POST = "POST";

    public static final String HTTP_URL_DOUBLIE_SLASH = "://";

    public static final String HTTP_URL_COLON = ":";

    public static final String HTTP_URL_UNKNOWN = "?";

    public static final char HTTP_URL_SINGLE_SLASH = '/';

    public static boolean isRealmHost(String host) {
        return host.endsWith(HTTP_REALM_CN) || host.endsWith(HTTP_REALM_COM) || host.endsWith(HTTP_REALM_NET);
    }
}
