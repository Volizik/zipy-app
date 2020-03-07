package com.example.zipy;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
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
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class MainActivity extends Activity {

    public WebView webView;
    private WebView mWebviewPop;
    private FrameLayout mContainer;

    private static final String TAG = "MyActivity";
    GoogleSignInClient mGoogleSignInClient;
    int RC_SIGN_IN = 0;
    private String mAccessToken;
    Activity activity;
    Context context;
    private final String client_id = "536995109378-qm8nap6j2i0i5a3ma3ete5kag1dd2qlb.apps.googleusercontent.com";
    private final String client_secret = "pYQmS3qeUMegVRQMuz-rjPX7";
    private final String home_page_url = "https://www.zipy.co.il/";
    private String home_page_url_prefix = "zipy.co.il";
    private String saved_url = home_page_url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        activity = this;
        context = this.getApplicationContext();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestServerAuthCode(client_id)
                .requestScopes(new Scope("https://www.googleapis.com/auth/userinfo.email"))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        if (isOnline(getApplicationContext())) {
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

            if (Build.VERSION.SDK_INT >= 21) {
                webSettings.setMixedContentMode( WebSettings.MIXED_CONTENT_ALWAYS_ALLOW );
            }

            webView.addJavascriptInterface(new JavaScriptInterface(this), "android");

            webView.setWebViewClient(new CustomWebViewClient());
            webView.setWebChromeClient(new UriWebChromeClient());

            webView.loadUrl(home_page_url);

        } else {
            showAlert();
        }

    }

    @Override
    public void onBackPressed() {

        if (webView.canGoBack()) {
            if (mWebviewPop != null) {
                mWebviewPop.setVisibility(View.GONE);
                mContainer.removeView(mWebviewPop);
                mWebviewPop = null;
            }
            webView.goBack();
        } else {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("Exit!")
                    .setMessage("Are you sure you want to close?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }

                    })
                    .setNegativeButton("No", null)
                    .show();
        }
    }

    public boolean isOnline(Context context) {
        ConnectivityManager conMgr = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        assert conMgr != null;
        NetworkInfo netInfo = conMgr.getActiveNetworkInfo();

        if(netInfo == null || !netInfo.isConnected() || !netInfo.isAvailable()){
            Toast.makeText(context, "No Internet connection!", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private class CustomWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            String host = Uri.parse(url).getHost();
            if (isOnline(getApplicationContext())) {

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
                    return true;
                } else if (url.startsWith("tg:resolve")) {
                    view.loadUrl(home_page_url);
                    return true;
                }
                return false;
            } else {
                showAlert();
                return true;
            }
        }

        public void onPageFinished(WebView view, String url) {
            webView.loadUrl("javascript:document.documentElement.classList.add('android-app')");
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

    private void showAlert() {
        try {
            AlertDialog alert = new AlertDialog.Builder(this).create();
            alert.setTitle("Error");
            alert.setMessage("Internet not available, Cross check your internet connectivity and try again");
            alert.setButton(Dialog.BUTTON_POSITIVE,"OK",new DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });

            alert.show();
        } catch (Exception e) {
            Log.d(TAG, "Show Dialog: " + e.getMessage());
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
        Context mContext;

        JavaScriptInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public void googleAuth() {
            signIn();
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
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "MainActivity: onStart()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "MainActivity: onResume()" + saved_url);
        webView.loadUrl(saved_url);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "MainActivity: onPause()" + saved_url);
        saved_url = webView.getUrl();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "MainActivity: onStop()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MainActivity: onDestroy()");
    }
}
