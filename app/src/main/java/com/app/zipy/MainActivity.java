package com.app.zipy;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.onesignal.OSNotificationOpenResult;
import com.onesignal.OSPermissionSubscriptionState;
import com.onesignal.OSSubscriptionObserver;
import com.onesignal.OSSubscriptionStateChanges;
import com.onesignal.OneSignal;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class MainActivity extends Activity implements OSSubscriptionObserver {

    public WebView webView;
    private WebView mWebviewPop;
    private FrameLayout mContainer;

    private static final String TAG = "MyActivity";
    GoogleSignInClient mGoogleSignInClient;
    int RC_SIGN_IN = 0;
    private String mAccessToken;
    Activity activity;
    Context context;
    private final String client_id = "163697187066.apps.googleusercontent.com";
    private final String client_secret = "6XgiioD9mZGx8-sXfiOIx-Tr";
    private final String home_page_url = "https://www.zipy.co.il/";
    private String home_page_url_prefix = "zipy.co.il";
    JavaScriptInterface javaScriptInterface;
    NetworkHelper networkHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        activity = this;
        context = this.getApplicationContext();

        final Intent intent = getIntent();
        final String action = intent.getAction();
        final String data = intent.getDataString();
        javaScriptInterface = new JavaScriptInterface();


        networkHelper = new NetworkHelper(activity);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestServerAuthCode(client_id)
                .requestScopes(new Scope("https://www.googleapis.com/auth/userinfo.email"))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        OneSignal.startInit(this)
                .setNotificationOpenedHandler(new OneSignal.NotificationOpenedHandler() {
                    @Override
                    public void notificationOpened(OSNotificationOpenResult result) {
                        JSONObject data = result.notification.payload.additionalData;
                        String launchingUrl;
                        if (data != null) {
                            launchingUrl = addParamsToURL(data.optString("launchUrl", home_page_url));
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
            webView = (WebView)findViewById(R.id.mWebView);
            mContainer = (FrameLayout) findViewById(R.id.view);
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

            webView.addJavascriptInterface(javaScriptInterface, "android");

            webView.setWebViewClient(new CustomWebViewClient());
            webView.setWebChromeClient(new UriWebChromeClient());

            if (Intent.ACTION_VIEW.equals(action) && data != null) {
                webView.loadUrl(addParamsToURL(data));

            } else {
                webView.loadUrl(addParamsToURL(home_page_url));
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
        } else if (webView.canGoBack()) {
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
                        if (mWebviewPop != null && !url.contains(home_page_url_prefix + "/tracking")) {
                            mWebviewPop.setVisibility(View.GONE);
                            mContainer.removeView(mWebviewPop);
                            mWebviewPop = null;
                        }
                        url = addParamsToURL(url);
                        view.loadUrl(url);
                        return false;
                    }

                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    if (host.contains("facebook")) {
                        if (url.equals("https://www.facebook.com/zipy.co.il")) {
                            startActivity(intent);
                        }
                        return false;
                    }

                    // Otherwise, the link is not for a page on my site, so launch
                    // another Activity that handles URLs
                    startActivity(intent);
                    return false;
                } else if (url.startsWith("tel:")) {
                    Intent tel = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
                    startActivity(tel);
                    return true;
                } else if (url.startsWith("mailto:")) {
                    Intent mail = new Intent(Intent.ACTION_SEND);
                    mail.setType("application/octet-stream");
                    String AddressMail = url.replace("mailto:" , "") ;
                    mail.putExtra(Intent.EXTRA_EMAIL, new String[]{ AddressMail });
                    mail.putExtra(Intent.EXTRA_SUBJECT, "");
                    mail.putExtra(Intent.EXTRA_TEXT, "");
                    startActivity(mail);
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
        public boolean onCreateWindow(WebView view, boolean isDialog,
                                      boolean isUserGesture, Message resultMsg) {
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

    private String addParamsToURL(String url) {

        if (url.contains("utm_medium=app&utm_source=app_android") || url.contains("utm_medium%3Dapp%26utm_source%3Dapp_android") || !url.contains("zipy.co.il")) {
            return url;
        }

        if (url.contains("#")) {
            String[] urlArray = url.split("#");
            String params = url.contains("?") ? "&utm_medium=app&utm_source=app_android" : "?utm_medium=app&utm_source=app_android";
            return urlArray[0] + params + "#" + urlArray[1];
        } else {
            return url += url.contains("?") ? "&utm_medium=app&utm_source=app_android" : "?utm_medium=app&utm_source=app_android";
        }
    }

    // Google auth
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }
    // Google auth
    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            String authToken = account.getServerAuthCode();
            // Signed in successfully, show authenticated UI.
            getAccessToken(authToken);
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w("Error", "signInResult:failed code=" + e.getStatusCode());
        }
    }
    // Google auth
    public void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    public class JavaScriptInterface {
        String token;

        @JavascriptInterface
        public void googleAuth() {
            signIn();
        }

        @JavascriptInterface
        public String getToken() {
            return token;
        }
    }

    public void getAccessToken(String authCode) {
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new FormEncodingBuilder()
                .add("grant_type", "authorization_code")
                .add("client_id", client_id)
                .add("client_secret", client_secret)
                .add("code", authCode)
                .build();
        final Request request = new Request.Builder()
                .url("https://www.googleapis.com/oauth2/v4/token")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .post(requestBody)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {

            }
            @Override
            public void onResponse(Response response) throws IOException {
                try {
                    JSONObject jsonObject = new JSONObject(response.body().string());
                    mAccessToken = jsonObject.get("access_token").toString();
                    activity.runOnUiThread(new Runnable() {
                        public void run() {
                            String jsAuth = "javascript:googleAuthByToken('" + mAccessToken + "')";
                            webView.loadUrl(jsAuth);
                            Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                public void run() {
                                    webView.loadUrl(home_page_url);
                                }
                            }, 1000);
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void onOSSubscriptionChanged(OSSubscriptionStateChanges stateChanges) {
        if (!stateChanges.getFrom().getSubscribed() && stateChanges.getTo().getSubscribed()) {
            javaScriptInterface.token = stateChanges.getTo().getUserId();
        }
    }

}
