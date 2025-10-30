package com.example.alertaciudadana.Controller;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class NotificationController {
    private static final String TAG = "NotificationController";
    public static final String PREFS = NotificationReceiver.PREFS;
    private static final String PREF_INTERVAL = "notif_interval";
    private static final String PREF_TITLE = "notif_title";
    private static final String PREF_BODY = "notif_body";
    private static final String PREF_TARGET = "notif_target";
    private static final String PREF_ENABLED = "notif_enabled";
    private static final int REQUEST_CODE_BASE = 0x1000;

    /**
     * Compat: mantiene la firma previa.
     */
    public static void scheduleOneShot(Context ctx, long delayMs, String title) {
        scheduleOneShot(ctx, delayMs, title, "", null);
    }

    /**
     * Nueva: programa una notificación única con title, body y target (target puede ser el nombre "FirstNewActivity"
     * u otro identificador que tu app interprete).
     */
    public static void scheduleOneShot(Context ctx, long delayMs, String title, String body, String target) {
        if (ctx == null) return;
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        long when = System.currentTimeMillis() + delayMs;

        Intent i = new Intent(ctx, NotificationReceiver.class);
        i.putExtra(NotificationReceiver.EXTRA_TITLE, title);
        i.putExtra(NotificationReceiver.EXTRA_BODY, body);
        i.putExtra(NotificationReceiver.EXTRA_TARGET, target);

        int req = REQUEST_CODE_BASE + 1;
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getBroadcast(ctx, req, i, flags);

        try {
            // Intentamos exact only si está permitido (API 31+ permite comprobar)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (am.canScheduleExactAlarms()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
                    } else {
                        am.setExact(AlarmManager.RTC_WAKEUP, when, pi);
                    }
                    Log.d(TAG, "OneShot exact scheduled at " + when + " title=" + title);
                } else {
                    am.set(AlarmManager.RTC_WAKEUP, when, pi);
                    Log.w(TAG, "No exact alarm permission. Scheduled inexact at " + when);
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    am.setExact(AlarmManager.RTC_WAKEUP, when, pi);
                } else {
                    am.set(AlarmManager.RTC_WAKEUP, when, pi);
                }
                Log.d(TAG, "OneShot scheduled at " + when + " title=" + title);
            }
        } catch (SecurityException se) {
            Log.e(TAG, "SecurityException scheduling one-shot alarm: " + se.getMessage(), se);
            try {
                am.set(AlarmManager.RTC_WAKEUP, when, pi);
                Log.w(TAG, "Fallback: scheduled inexact alarm after SecurityException");
            } catch (Exception e) {
                Log.e(TAG, "Fallback failed: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception scheduling one-shot alarm: " + e.getMessage(), e);
        }
    }

    /**
     * Repeating: añadimos guardado de body/target en prefs para reprogramar en BOOT.
     */
    public static void scheduleRepeating(Context ctx, long intervalMs, String title, String body, String target) {
        if (ctx == null) return;
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        long first = System.currentTimeMillis() + intervalMs;

        Intent i = new Intent(ctx, NotificationReceiver.class);
        i.putExtra(NotificationReceiver.EXTRA_TITLE, title);
        i.putExtra(NotificationReceiver.EXTRA_BODY, body);
        i.putExtra(NotificationReceiver.EXTRA_TARGET, target);

        int req = REQUEST_CODE_BASE;
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getBroadcast(ctx, req, i, flags);

        // Guardar prefs para reprogramar en BOOT
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putLong(PREF_INTERVAL, intervalMs)
                .putString(PREF_TITLE, title)
                .putString(PREF_BODY, body)
                .putString(PREF_TARGET, target)
                .putBoolean(PREF_ENABLED, true)
                .apply();

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                am.setInexactRepeating(AlarmManager.RTC_WAKEUP, first, intervalMs, pi);
            } else {
                am.setRepeating(AlarmManager.RTC_WAKEUP, first, intervalMs, pi);
            }
            Log.d(TAG, "Alarm set repeating first=" + first + " every=" + intervalMs);
        } catch (SecurityException se) {
            Log.e(TAG, "SecurityException scheduling repeating alarm: " + se.getMessage(), se);
        } catch (Exception e) {
            Log.e(TAG, "Exception scheduling repeating alarm: " + e.getMessage(), e);
        }
    }

    /**
     * Compat: mantener firma previa (title only) para repeating.
     */
    public static void scheduleRepeating(Context ctx, long intervalMs, String title) {
        scheduleRepeating(ctx, intervalMs, title, "", null);
    }

    /**
     * Cancela programadas (sin cambios funcionales).
     */
    public static void cancelScheduled(Context ctx) {
        if (ctx == null) return;
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent i = new Intent(ctx, NotificationReceiver.class);
        int req = REQUEST_CODE_BASE;
        int flags = PendingIntent.FLAG_NO_CREATE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getBroadcast(ctx, req, i, flags);
        if (pi != null) {
            try {
                am.cancel(pi);
                pi.cancel();
                Log.d(TAG, "Scheduled alarm cancelled");
            } catch (Exception e) {
                Log.e(TAG, "Error cancelling alarm: " + e.getMessage(), e);
            }
        }

        // borrar prefs
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .remove(PREF_INTERVAL)
                .remove(PREF_TITLE)
                .remove(PREF_BODY)
                .remove(PREF_TARGET)
                .putBoolean(PREF_ENABLED, false)
                .apply();
    }
}