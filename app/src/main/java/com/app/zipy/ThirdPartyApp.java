package com.app.zipy;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.widget.Toast;

public class ThirdPartyApp {
    static void intentMessageTelegram(Activity activity) {
        final boolean isAppInstalled = isAppAvailable(activity.getApplicationContext());
        if (isAppInstalled) {
            Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=zipyofficial"));
            activity.startActivity(myIntent);
        }
        else {
            Toast.makeText(activity.getApplicationContext(), "Telegram not Installed", Toast.LENGTH_SHORT).show();
        }
    }

    private static boolean isAppAvailable(Context context) {
        final String appName = "org.telegram.messenger";
        PackageManager pm = context.getPackageManager();

        try {
            pm.getPackageInfo(appName, PackageManager.GET_ACTIVITIES);
            return true;
        }
        catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
