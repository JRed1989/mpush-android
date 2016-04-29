package com.shinemo.mpush.android;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.SystemClock;

import com.shinemo.mpush.api.Constants;

import static android.net.ConnectivityManager.CONNECTIVITY_ACTION;

/**
 * Created by yxx on 2016/2/14.
 *
 * @author ohun@live.cn
 */
public final class MPushReceiver extends BroadcastReceiver {
    public static final String ACTION_HEALTH_CHECK = "com.shinemo.mpush.HEALTH_CHECK";
    public static final String ACTION_NOTIFY_CANCEL = "com.shinemo.mpush.NOTIFY_CANCEL";
    public static int delay = Constants.DEF_HEARTBEAT;
    public static State STATE = State.UNKNOWN;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_HEALTH_CHECK.equals(action)) {//处理心跳
            if (MPush.I.hasStarted()) {
                if (MPush.I.client.isRunning()) {
                    if (MPush.I.client.healthCheck()) {
                        startAlarm(context, delay);
                    }
                }
            }
        } else if (CONNECTIVITY_ACTION.equals(action)) {//处理网络变化
            if (hasNetwork(context)) {
                if (STATE != State.CONNECTED) {
                    STATE = State.CONNECTED;
                    if (MPush.I.hasStarted()) {
                        MPush.I.resumePush();
                    } else {
                        MPush.I.checkInit(context).startPush();
                    }
                }
            } else {
                if (STATE != State.DISCONNECTED) {
                    STATE = State.DISCONNECTED;
                    MPush.I.pausePush();
                    cancelAlarm(context);//防止特殊场景下alarm没被取消
                }
            }
        } else if (ACTION_NOTIFY_CANCEL.equals(action)) {//处理通知取消
            Notifications.I.clean(intent);
        }
    }

    static void startAlarm(Context context, int delay) {
        Intent it = new Intent(MPushReceiver.ACTION_HEALTH_CHECK);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, it, 0);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + delay, pi);
        MPushReceiver.delay = delay;
    }

    static void cancelAlarm(Context context) {
        Intent it = new Intent(MPushReceiver.ACTION_HEALTH_CHECK);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, it, 0);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pi);
    }

    public static boolean hasNetwork(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return (info != null && info.isConnected());
    }
}
