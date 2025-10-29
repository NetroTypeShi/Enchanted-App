package com.example.alertaciudadana.Views;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.alertaciudadana.R;
import com.example.alertaciudadana.Controller.NewsTimerController;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Cambios principales:
 * - Solo añadimos la tarjeta inicial y programamos el timer si savedInstanceState == null.
 * - Guardamos/restauramos el anchor en onSaveInstanceState para evitar que al recrear la Activity
 *   se pierda la referencia a la última tarjeta añadida.
 * - onResume comprueba SharedPreferences para cualquier noticia pendiente (si usas Service/Worker).
 */
public class NewsPanelActivity extends AppCompatActivity {

    private static final String TAG = "NewsPanelActivity";
    private static final String KEY_ANCHOR = "key_anchor";

    // Controller externo para programar la aparición de la segunda noticia
    private NewsTimerController newsTimerController;

    // Anchor shared to chain added cards (starts pointing to textView3)
    private final AtomicInteger anchor = new AtomicInteger(R.id.textView3);

    private static final String CHANNEL_ID = "news_channel_id";
    private static final int REQ_POST_NOTIF = 1001;

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Noticias",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Canal para notificaciones de noticias");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel creado: " + CHANNEL_ID);
            } else {
                Log.w(TAG, "NotificationManager es null, no se puede crear channel");
            }
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Solicitando permiso POST_NOTIFICATIONS");
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        REQ_POST_NOTIF);
            } else {
                Log.d(TAG, "Permiso POST_NOTIFICATIONS ya concedido");
            }
        }
    }

    private void showNotificationWithTitle(String title) {
        Log.d(TAG, "showNotificationWithTitle: " + title);

        // permiso (si no está concedido no intentamos notificar)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "No se ha concedido POST_NOTIFICATIONS -> no se muestra notificación");
                return;
            }
        }

        int smallIcon = R.drawable.logo; // poner la imagen (tu drawable)

        NotificationCompat.Builder builder = new NotificationCompat.Builder(NewsPanelActivity.this, CHANNEL_ID)
                .setSmallIcon(smallIcon)
                .setContentTitle(title)
                .setContentText("") // vacío porque solo quieres el título
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManagerCompat.from(NewsPanelActivity.this)
                .notify((int) (System.currentTimeMillis() % Integer.MAX_VALUE), builder.build());

        Log.d(TAG, "Notificación enviada: " + title);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_news_panel);

        // Restaurar anchor si la Activity se recreó
        if (savedInstanceState != null) {
            int savedAnchor = savedInstanceState.getInt(KEY_ANCHOR, R.id.textView3);
            anchor.set(savedAnchor);
            Log.d(TAG, "onCreate: restaurado anchor=" + savedAnchor);
        }

        // Crear canal y solicitar permiso (si es necesario)
        createNotificationChannel();
        requestNotificationPermissionIfNeeded();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ConstraintLayout main = findViewById(R.id.main);

        // Solo la primera vez (savedInstanceState == null) añadimos la tarjeta inicial y programamos el timer.
        // Si la Activity se recrea (p. ej. rotación) o volvemos desde otra Activity, evitamos duplicados.
        if (savedInstanceState == null) {
            // ancla inicial: debajo de textView3 (tal y como está en tu XML)
            anchor.set(R.id.textView3);

            // Primera tarjeta: actualizamos 'anchor' con el id del card creado
            int createdId = addNewsCard(main, anchor.get(),
                    "El ayuntamiento de Alfacar detecta sonidos y acontecimientos extraños en el bosque de Alfaguara",
                    "Los cazadores y senderistas han avistado sonidos de animales desconocidos en el bosque y proximidades, los testigos a pesar de buscar, no han encontrado la fuente de los sonidos",
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // acción del botón "Detalles" - abrimos FirstNewActivity
                            Intent intent = new Intent(NewsPanelActivity.this, FirstNewActivity.class);
                            startActivity(intent);
                        }
                    });
            anchor.set(createdId);

            // Inicializar el controller con un listener sencillo: al tick añadimos la segunda noticia
            newsTimerController = new NewsTimerController(new NewsTimerController.Listener() {
                @Override
                public void onTick() {
                    Log.d(TAG, "onTick() recibido en Activity");
                    runOnUiThread(() -> Toast.makeText(NewsPanelActivity.this, "onTick ejecutado (prueba)", Toast.LENGTH_SHORT).show());

                    int id2 = addNewsCard(main, anchor.get(),
                            "Mepicanlococo",
                            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    // acción del botón "Detalles" para la segunda noticia
                                }
                            });
                    anchor.set(id2);

                    // Mostrar notificación con solo el título
                    showNotificationWithTitle("Mepicanlococo");
                }
            });

            // Programar aparición única de la segunda noticia a los 5 segundos (prueba)
            newsTimerController.startOneShot(5_000L);
            Log.d(TAG, "Timer programado (5s)");

            newsTimerController = new NewsTimerController(new NewsTimerController.Listener() {
                @Override
                public void onTick() {
                    Log.d(TAG, "onTick() recibido en Activity");
                    runOnUiThread(() -> Toast.makeText(NewsPanelActivity.this, "onTick ejecutado (prueba)", Toast.LENGTH_SHORT).show());

                    int id2 = addNewsCard(main, anchor.get(),
                            "Noticia3",
                            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    // acción del botón "Detalles" para la segunda noticia
                                }
                            });
                    anchor.set(id2);

                    // Mostrar notificación con solo el título
                    showNotificationWithTitle("Nueva noticia");
                }
            });

            // Programar aparición única de la segunda noticia a los 5 segundos (prueba)
            newsTimerController.startOneShot(10_000L);
        } else {
            // Si savedInstanceState != null no reprogramamos timer; aseguramos que el controller no arranque doble.
            if (newsTimerController == null) {
                // mantener null: no iniciar timer automáticamente
                Log.d(TAG, "onCreate: savedInstanceState != null -> no se crea timer");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Si usas Service/Worker que dejó una noticia pendiente, añádela aquí.
        SharedPreferences prefs = getSharedPreferences("news_prefs", MODE_PRIVATE);
        String pendingTitle = prefs.getString("pending_title", null);
        if (pendingTitle != null) {
            prefs.edit().remove("pending_title").apply();
            ConstraintLayout main = findViewById(R.id.main);
            int id = addNewsCard(main, anchor.get(), pendingTitle, "", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // acción detalles
                }
            });
            anchor.set(id);
            Toast.makeText(this, "Se añadió noticia pendiente: " + pendingTitle, Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Se añadió noticia pendiente a la UI: " + pendingTitle);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_POST_NOTIF) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "POST_NOTIFICATIONS concedido");
                Toast.makeText(this, "Permiso de notificaciones concedido", Toast.LENGTH_SHORT).show();
            } else {
                Log.w(TAG, "POST_NOTIFICATIONS DENEGADO por el usuario");
                Toast.makeText(this, "Permiso de notificaciones denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Guardar el anchor actual (id del último card añadido) para restaurarlo si la Activity se recrea
        outState.putInt(KEY_ANCHOR, anchor.get());
        Log.d(TAG, "onSaveInstanceState: guardado anchor=" + anchor.get());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cancelar timer para evitar callbacks cuando la Activity ya no exista
        if (newsTimerController != null) {
            newsTimerController.stop();
            newsTimerController = null;
        }
    }

    //convertir dp a px
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    /**
     * Crea una tarjeta de noticia, la añade al ConstraintLayout 'main' y la ancla debajo de 'anchorViewId'.
     * Devuelve el id del CardView creado (útil para encadenar tarjetas sin solapamiento).
     */
    public int addNewsCard(ConstraintLayout main, int anchorViewId, String title, String body, View.OnClickListener listener) {
        // CardView
        CardView card = new CardView(this);
        int cardId = View.generateViewId();
        card.setId(cardId);
        card.setCardBackgroundColor(Color.WHITE);
        card.setRadius(dpToPx(14));
        card.setCardElevation(dpToPx(4));
        card.setUseCompatPadding(true);
        card.setPreventCornerOverlap(true);

        // Contenedor vertical dentro del Card
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        // Título (verde, negrita)
        TextView newsTitle = new TextView(this);
        newsTitle.setText(title);
        newsTitle.setTextColor(Color.parseColor("#017F39"));
        newsTitle.setTypeface(Typeface.DEFAULT_BOLD);
        newsTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        newsTitle.setLineSpacing(dpToPx(4), 1f);
        newsTitle.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
        newsTitle.setMaxWidth(dpToPx(360));

        // Cuerpo (negro, normal)
        TextView newsBody = new TextView(this);
        newsBody.setText(body);
        newsBody.setTextColor(Color.BLACK);
        newsBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        newsBody.setLineSpacing(dpToPx(4), 1f);
        newsBody.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
        LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        bodyLp.topMargin = dpToPx(8);

        // Botón "Detalles" (fondo verde pill)
        Button detailsBtn = new Button(this);
        detailsBtn.setText("Detalles");
        detailsBtn.setTextColor(Color.WHITE);
        detailsBtn.setAllCaps(false);
        detailsBtn.setOnClickListener(listener);

        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(Color.parseColor("#017F39"));
        btnBg.setCornerRadius(dpToPx(24));
        detailsBtn.setBackground(btnBg);

        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(48));
        btnLp.topMargin = dpToPx(16);

        // Añadimos vistas al container
        container.addView(newsTitle);
        container.addView(newsBody, bodyLp);
        container.addView(detailsBtn, btnLp);

        // Añadimos container al CardView
        card.addView(container, new CardView.LayoutParams(
                CardView.LayoutParams.MATCH_PARENT,
                CardView.LayoutParams.WRAP_CONTENT));

        // Añadir Card al ConstraintLayout principal
        ConstraintLayout.LayoutParams cardLp = new ConstraintLayout.LayoutParams(
                0, // MATCH_CONSTRAINT para respetar start/end
                ConstraintLayout.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(16));
        main.addView(card, cardLp);

        // Aplicar constraints: clonar primero y luego conectar usando anchorViewId
        ConstraintSet cs = new ConstraintSet();
        cs.clone(main);
        cs.connect(card.getId(), ConstraintSet.TOP, anchorViewId, ConstraintSet.BOTTOM, dpToPx(12));
        cs.connect(card.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, dpToPx(16));
        cs.connect(card.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, dpToPx(16));
        cs.applyTo(main);

        return cardId;
    }

}