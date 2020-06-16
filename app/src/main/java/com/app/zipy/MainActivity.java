package com.app.zipy;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import com.onesignal.OSNotificationOpenResult;
import com.onesignal.OSSubscriptionObserver;
import com.onesignal.OSSubscriptionStateChanges;
import com.onesignal.OneSignal;
import org.json.JSONObject;

public class MainActivity extends Activity implements OSSubscriptionObserver {
    public WebView webView;
    private WebView mWebviewPop;
    private FrameLayout mContainer;

    Activity activity;
    Context context;

    private String home_page_url;
    private String home_page_url_prefix;

    JavaScriptInterface javaScriptInterface;
    NetworkHelper networkHelper;
    GoogleAuth googleAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.mWebView);
        mContainer = findViewById(R.id.view);

        activity = this;
        context = this.getApplicationContext();

        home_page_url = activity.getString(R.string.home_page_url);
        home_page_url_prefix = activity.getString(R.string.home_page_url_prefix);

        final Intent intent = getIntent();
        final String action = intent.getAction();
        final String data = intent.getDataString();
        javaScriptInterface = new JavaScriptInterface();

        networkHelper = new NetworkHelper(activity);
        googleAuth = new GoogleAuth(activity, webView);
        googleAuth.init();

        OneSignal.startInit(this)
                .setNotificationOpenedHandler(new OneSignal.NotificationOpenedHandler() {
                    @Override
                    public void notificationOpened(OSNotificationOpenResult result) {
                        JSONObject data = result.notification.payload.additionalData;
                        String launchingUrl;
                        if (data != null) {
                            launchingUrl = UrlHelper.addParamsToURL(data.optString("launchUrl", home_page_url));
                            Log.d("LOGGED", "notificationOpened: " + launchingUrl);

                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(launchingUrl), context, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                    }
                })
                .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
                .unsubscribeWhenNotificationsAreDisabled(true)
                .init();
        OneSignal.addSubscriptionObserver(this);


        if (networkHelper.isOnline()) {
            WebSettings webSettings = webView.getSettings();
            final String ua = webSettings.getUserAgentString().replace("; wv", "");

            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);
            cookieManager.setAcceptThirdPartyCookies(webView, true);

            webSettings.setJavaScriptEnabled(true);
            webSettings.setUserAgentString(ua);
            webSettings.setAppCacheEnabled(true);
            webSettings.setDomStorageEnabled(true);
            webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
            webSettings.setSupportMultipleWindows(true);
            webSettings.setMixedContentMode( WebSettings.MIXED_CONTENT_ALWAYS_ALLOW );

            webView.addJavascriptInterface(javaScriptInterface, "android");

            webView.setWebViewClient(new CustomWebViewClient());
            webView.setWebChromeClient(new UriWebChromeClient());

            if (Intent.ACTION_VIEW.equals(action) && data != null) {
                webView.loadUrl(UrlHelper.addParamsToURL(data));

            } else {
                webView.loadUrl(UrlHelper.addParamsToURL(home_page_url));
            }

        } else {
            networkHelper.showOnlineAlert();
        }

    }

    @Override
    public void onBackPressed() {

        if (mWebviewPop != null) {
            mWebviewPop.setVisibility(View.GONE);
            mContainer.removeView(mWebviewPop);
            mWebviewPop = null;
        }
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            new AlertDialog.Builder(this, R.style.AlertDialogCustom)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("יציאה")
                    .setMessage("האם לצאת מזיפי?")
                    .setPositiveButton("כן", new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }

                    })
                    .setNegativeButton("לא", null)
                    .show();
        }
    }

    private class CustomWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            String host = Uri.parse(url).getHost();
            if (networkHelper.isOnline()) {

                if( url.startsWith("http:") || url.startsWith("https:") ) {
                    if (url.startsWith("https://t.me")) {
                        ThirdPartyApp.intentMessageTelegram(activity);
                        return false;
                    }
                    if (host.contains(home_page_url_prefix)) {
                        if (mWebviewPop != null) {
                            mWebviewPop.setVisibility(View.GONE);
                            mContainer.removeView(mWebviewPop);
                            mWebviewPop = null;
                        }
                        url = UrlHelper.addParamsToURL(url);
                        view.loadUrl(url);
                        return false;
                    }
                    if (host.equals("m.facebook.com") || host.equals("www.facebook.com") || host.equals("facebook.com")) {
                        return false;
                    }

                    // Otherwise, the link is not for a page on my site, so launch
                    // another Activity that handles URLs
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return false;
                } else if (url.startsWith("tel:")) {
                    Intent tel = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
                    startActivity(tel);
                    return true;
                } else if (url.startsWith("mailto:")) {
                    Intent mail = new Intent(Intent.ACTION_SEND);
                    mail.setType("application/octet-stream");
                    String AddressMail = new String(url.replace("mailto:" , "")) ;
                    mail.putExtra(Intent.EXTRA_EMAIL, new String[]{ AddressMail });
                    mail.putExtra(Intent.EXTRA_SUBJECT, "");
                    mail.putExtra(Intent.EXTRA_TEXT, "");
                    startActivity(mail);
                    view.loadUrl(UrlHelper.addParamsToURL(home_page_url));
                    return true;
                } else if (url.startsWith("tg:resolve")) {
                    view.loadUrl(home_page_url);
                    return true;
                }
                return false;
            } else {
                networkHelper.showOnlineAlert();
                return true;
            }
        }

        public void onPageFinished(WebView view, String url) {
            String script = "javascript:" +
                    "document.documentElement.classList.add('android-app');" +
                    "window.addEventListener('beforeunload', function () {" +
                    "   document.querySelector('.android-app__preloader').classList.remove('hidden-all');" +
                    "   return null;" +
                    "})";
            webView.loadUrl(script);
            if (url.startsWith("https://m.facebook.com/dialog/oauth")) {
                if (mWebviewPop != null) {
                    mWebviewPop.setVisibility(View.GONE);
                    mContainer.removeView(mWebviewPop);
                    mWebviewPop = null;
                }
                view.loadUrl(url);
                return;
            }

            super.onPageFinished(view, url);
        }

    }

    private class UriWebChromeClient extends WebChromeClient {

        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
            mWebviewPop = new WebView(context);
            mWebviewPop.setVerticalScrollBarEnabled(false);
            mWebviewPop.setHorizontalScrollBarEnabled(false);
            mWebviewPop.setWebViewClient(new CustomWebViewClient());
            mWebviewPop.getSettings().setJavaScriptEnabled(true);
            mWebviewPop.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            mContainer.addView(mWebviewPop);
            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(mWebviewPop);
            resultMsg.sendToTarget();

            return true;
        }

        @Override
        public void onCloseWindow(WebView window) {
            Log.d("onCloseWindow", "called");
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GoogleAuth.RC_SIGN_IN) {
            googleAuth.getSignedInAccountFromIntent(data);
        }
    }

    public class JavaScriptInterface {
        String token;

        @JavascriptInterface
        public void googleAuth() {
            googleAuth.signIn();
        }

        @JavascriptInterface
        public String getToken() {
            return token;
        }
    }

    public void onOSSubscriptionChanged(OSSubscriptionStateChanges stateChanges) {
        if (!stateChanges.getFrom().getSubscribed() && stateChanges.getTo().getSubscribed()) {
            javaScriptInterface.token = stateChanges.getTo().getUserId();
        }
    }

}
