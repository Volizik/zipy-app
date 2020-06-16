package com.app.zipy;

public class UrlHelper {
    public static String addParamsToURL(String url) {
        if (url.contains("utm_medium=app&utm_source=app") || url.contains("utm_medium%3Dapp%26utm_source%3Dapp") || !url.contains("zipy.co.il")) {
            return url;
        }

        if (url.contains("#")) {
            String[] urlArray = url.split("#");
            String params = url.contains("?") ? "&utm_medium=app&utm_source=app" : "?utm_medium=app&utm_source=app";
            return urlArray[0] + params + "#" + urlArray[1];
        } else {
            return url += url.contains("?") ? "&utm_medium=app&utm_source=app" : "?utm_medium=app&utm_source=app";
        }
    }
}
