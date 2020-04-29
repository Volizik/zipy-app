package com.app.zipy;

import android.content.Context;
import android.webkit.JavascriptInterface;

public class JavaScriptInterface {
    public static String token;
    private static JavaScriptInterface instance;
    private MainActivity mainActivity;

    private JavaScriptInterface() {}

    public JavaScriptInterface(MainActivity activity) {
        mainActivity = activity;
    }

    public static JavaScriptInterface getInstance() {
        if (instance == null) {
            instance = new JavaScriptInterface();
        }
        return instance;
    }

    @JavascriptInterface
    public void googleAuth() {
        if (mainActivity != null) {
            mainActivity.signIn();
        }
    }

    @JavascriptInterface
    public String getToken() {
        return token;
    }

}