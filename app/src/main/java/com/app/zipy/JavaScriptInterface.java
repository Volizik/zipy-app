//package com.app.zipy;
//
//import android.webkit.JavascriptInterface;
//
//public class JavaScriptInterface {
//    private String token;
//    private MainActivity mainActivity;
//    private static JavaScriptInterface instance;
//
//    private JavaScriptInterface() {}
//
//    static JavaScriptInterface getInstance() {
//        if (instance == null) {
//            instance = new JavaScriptInterface();
//        }
//        return instance;
//    }
//
//    JavaScriptInterface setToken(String token) {
//        instance.token = token;
//        return instance;
//    }
//
//    JavaScriptInterface setMainActivity(MainActivity activity) {
//        instance.mainActivity = activity;
//        return instance;
//    }
//
//    @JavascriptInterface
//    public void googleAuth() {
//        if (instance.mainActivity != null) {
//            instance.mainActivity.signIn();
//        }
//    }
//
//    @JavascriptInterface
//    public String getToken() {
//        return instance.token;
//    }
//
//}