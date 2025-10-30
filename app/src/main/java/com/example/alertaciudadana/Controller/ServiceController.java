package com.example.alertaciudadana.Controller;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.content.ContextCompat;

/**
 * Helper para programar/cancelar notifications sin usar VigilanciaService.
 */
public final class ServiceController {

    private static final String TAG = "ServiceController";

    private ServiceController() { /* no instances */ }

    /**
     * Arranca la programación de notificaciones (repetidas) desde una Activity.
     * intervalMs: tiempo entre notificaciones en ms
     * titlePrefix: título de la notificación
     */
    public static void startFromActivity(Activity activity, long intervalMs, String titlePrefix) {
        if (activity == null) return;
        // Comprobar permiso POST_NOTIFICATIONS en Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            int perm = ContextCompat.checkSelfPermission(activity, android.Manifest.permission.POST_NOTIFICATIONS);
            if (perm != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "POST_NOTIFICATIONS no concedido. Pide permiso en la Activity antes de programar.");
                // Podemos seguir y programar, NotificationReceiver guardará en prefs si falta permiso.
            }
        }
        // Programar alarmas
        NotificationController.scheduleRepeating(activity.getApplicationContext(), intervalMs, titlePrefix);
        Log.d(TAG, "NotificationController.scheduleRepeating solicitado (intervalMs=" + intervalMs + ", prefix=" + titlePrefix + ")");
    }

    /**
     * Detiene la programación.
     */
    public static void stop(Context context) {
        if (context == null) return;
        NotificationController.cancelScheduled(context.getApplicationContext());
        Log.d(TAG, "NotificationController.cancelScheduled solicitado");
    }
}
