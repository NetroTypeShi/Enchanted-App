package com.example.alertaciudadana.Controller;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.alertaciudadana.R;
import com.example.alertaciudadana.Views.FirstNewActivity;
import com.example.alertaciudadana.Views.NewsPanelActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationReceiver";
    public static final String CHANNEL_ID = "news_broadcast_channel";
    // Misma prefs que NewsPanelActivity
    public static final String PREFS = "news_prefs";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_BODY = "body";
    public static final String EXTRA_TARGET = "target";
    public static final String PREF_PENDING_TITLE = "pending_title";
    public static final String PREF_PENDING_BODY = "pending_body";
    public static final String PREF_PENDING_TARGET = "pending_target";
    // clave para lista persistente
    public static final String PREF_NEWS_LIST = "news_list";

    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent != null ? intent.getStringExtra(EXTRA_TITLE) : null;
        String body = intent != null ? intent.getStringExtra(EXTRA_BODY) : null;
        String target = intent != null ? intent.getStringExtra(EXTRA_TARGET) : null;

        if (title == null) title = "Nueva notificación";
        if (body == null) body = "";

        createChannelIfNeeded(context);

        // En Android 13+ necesitamos POST_NOTIFICATIONS runtime
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "POST_NOTIFICATIONS no concedido, guardando pending en prefs");
                SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
                prefs.edit()
                        .putString(PREF_PENDING_TITLE, title)
                        .putString(PREF_PENDING_BODY, body)
                        .putString(PREF_PENDING_TARGET, target)
                        .apply();
                // también añadimos a la lista persistente para que se recupere al abrir la app
                appendNewsToStorage(context, title, body, target);
                return;
            }
        }

        // PendingIntent para abrir la Activity al tocar la notificación o el botón "Detalles"
        Intent openIntent;
        if ("FirstNewActivity".equals(target)) {
            openIntent = new Intent(context, FirstNewActivity.class);
        } else {
            // por defecto abrimos NewsPanelActivity y pasamos título/body para que se muestre si hace falta
            openIntent = new Intent(context, NewsPanelActivity.class);
            openIntent.putExtra(EXTRA_TITLE, title);
            openIntent.putExtra(EXTRA_BODY, body);
        }
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int flags = android.app.PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= android.app.PendingIntent.FLAG_IMMUTABLE;
        android.app.PendingIntent pi = android.app.PendingIntent.getActivity(context, (int) (System.currentTimeMillis() % Integer.MAX_VALUE), openIntent, flags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle(title)
                .setContentText(body != null && !body.isEmpty() ? body : "")
                .setAutoCancel(true)
                .setContentIntent(pi)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        // Añadir acción "Detalles" (abre la misma pendingIntent)
        builder.addAction(new NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_info_details,
                "Detalles",
                pi).build());

        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        nm.notify((int) (System.currentTimeMillis() % Integer.MAX_VALUE), builder.build());

        // Guardar título/body/target en prefs para que NewsPanelActivity lo añada a la UI si la app está cerrada
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(PREF_PENDING_TITLE, title)
                .putString(PREF_PENDING_BODY, body)
                .putString(PREF_PENDING_TARGET, target)
                .apply();

        // Añadir también a la lista persistente (news_list)
        appendNewsToStorage(context, title, body, target);

        Log.d(TAG, "Notificación enviada: " + title + " (body present? " + (body != null && !body.isEmpty()) + ") target=" + target);
    }

    private void createChannelIfNeeded(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Noticias programadas",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Canal para notificaciones programadas por la app");
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    /**
     * Helper público: añade un item {title,body,target} al JSONArray almacenado en prefs bajo PREF_NEWS_LIST.
     * Silencioso (try/catch) para no romper receivers/actividades.
     */
    public static void appendNewsToStorage(Context ctx, String title, String body, String target) {
        if (ctx == null) return;
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String current = prefs.getString(PREF_NEWS_LIST, "[]");
        try {
            JSONArray arr = new JSONArray(current);
            JSONObject o = new JSONObject();
            o.put("title", title != null ? title : "");
            o.put("body", body != null ? body : "");
            o.put("target", target != null ? target : "");
            arr.put(o);
            prefs.edit().putString(PREF_NEWS_LIST, arr.toString()).apply();
            Log.d(TAG, "appendNewsToStorage: added title=" + title);
        } catch (JSONException je) {
            Log.e(TAG, "appendNewsToStorage JSON error: " + je.getMessage(), je);
            // fallback: overwrite with single element
            try {
                JSONArray arr = new JSONArray();
                JSONObject o = new JSONObject();
                o.put("title", title != null ? title : "");
                o.put("body", body != null ? body : "");
                o.put("target", target != null ? target : "");
                arr.put(o);
                prefs.edit().putString(PREF_NEWS_LIST, arr.toString()).apply();
            } catch (JSONException ignored) { }
        } catch (Exception e) {
            Log.e(TAG, "appendNewsToStorage error: " + e.getMessage(), e);
        }
    }
}
