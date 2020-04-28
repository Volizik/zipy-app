package com.app.zipy;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

public class FirebaseMessageReceiver extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (remoteMessage.getData().size() > 0) {
            sendNotification(remoteMessage.getData());
        }
    }

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    @Override
    public void onNewToken(String token) {

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        sendRegistrationToServer(token);
    }

    /**
     * Persist token to third-party servers.
     *
     * Modify this method to associate the user's FCM InstanceID token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token) {
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new FormEncodingBuilder()
                .add("grant_type", "authorization_code")
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
//                    mAccessToken = jsonObject.get("access_token").toString();
//                    activity.runOnUiThread(new Runnable() {
//                        public void run() {
//                            String jsAuth = "javascript:googleAuthByToken('" + mAccessToken + "')";
//                            webView.loadUrl(jsAuth);
//                            Handler handler = new Handler();
//                            handler.postDelayed(new Runnable() {
//                                public void run() {
//                                    webView.loadUrl(home_page_url);
//                                }
//                            }, 1000);
//                        }
//                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void sendNotification(Map<String, String> data) {
        String messageTitle = data.get("title");
        String messageBody = data.get("message");
        String url = data.get("url");

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url), this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("1", name, importance);
            channel.setDescription(description);
            channel.setShowBadge(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, "1")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(messageTitle)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_SOUND | NotificationCompat.DEFAULT_VIBRATE)
                .setPriority(Notification.PRIORITY_MAX);

        Notification buildNotification = mBuilder.build();
        NotificationManager mNotifyMgr = (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.notify(1, buildNotification);
    }

}
