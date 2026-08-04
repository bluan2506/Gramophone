package org.akanework.gramophone.logic.utils.firebase;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.List;

/**
 * Created by Quang Phúc on 15/10/24.
 */
public class FirebaseEventUtils {

    private static final String TAG = "MyAppFirebaseEventUtils";

    private static FirebaseEventUtils mFirebaseEventUtils;

    public static FirebaseEventUtils getInstances() {
        if (mFirebaseEventUtils == null) {
            mFirebaseEventUtils = new FirebaseEventUtils();
        }
        return mFirebaseEventUtils;
    }

    public void logEvent(Context context, String eventName) {
        logEvent(context, eventName, new Bundle());
    }

    public void logEvent(Context context, String eventName, Bundle bundle) {
        try {
            Log.i(TAG, eventName);
            for (String key : bundle.keySet()) {
                Log.i(TAG, key + " = " + bundle.get(key));
            }

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

            String device_id = context.getSharedPreferences("device_id", Context.MODE_PRIVATE).getString("device_id", "");
            String appSetId = preferences.getString(context.getPackageName() + "AppSetId", "");
            String referrerUrl = preferences.getString(context.getPackageName() + "referrerUrl", "");

            Bundle newBundle = new Bundle();
            newBundle.putString(FirebaseAnalytics.Param.ITEM_ID, eventName);
            newBundle.putString(FirebaseAnalytics.Param.ITEM_NAME, eventName);
            newBundle.putString("device_id", device_id);
            newBundle.putString("AppSetId", appSetId);
            newBundle.putString("referrerUrl", referrerUrl);

            for (String key : bundle.keySet()) {
                Object value = bundle.get(key);
                if (value instanceof String)
                    newBundle.putString(key, (String) value);
                else if (value instanceof Integer)
                    newBundle.putInt(key, (Integer) value);
                else if (value instanceof Long)
                    newBundle.putLong(key, (Long) value);
                else if (value instanceof List)
                    newBundle.putStringArrayList(key, bundle.getStringArrayList(key));
            }

            FirebaseAnalytics.getInstance(context).logEvent(eventName, newBundle);
        } catch (Exception e) {
            recordException(e);
        }
    }

    public void logEventUserClickRateApp(Context context) {
        Bundle bundle = new Bundle();

        logEvent(context, Keys.USER_CLICK_RATE_APP, bundle);
    }

    public void logEventUserShareApp(Context context) {
        Bundle bundle = new Bundle();

        logEvent(context, Keys.USER_SHARE_APP, bundle);
    }

    public void logEventUserGotoEqualizer(Context context) {
        Bundle bundle = new Bundle();

        logEvent(context, Keys.USER_GOTO_EQUALIZER, bundle);
    }

    public void logEventUserConnectToAndroidAuto(Context context) {
        Bundle bundle = new Bundle();

        logEvent(context, Keys.USER_CONNECT_TO_ANDROID_AUTO_2, bundle);
    }

    public void logEventDownloadMusicRestricted(Context context) {
        Bundle bundle = new Bundle();

        logEvent(context, Keys.DOWNLOAD_MUSIC_RESTRICTED, bundle);
    }

    public void logEventGetConfigSuccessAndUseAdmob(Context context) {
        Bundle bundle = new Bundle();

        logEvent(context, Keys.GET_CONFIG_SUCCESS_AND_USE_ADMOB, bundle);
    }

    public void logEventGetConfigSuccessAndUseTopOn(Context context) {
        Bundle bundle = new Bundle();

        logEvent(context, Keys.GET_CONFIG_SUCCESS_AND_USE_TOP_ON, bundle);
    }

    public void logEventGetConfigFailed(Context context) {
        Bundle bundle = new Bundle();

        logEvent(context, Keys.GET_CONFIG_FAILED, bundle);
    }

    public void logEventShowDialogTurnOffVpnOrProxy(Context context) {
        Bundle bundle = new Bundle();

        logEvent(context, Keys.SHOW_DIALOG_TURN_OFF_VPN_OR_PROXY, bundle);
    }

    public void recordException(Throwable t) {
        Log.d(TAG, "recordException");
        t.printStackTrace();
        try {
            FirebaseCrashlytics.getInstance().recordException(t);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
