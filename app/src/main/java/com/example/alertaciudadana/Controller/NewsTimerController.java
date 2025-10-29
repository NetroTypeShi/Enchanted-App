package com.example.alertaciudadana.Controller;

import android.os.Handler;
import android.os.Looper;

import java.lang.ref.WeakReference;

/**
 * Temporizador simple y ligero que ejecuta un único tick en el hilo principal (UI).
 * - startOneShot(delayMs) ejecuta listener.onTick() después de delayMs ms.
 * - stop() cancela la ejecución si aún no se había ejecutado.
 *
 * Usa WeakReference para no retener la Activity si esta se destruye.
 */
public class NewsTimerController {
    public interface Listener {
        void onTick();
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final WeakReference<Listener> listenerRef;
    private Runnable runnable;

    public NewsTimerController(Listener listener) {
        this.listenerRef = new WeakReference<>(listener);
    }

    /**
     * Ejecuta listener.onTick() una sola vez después de delayMs milisegundos.
     */
    public void startOneShot(long delayMs) {
        stop();
        runnable = new Runnable() {
            @Override
            public void run() {
                Listener l = listenerRef.get();
                if (l != null) {
                    l.onTick();
                }
            }
        };
        handler.postDelayed(runnable, delayMs);
    }

    /**
     * Cancela la ejecución programada si existe.
     */
    public void stop() {
        if (runnable != null) {
            handler.removeCallbacks(runnable);
            runnable = null;
        }
    }
}