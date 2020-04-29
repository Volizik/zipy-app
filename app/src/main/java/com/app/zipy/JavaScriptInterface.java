package com.app.zipy;

import android.content.Context;
import android.webkit.JavascriptInterface;

public class JavaScriptInterface {
    private static JavaScriptInterface instance;
    private String token = "";
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


    public void setFCMToken(String token) {
        this.token = token;
    }

    @JavascriptInterface
    public void googleAuth() {
        if (mainActivity != null) {
            mainActivity.signIn();
        }
    }

    @JavascriptInterface
    public String getFCMToken() {
        return this.token;
    }

}