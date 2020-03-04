package com.example.zipy;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.widget.Toast;

public class ThirdPartyApp {
    static void intentMessageTelegram(Context context) {
        final String appName = "org.telegram.messenger";
        final boolean isAppInstalled = isAppAvailable(context, appName);
        if (isAppInstalled) {
            Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=zipyofficial"));
            context.startActivity(myIntent);
        }
        else {
            Toast.makeText(context, "Telegram not Installed", Toast.LENGTH_SHORT).show();
        }
    }

    public static boolean isAppAvailable(Context context, String appName) {
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
