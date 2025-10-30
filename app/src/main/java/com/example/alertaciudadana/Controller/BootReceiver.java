package com.example.alertaciudadana.Controller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Reprograma notificaciones tras reinicio si estaban activas.
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)) {
            SharedPreferences prefs = context.getSharedPreferences(NotificationReceiver.PREFS, Context.MODE_PRIVATE);
            boolean enabled = prefs.getBoolean("notif_enabled", false);
            if (enabled) {
                long interval = prefs.getLong("notif_interval", -1);
                String title = prefs.getString("notif_title", "Nueva noticia");
                if (interval > 0) {
                    NotificationController.scheduleRepeating(context, interval, title);
                    Log.d(TAG, "Reprogramada notificación tras BOOT: interval=" + interval + " title=" + title);
                } else {
                    Log.w(TAG, "BootReceiver: interval inválido, no se reprograma");
                }
            } else {
                Log.d(TAG, "BootReceiver: notificaciones no estaban activadas previamente");
            }
        }
    }
}
