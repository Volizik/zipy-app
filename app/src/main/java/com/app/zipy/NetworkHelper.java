package com.app.zipy;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class NetworkHelper {
    private static final String TAG = "NetworkHelper";
    private Context context;
    private Activity activity;

    public NetworkHelper(Activity a) {
        activity = a;
        context = a.getApplicationContext();
    }

    public boolean isOnline() {
        ConnectivityManager conMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        assert conMgr != null;
        NetworkInfo netInfo = conMgr.getActiveNetworkInfo();

        if(netInfo == null || !netInfo.isConnected() || !netInfo.isAvailable()){
            showOnlineAlert();
            return false;
        }
        return true;
    }

    public void showOnlineAlert() {
        try {
            AlertDialog alert = new AlertDialog.Builder(activity, R.style.AlertDialogCustom).create();
            alert.setTitle("שגיאה");
            alert.setMessage("אינטרנט לא זמין. יש לבדוק שהמכשיר מחובר לרשת ולנסות שוב");
            alert.setButton(Dialog.BUTTON_POSITIVE,"אוקיי",new DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    activity.finish();
                }
            });

            alert.show();
        } catch (Exception e) {
            Log.d(TAG, "Show Dialog: " + e.getMessage());
        }
    }

}
