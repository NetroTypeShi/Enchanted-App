package com.example.alertaciudadana.Views;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
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

public class NewsPanelActivity extends AppCompatActivity {

    private static final String TAG = "NewsPanelActivity";

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

        int smallIcon = R.drawable.logo; // poner la imagen

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

        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        final String [] cameraId = {null};
        final boolean[] encendido = {false};

        // Crear canal y solicitar permiso (si es necesario)
        createNotificationChannel();
        requestNotificationPermissionIfNeeded();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ConstraintLayout main = findViewById(R.id.main);

        // ancla inicial: debajo de textView3 (tal y como está en tu XML)
        anchor.set(R.id.textView3);

        // Primera tarjeta: actualizamos 'anchor' con el id del card creado
        int createdId = addNewsCard(main, anchor.get(),
                "El ayuntamiento de Alfacar detecta sonidos y acontecimientos extraños en el bosque de Alfaguara",
                "Los cazadores y senderistas han avistado sonidos de animales desconocidos en el bosque y proximidades, los testigos a pesar de buscar, no han encontrado la fuente de los sonidos",
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // acción del botón "Detalles"
                        Intent intent = new Intent(NewsPanelActivity.this, FirstNewActivity.class);
                        startActivity(intent);
                        if(!encendido[0]){
                            try{
                                cameraId[0] = cameraManager.getCameraIdList()[0];
                                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                                    cameraManager.setTorchMode(cameraId[0],true);
                                    encendido[0] = true;
                                    
                                }
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }
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
                        "Han avistado sombras supuestamente de personas deformes en el bosque de alfaguara",
                        "Las autoridades locales han emitido un comunicado instando a la calma tras los inquietantes reportes sobre las misteriosas sombras avistadas en el bosque de Alfaguara... ",
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(NewsPanelActivity.this, SecondNewActivity.class);
                                startActivity(intent);
                            }
                        });
                anchor.set(id2);
                if(encendido[0]){
                    try{
                        cameraId[0] = cameraManager.getCameraIdList()[0];
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                            cameraManager.setTorchMode(cameraId[0],false);
                            encendido[0] = false;

                        }
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                // Mostrar notificación con solo el título
                showNotificationWithTitle("Nuevo aviso del bosque de Alfaguara");
            }
        });

        // Programar aparición única de la segunda noticia a los 5 segundos (prueba)
        newsTimerController.startOneShot(10000L);

        newsTimerController = new NewsTimerController(new NewsTimerController.Listener() {
            @Override
            public void onTick() {
                Log.d(TAG, "onTick() recibido en Activity");
                runOnUiThread(() -> Toast.makeText(NewsPanelActivity.this, "onTick ejecutado (prueba)", Toast.LENGTH_SHORT).show());

                int id3 = addNewsCard(main, anchor.get(),
                        "El gobierno llama a la calma tras nuevos avistamientos en el bosque de Alfaguara",
                        "En las últimas horas, varios residentes de las zonas cercanas al bosque de Alfaguara aseguran haber visto figuras humanas deformes moviéndose entre los árboles al anochecer.",
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(NewsPanelActivity.this, ThirdNewActivity.class);
                                startActivity(intent);
                            }
                        });
                anchor.set(id3);

                // Mostrar notificación con solo el título
                showNotificationWithTitle("Nuevo aviso del bosque de Alfaguara");
            }
        });

        newsTimerController.startOneShot(20000L);

        newsTimerController = new NewsTimerController(new NewsTimerController.Listener() {
            @Override
            public void onTick() {
                Log.d(TAG, "onTick() recibido en Activity");
                runOnUiThread(() -> Toast.makeText(NewsPanelActivity.this, "onTick ejecutado (prueba)", Toast.LENGTH_SHORT).show());

                int id4 = addNewsCard(main, anchor.get(),
                        "El Gobierno Establece un Perímetro de Protección Sanitaria en Alfaguara",
                        "Ante la proliferación de información no verificada y con el objetivo primordial de proteger la salud y la seguridad pública, el Gobierno de la Nación ha decidido activar la Fase 3 de Contención Preventiva en la zona colindante al Bosque de Alfaguara.",
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(NewsPanelActivity.this, FourthNewActivity.class);
                                startActivity(intent);
                            }
                        });
                anchor.set(id4);

                // Mostrar notificación con solo el título
                showNotificationWithTitle("Nuevo aviso del bosque de Alfaguara");
            }
        });

        // Programar aparición única de la segunda noticia a los 5 segundos (prueba)
        newsTimerController.startOneShot(30000L);

        newsTimerController = new NewsTimerController(new NewsTimerController.Listener() {
            @Override
            public void onTick() {
                Log.d(TAG, "onTick() recibido en Activity");
                runOnUiThread(() -> Toast.makeText(NewsPanelActivity.this, "onTick ejecutado (prueba)", Toast.LENGTH_SHORT).show());

                int id5 = addNewsCard(main, anchor.get(),
                        "Gobierno Desmiente \"Bulos Absurdos\" y Anuncia la Llegada de \"Expertos Antivandalismo\" a Alfaguara",
                        "El Gobierno ha convocado una rueda de prensa de urgencia para hacer frente a la \"ola de desinformación sin precedentes\" que, según afirman, se está propagando mediante aplicaciones y redes sociales.",
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(NewsPanelActivity.this, FifthNewActivity.class);
                                startActivity(intent);
                            }
                        });
                anchor.set(id5);

                // Mostrar notificación con solo el título
                showNotificationWithTitle("Nuevo aviso del bosque de Alfaguara");
            }
        });

        // Programar aparición única de la segunda noticia a los 5 segundos (prueba)
        newsTimerController.startOneShot(40000L);

        newsTimerController = new NewsTimerController(new NewsTimerController.Listener() {
            @Override
            public void onTick() {
                Log.d(TAG, "onTick() recibido en Activity");
                runOnUiThread(() -> Toast.makeText(NewsPanelActivity.this, "onTick ejecutado (prueba)", Toast.LENGTH_SHORT).show());

                int id6 = addNewsCard(main, anchor.get(),
                        "Se Pierde el Rastro del \"Equipo Antivandalismo\" a las Pocas Horas de Entrar al Perímetro",
                        "El intento del gobierno de recuperar la autoridad ha resultado en un fracaso espeluznante. El contingente de \"Expertos de Seguridad y Antivandalismo\", desplegado hace apenas unas horas para \"restablecer la normalidad\", ha dejado de responder a las llamadas.",
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(NewsPanelActivity.this, SixthNewActivity.class);
                                startActivity(intent);
                            }
                        });
                anchor.set(id6);

                // Mostrar notificación con solo el título
                showNotificationWithTitle("Nuevo aviso del bosque de Alfaguara");
            }
        });

        // Programar aparición única de la segunda noticia a los 5 segundos (prueba)
        newsTimerController.startOneShot(50000L);

        newsTimerController = new NewsTimerController(new NewsTimerController.Listener() {
            @Override
            public void onTick() {
                Log.d(TAG, "onTick() recibido en Activity");
                runOnUiThread(() -> Toast.makeText(NewsPanelActivity.this, "onTick ejecutado (prueba)", Toast.LENGTH_SHORT).show());

                int id7 = addNewsCard(main, anchor.get(),
                        "Hallados los Restos Mutilados del \"Equipo Antivandalismo\" Cerca del Cordón",
                        "La esperanza oficial ha muerto de la forma más brutal. A pesar del hermetismo gubernamental, la verdad ha sido forzada por el descubrimiento: Los cuerpos del desaparecido \"Equipo de Seguridad y Antivandalismo\" han sido encontrados dispersos y horriblemente mutilados en el límite exterior del Perímetro Sanitario.",
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(NewsPanelActivity.this, SeventhNewActivity.class);
                                startActivity(intent);
                            }
                        });
                anchor.set(id7);

                // Mostrar notificación con solo el título
                showNotificationWithTitle("Nuevo aviso del bosque de Alfaguara");
            }
        });

        // Programar aparición única de la segunda noticia a los 5 segundos (prueba)
        newsTimerController.startOneShot(60000L);

        newsTimerController = new NewsTimerController(new NewsTimerController.Listener() {
            @Override
            public void onTick() {
                Log.d(TAG, "onTick() recibido en Activity");
                runOnUiThread(() -> Toast.makeText(NewsPanelActivity.this, "onTick ejecutado (prueba)", Toast.LENGTH_SHORT).show());

                int id8 = addNewsCard(main, anchor.get(),
                        "¡ÚLTIMA ALERTA OFICIAL! El Gobierno Rompe el Silencio y Ordena: \"No Salgan Bajo Ninguna Circunstancia\"",
                        "El muro de mentiras ha caído. Ante los macabros hallazgos y el colapso de sus fuerzas de contención, la Delegación de Gobierno ha emitido un comunicado de emergencia de carácter nacional que anula toda declaración previa.",
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(NewsPanelActivity.this, EighthNewActivity.class);
                                startActivity(intent);
                            }
                        });
                anchor.set(id8);

                // Mostrar notificación con solo el título
                showNotificationWithTitle("Nuevo aviso del bosque de Alfaguara");
            }
        });
        // Programar aparición única de la segunda noticia a los 5 segundos (prueba)
        newsTimerController.startOneShot(70000L);

        newsTimerController = new NewsTimerController(new NewsTimerController.Listener() {
            @Override
            public void onTick() {
                Log.d(TAG, "onTick() recibido en Activity");
                runOnUiThread(() -> Toast.makeText(NewsPanelActivity.this, "onTick ejecutado (prueba)", Toast.LENGTH_SHORT).show());

                int id9 = addNewsCard(main, anchor.get(),
                        "El Confinamiento Falla: Se Reportan Seres Colosales; El Terror Entra en los Hogares",
                        "La orden de \"no salir\" ha marcado el inicio de la pesadilla definitiva. Los pocos enlaces de comunicación aún activos confirman que la amenaza de Alfaguara no solo se ha extendido, sino que ha escalado en tamaño y brutalidad.",
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(NewsPanelActivity.this, NinthActivity.class);
                                startActivity(intent);
                            }
                        });
                anchor.set(id9);

                // Mostrar notificación con solo el título
                showNotificationWithTitle("Nuevo aviso del bosque de Alfaguara");
            }
        });
        // Programar aparición única de la segunda noticia a los 5 segundos (prueba)
        newsTimerController.startOneShot(80000L);

        newsTimerController = new NewsTimerController(new NewsTimerController.Listener() {
            @Override
            public void onTick() {
                Log.d(TAG, "onTick() recibido en Activity");
                runOnUiThread(() -> Toast.makeText(NewsPanelActivity.this, "onTick ejecutado (prueba)", Toast.LENGTH_SHORT).show());

                int id10 = addNewsCard(main, anchor.get(),
                        "Gobierno Declara el Colapso: \"No Hay Esperanza. Sobrevivan Como Puedan\"",
                        "La última luz de la autoridad se ha apagado. Ante la aniquilación de los equipos de contención y la extensión de las figuras grotescas a las áreas residenciales, el Gobierno ha emitido un comunicado final, breve y devastador.",
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(NewsPanelActivity.this, NinthActivity.class);
                                startActivity(intent);
                            }
                        });
                anchor.set(id10);

                // Mostrar notificación con solo el título
                showNotificationWithTitle("Nuevo aviso del bosque de Alfaguara");
            }
        });
        // Programar aparición única de la segunda noticia a los 5 segundos (prueba)
        newsTimerController.startOneShot(90000L);

        newsTimerController = new NewsTimerController(new NewsTimerController.Listener() {
            @Override
            public void onTick() {
                Log.d(TAG, "onTick() recibido en Activity");
                runOnUiThread(() -> Toast.makeText(NewsPanelActivity.this, "onTick ejecutado (prueba)", Toast.LENGTH_SHORT).show());

                int id11 = addNewsCard(main, anchor.get(),
                        "G̨͚̬̣̣̲̞̣͔̳͐̀ͧͬͤ̌̊̋͒̍͆̄̈̓̌͠͝ͅob̴͍̺̼̰̤̟͈̣̮͉̱͓̜̠̺̃̿͛̾͐ͩ̕͢͠͞ię̣̲̫̙͎͐̍ͥ̾͝_̶̼͎͂́̅̌ͩ̄ͯ͘͢r̞͔͒͘ņ̨̘͔̭̮̰̻̮̺̼͉̗̬̲ͮͪ́̌ͮ̓̌͊ͦͤ̐͒͒̔ͮ͒̈́̃ͭ͟͡͡ǫ̩̺̳̻̦̦̬̞̩͇̖͌͛ͫ̉̿͒̽͒͘͟͜ Ḑ̢͓͡͝ę̛̜̗̙̲̾͆ͤ͌̿͒̐̐̚_͎̰̲͕̙ͥ͌͆ͣ̅̈́̌̀̎ͯ́̕c̤̥̞̈̐̅́̚l̵̡͖͎̦͙̹͉̦͎̄́̈́ͦͪ͜ͅ_̨̬̄ͤͯ̾a̛̝̤̼̝̼͈̮͓̬̩̜̥̳̮ͧ̀̾̀ͯ̍ͧ̃̓ͬͬͦͭ̈́̉ͪ͋̋̄̚͘͟͢͡͡͡͡͡ͅr͎̣͔̥͔ͩ͋ͥ̄̒͠ḁ̴̶̢͈̹̓ͭ̽ͪͤͣ̾͠ ȩ̵̼̬͇͕ͮͧͦ͢_̷̧̙̱͍͈͚̞̗̌̈̐ͫ̈ͦ͆̚͟͡l̥̫͚̤̲̪̞̀͑͂ͪ̊͌̆͊̓͌̈̇̀̾̔̀̐̂ͣ͌̀̚͜ C̸̭̪͉̖͋͂ͪ͊̋̾_̷̴̴̨̟̰̪͚̦͚̔̅ͫ̌͂ͫ͠ọ̧̜̱̹̩͓̭̳̻̀̐̅ͩ̐̀̈̆ͫ͌ͤͨͨ̅͢͞͡l̶̡̛͎͍̬̗̹̖̬̦̝̲̹̼͍͎̮̿ͯ̈́ͯ̉ͧͮ̑̈̑̿ͣ́ͪ̕̚͡͠ͅa͉͍̹̝̼͖̎̿̒͛̌p̷̷̷̸̢̺̟̱̠̪͉̂̾̇͆ͩsö̢̢͎͉͓͓̭̞͔̹̝͓ͤͪͯ̃̅̄ͭͤ̊ͭͩ̿ͬ̂͂͊̂ͧ̈͘͘͜͟͝ͅ:̡͔̯̹͍͍̦̹͈̣̲ͩ͗̑̽́̏͂͑͜_̭͇̭͓͓̂ͥͣ̆̀ͥ̆͝ͅ \"̷̮̺̐ͣ̔͗_̸̸͇̯̲͚͖̀̉͐͗̽͑̈́̓ͅ_̵̷̢͚̟̙̳ͩ͊̾̓͟͝N̶̴̶̵̝̬͚̺̜̘̹̥̠̿͑͋ͩͯ͛́ͧ̓͑ͯ͗́̕͘͠ͅͅo̱̰͇͒̐ͨ̚̕͢ Ḩ̵̠͎̪͈̮̱̤̠̄̓̀̽̎̈́͛ͧͣ̂̏̇ͪ̿̕͝a̷̢̨͙̩̻̻͎̯̭͎̝͑̐ͦ̎͌͐̏͊͑̐̾̃̑͊̇̈́̇̀͛͐͋̕͘͜͢͢͡͝ͅͅy̴̢͔͎̲̰̟̞̘͇͚̞̳̮̯̠̰͋̿̔ͦ͛̾ͮ̅́͋ͭ̇̂͘͟͠͡ͅ E̸̸͔̺͚͚̥̺̤̟̳̫̗͚̮͖̎͒̓̋̇͗́̏̽̈̾̓͂ͮ͊ͮ̃ͪ̀̚͘͟͜͠͞͝s̷̵̶̛̛͎͎̹̮̫̩̣̼̻͓̯̗͉̞͈̿͌ͯ̓ͦͬ̒̂̂ͪ̍ͤͨ̆̅̈́̄̈̍ͦ̆̐̿͟͠p̴̢̗̺̺̫̣̙͇͉̲ͩ̓̿ͮ̈͛͊̇̾̉͂ͫͩͩ͊̈́ͪ̓̃͜͟͡e̡͖̝͖̗̼͎̩̭̦̅͆͗́͋ͭ̾́͘͡_̻͉͇̋̂̇̑ͅr̷̰̜̬͕͖̙̠͓̻̹͍̝ͪ̄̒͒̇̽̈́ͦ̇͗͗̔ͩ̾̉̀ͯ̚͜͡͞ȃ̴̢̡͕͓̮̹̮͙̲̳̝̮̤̘͔̱͓̖͙̝̬ͥͧ̃̍ͣ́ͮ̉̅͐̌̆͒̽̉̂̃́̊͊͌͟͡ņ̷̵̶̛̞̣̠̦̪̗̦̞̜͇̪̬̯͑ͦ́̈͂̏͌ͭͮ̓ͪ̓̄̓ͥ͌̓̅̉͝z̵̢̧̧̛̟̪͉̱̠̲̯̥͓̫̯̈͂ͪ̔̍̇͝͡_̷̨̮̟̭̺ͥ͐ͥ̔̀̄͟͠͞à̲͇̜̖͜_.̫̝͉̗̭̳̤ͧͥͤͤ͗̔ͦ̓͋ͦͫ̊́͟͝ Sͩͭo̗͇̽̏b̢̛̠͒̀̄̑͌̐ͤ̿͊̕r̹̝̃̈́͆ͅ_̷̺̦̬̺͎͓̔̈́̐̒̇ͪ̈̓͆ͦ͋̉e̤̲͎͕̣̮ͤ́͐_̤̥͋̃͌̌ͪ͜͝v̵̨̧̛͎͎̯̫̦̯̙̻̳͔͕ͯ̿ͥͬ̋́ͯ̃ͯ̔̆̓̿ͧ̄̕̚͘͟͜i̴̵̴̶̢̛̬̤̭̯̰̟̻̼̺̖̟͊͒̀̈́̓̌̄̍̅ͭ͂̂̂̋̕͜v̶͉̯̜̪̫̓͐̇̀̄̉̊̓͒̏͌ͯ̔͑ͭ́ͪ͡a͕̦̟͙̬͉͉̺̖̳͙̖ͭ̇ͮ̌ͮ̎́̒̈̏ͬͤ͋ͣ̊ͩͪ͢͢͞͠͝ͅn̴̥͕̫͓͚̅͋̓̃̽̈ͭ̽̊͐́͢͢͝͞͞ Ç̷̶̴̶̣̬͎̭͎̫̲̬̥͕͈̯̍ͭ͗͆̍͌ͧͣ͆͊̆̑ͨ͠͞o̵̷̟̥̠͕̜̩͚ͭ͂̾̒ͧ͒̎̌͆̿̀ͨ͒͢ͅ_̜̻m̶̡͍̰͙̞̤̫̼̩̯̗̤̻̉͂̃ͤ̓̆ͮ͗̆ͧ̈́̏͠_̶̼̲̲̉̀ͩͫ̽ͨ͒͌o̴̧̨̦̖̤̞͖̹̭̺͉͚͕̪̍̈́͐̇̒̏́͜ P͖̗͂̓͡_̸̱͕̠͍̥͙̱̋́̿ͥu̴̢̞̖̝͙͓̥͎̞̪̹̫͚̔̂̈́͐̈́̋͋ͧ̂ͮ͐͂̀͛̀͒̈̒̄ͥͦͣ̓̕͘̕͟͡͞͡͞ȩ̶̷̶̨̧̜̯̝̤̗͔̻̌ͪͨͬ̒͒̍̂̄͑̍̓̈́͘͠d̸̡̹̪̪̭͕̺͓͎̼͖̳͛ͭ̄̓̇͘͘͢͞ȧ̸̢͔͙̹͍̟̝͍̪̆ͫͫ́͟͝_͙̰̦̭͚̋ͤͩ̅ͬ̑̾͘̚͢n̵̜͗ͫ̑\"̷̧̤̺͙̻͈̱͖͉̱̺͖̞͍͔̮̦̌̂͑ͭ̿ͪ̇ͥ̂̀͛̐̍̆ͩ̿́̂̕͟",
                        "A͖̩̯͉̯̣ͩ̊͂ͭͥ̾͒͆̓ͩ͜͢͞_̞͂̓ͅb̨̹͉͇͙̿͑ͪ́̓̌͛̎ͪ͗͘s̵̡̨͓̬̝̘̜̠̙̱͍̭͇͊ͪͧͨ̓̓̀͐ͭ̊͜͟͞ͅͅơ̷̺̦̲̭͔͕̠̞̣̺̲ͣ͒ͬ̑ͨ͆̽͂̓̂̚̕̚̚͞ḻ̶̜͓͆̒̌̐ͥ͞_̉̽u͔͉̪̯͇̼ͨ͒͐͛ͣͪ̓̒̚͜͡͡_̶̶̵̛̞͖̹̪̪̳̹͋̃ͦ̇͑̓ͬ́̕͡t͎̜̩͕̳̬̗̉ͩ͒́̂̀̐̒̇͠͝͞ą̶̶̢̘̤̦̝͙̦̘̠̱̩̘̗̠̪̱͖͚͔̗͈̲̤̩̓ͭ͂ͮ͌̈͆̀̂̂ͤ͋͗ͭ͘͢͝ͅm̨͇̞͓̔͂̑ͣ͌ͨͤ̄̾̈ͩ͝ȩ̴̵̨͓̤͍̖̙ͬ̂̐͆̀̍̇̿ͥ͝͝͡n̵̷̡̛͚̯̱̭̱̘̣͕͈̲͓̳͚̲̲͔̄ͮ͆̎ͮ̓̇ͮ̈ͨ͟͝͞t̵͙͕͚̯̺́̈́̀̇̿͋̋ͬ̏͐̅ͤ̅͡ę̦͈̜͇̹̩̲̫̩̒̀̒̃ͪ̔ͯ_̨͉̣̥̫̙͐̑̓̀̓͜͠!̴̷̭̘͔̰̼ͣͧͪ̎̔͆ͤ̅̚͟͡_̟͒ͦ͑ͨ͂̿ Ą̸̸̧̞͓̞͉̣̮͉̰̣̤ͤ̂̏ͭ̑̏ͩͥ̕͡q̴̷̡̖̥̗̜̟̖̠̳̟͐̉͊͗̾͛ͦͪ̉̑̌̓̑͌̃͘͜͟͞u̩͔̫͒͌ͬ̅͢í̛̝̻̓̌̓ t_̵̷̸̡̨̢̛̬͕͇̯̮̠̫̙̲͖̠̫̤̬͉̗̓̇͌͑́ͮͧ͂́́͆̓͗͗ͦ̆ͭ̎̏͘͜͡͡íe̶̢̘̠̣̹͚͉̗̘̝̤̟̭̪͖ͣͫ̌̑̐̾̈ͬ́̑̎̄̾̀ͪ̚̚͜͡ͅne̶̷̢̨̛͚̦͑̍͋̓ͯ͗̓ͩͤ͗̇̄́ͬͯ̕s̶̢̛̖̪̠̞̰̠͈̬͙̰̎̂̈́̂̓ͯͮ̾͗ͥ̒͋͛̃ͦ͑ͥ̓̄͆̋͑ͯ͊͜͞ l̵̬̩͈̪ͣ͑ͬ̑͋ͅͅ_̡͕͕̬̼̘͚̜͓̜̊̀ͮ̀̂ͪ͐͒͒͜͜ą̨̡̜͔̤͎͚̮̱͌ͭ͊̊͛͊͊ͬͨͥ̅͂͒̓ͫ ų̶̡̮̮͔̞́̋ͬ̂̉ͮ̍̃̊̅ͦl̨̧͉̫̭̙͕̩̠̤̀̅̈́̎̈̌̄ͫ̃̿͢͡͡t̨̖͔̞̳̝̪̰̪̋̔ͬͩͭ̒͐ͨ̀̕̚_̴͇̯̺̤̣̣̪ͦͫ̒͑̐ͥ͢͞i̴̷̢̢̧̭͓̜̟̳̳͚̼͚̭̰͉͙̇̉͌̓͗̍ͭͫ̎̓ͭ̊̌́̑̈͗̓̾ͮ̽̃̕͜͝͞͡͠m̧̨̛̱̗̣̱̱͕͈͔̓̿ͯ͂̌̄̿̆̍̏͜͡á̫̜̈̽͝ p̸̴̴̡̭͖̫͇̠̱̠͓͓͎͇̀̃͌̊̊̾͒͒̍̎̍ͣ̊̉ͨͧ͜͜͞͝ͅị̵̷̶̧̛͇̤̯͚͖͉̗͇̳̼̘͎͔̱̲ͯ̃̓̌͗̂ͩ̈ͪ́̇ͣ͠ͅ_̷̥̳̮̲̈́̃͌ȩ̡̩̠̲̝ͫ̌ͧ̋ͯ̔ͮ͘z̶̶͙͓͖͈̘͐ͥ̎̆̇ͧ̔ͥ̍͢ͅ_̶̤̘̳̟̦̟ͮ͗́͂̊ͩ͒́́ͧ͂͟͢͝͝͞a̤̦̝̮̰̿̈ͪ̈́̒ͮ́͜_,̈́͆_̰̲̝̣̪͂̒̇͢͟_͙̦͔͉̼ͯ̓̾ͯ̽ͅ d̷̙̪̪͂̒͊̎̉_̘̝̤̙̻̣̝̹͋́ͥͫ̈́ͮ͒̏͌̋̋̀ͤ͌̚͢ͅi̴̢̟̫͈͙̗͈͓̙̹̦̦̹͂̾̈ͧͨ̔͌͟͜s̟̎͒_̸̘̫̫̣̬̤̙͍̜͕̒̔́ͪͬ͂̽̀̈́ͫ͆ͪ̃ͮͯ̚e̷̤̣͎͎̙͍̘̩̮ͣͩ̅̾͛̈́͒͂̿͂͂̀ͤ̽̍ͥ̕͠͞_̾͒ͦ͆͊͝͝ṇ̃̅͗ͣ́ͧͥ̃͜a̷̡͈̙̘̦̘̮̭̫̯ͫ̽͗̃͋̀̉͝͡͠d̨̢̧̠̙̮̯͔̘̮̗͐̄̓ͯ͊̎̿ͣ̀͘a̷̬̱͕̗̲̲ͤͥͨ̇̇̾̒ͤ̆͊͛ p̵̦̹̍̓̓̂͐ȁr̷͎͓̮̦̥̠̪̿̂̂̎͂ͪ̚͠a̢͍̻̳̬̰͆̐̐̆͋̒̓̚͝_̴̵̷̗͉̱͈͓͔̽́̿͌̎͗ͩ̊ͤ̿̄ͭͥ̋̕͢ e̡͕̹̳͖ͯ̈͋̒ͯ̌̌ͅl̸̡̨͙̟͇̱̯̥̫̖̼͎͍͊͑̍͛̇̈̀͐̂̀ͥ̅͐͊͑̍͒ͮͥͥ̀ͯ͜ c̟͇̝̞͒͌ͦ̊ͦ̕͜ǫ̵̴̶̜̭͎͉̖̮̫̖͍̳̌ͯͥ̾̃͐̔̑̔͂̃͗́͘l_̝̣̭̘̯̜̬͇̿ͣ̌̀̃̈́̌́ͧͬͫͪ̃̈́̉͐ͯ̕͘̚a̸̷̬͙͓͇̟͍̟͈̟̟̱̮̤̪̋̉̓ͪ̂͗ͥͩ̅ͥͪ̆̈̚͟͢͡͝͡p̯͕̮͇͊͌ͮ́͑̀͂̓ͦ̐s̶̛̹̲͇͕̬̪̘̋̏͊ͮ̏͆́̂͒ͩ̋̐͛̇͌͞͞͡ơ̴͉̗͍͊͟_̼̱̯͈͍̩̼ͩ̇ͦ͜͝ f̸ͦï̸̷̢̬̰͇͕̹̀̆̓̿͋͌͂ͪn̶͇̥͍̩̑ͨ̓ͤ͘a̩̙̯̜̮͔̓̀̓̾̈́ͧͫ̉̋̓͌͗̿ͩͮ̿ļ̪̗̥̩̞͖̰̺ͨͭ̆ͧͪͪ̉ͫ͌̕͝͡ d̟̰͇̈́̄̊̽͐̈̓ͪ̅͑ͤͯ͛̎e͈ ĺ̶̡̬̹̗̼̙̪̯̱̙͚̳ͨ͂͋͊ͣͫ͒ͣ̊͑̋͘̚͡͞a̡͙͈̞̬̫͚͍̓ͣͮ̈́̂ͤͦ̋̊̈́ͬ͆ͩ́͜͜͟ lͤ_̶̱̺͔̺̲͔̝̻͇͛̉ͩ̈̇͋̐̿̓̅ͤͣ͌͋̂̄ͫ͟ͅǫ̸̷̧͙͈́͐̔ͩ̈̌͒̇͐ģ̶͙̥͈̟͉̭̜̝ͬ̽̈́̾͂͋͞͞ĩ̴̹̩͍̬̟̝ͦ͠c̵̸̸̵̸̡̛̛̺̝̯̘̭̝̝͉͔̝̪̘̙̫͉̖̆ͣͥ̾ͬ̎͆̉͒̿ͨ̄̿͂̒̕͘̕͜a̵̢̛̭̝̗̻̠̟̎͊͐̔̒̾ͬ̾͊ͧ͊͜ y̯͒͗̿ l̈́̃a̤̥̩̞͈̦͚͆ͮ̌̆͌̊ͨ́͗ͧ̈́ c͑͡_̷̶͍̱̟̠ͫ̅͌̌̌̄ͣ̄_̶͎̺̩̞͔͛ͤ͂͠ͅǫ̴̻̼̗̤̮̲͈̘̃̒̈̐̑̀͊m̢̲͔̩͙͓̜̘͌̿͒̐̀̈͌͠ͅṷ̮̱̯̲̎͒̏́ͣn̳̰̟͔͔͑̃i̷͍̗̼̖͙̼͈ͧ͌̊ͥ̎͂̈̓͒ͯ͢͠ͅͅç̢̢̳͓̹̬̟̮̼̟̩̻̳͍̮̥̖͒ͭͦͯͩ̍͆̀̊̌̐̒̀̈́ͦ̚͢a̺̞̮͇͐̔̈́͊̐̕_̷̶̧̨̛̞̩̠͓͇͇͉͔̫̊̈́̀͒͒̂ͧͥ̈̚ç̶̱̹̜͕̫̠̖̻̤̟̊ͧ̐ͩ̇ͩ̐ͬ̄ͤ̃̃̈́̈́͞ì̶͚͎̞̥̩̱̦̓͊ͫ͘͡ó̸̹͕̫̙̲̩̒̃͑ͩ̀̓ͨ̽ń̡̙̖͍̯̻̝̟̀ͯ̽̊̓̒ͭ̉ͥ̿ͭ̀̃̈͐:̴̶̻̙͔͙̤̮͓̝͈͓͙̠̑́͆̂ͭͫ̏͂̌̄̆͐͑ͪͧ͗̕͜͟͢ u̵̱̯̹̪͈̎͑̿ͧ̐̕͜͠͝ͅn̙͍̖̂̚_̴͈̜̣͓̙̟̲̗̫̗̮̉ͦͦ̈́͌ͭ̽ͬ̾́̍͂͝ͅ m͓ͪͦ̒͒ͥ́ͤ̆̅en̶̴̝͍͍͇̺̞͔̰̈͊̃ͪ̂̋̓̌̊̈́̔͂̿͜͜͝s̴̸̸̡̭͚̮͈̹͈̤̮̞̤̦̤͊ͯ͌̐ͧ̍ͦ̊̎ͥͮ̒̀̌̎̒͂ͧ̿̊̚̚͢_a̴̢̨̲̗͙̥̫̦̓̎̿̍͒̕͠͡ͅ_̴̖͓̳̦̱̅̈̍͛̂͝͞j̷̟̱̣ͪ̈̋̑̓̇̕͟͠ͅe̵̛̩̝̩̩͔̯͓̥͓͇̱̺̪̍́̅̏ͫͭ́̇͐͘͢͟͟ ċ̘͍̿ͪ̇ͦ͆͐ǫ̶̵͎͔̳̯̮͕͓̜͛͒̏͐ͮ͊͌ͧ̆͐̈́͘r̢̝̯̭͚ͧͪ͆͊͊ͭ͋̾̊́͐r̶̸̨̯̹͉̰̤̗̼͓̰̙̬̥̔ͤ̂ͩͦ̊͐ͫ̂͐́͆̆ͤ͛̀ͥͩ́̕͠͝u̸_̢̡̘̳̩̘̟̜͓ͪ̒̅̈̓͌̉̋ͅ_̳̼͍͛ͫ͊ͅp̷̴̧̟̠̫̙̟͕̦̳̥̼͓̟̜̝͇͖͔̤̀̅̀ͦ̐ͩ̓ͫ̽͐̓̉́ͩ͛ͧ͌̌ͧ͡͠t̰̼͖̪̱̣̫̩̆͂͂̋ͤ̅̊̓ͧͬͦ̕ͅo̡̢̧͕̱͍͚̘̥̼͎͛̎̒͂̾͆̋ͩ̍͆̓ͦ͊̔ͅ ȳ̶̷̛͈̬͕͎͇̱̲̺̬͖͆͑ͭ̄̀̽̄ͮͫͭ̚͞ c̡̨̮̦̞̣̘̤̠͉͇ͯͦ̅͊́̎̒̒̾́͢͢_̢̼̗͚͔̩͕͖̠͓̃ͨ̋̽̈̈́̅̌ͦ̀̑̅̚r͖̩͕̳̜͂̚̚ͅí̵̩̣̯̮͇͕̥́ͤ̽̅ͯͦ͜p̘̖͉͈͉̤̃̊ẗ̴̷̶̴̢͚̟̙͇͈̝̱̝͖̳̣͔͉̣̫̼̪́͗̃ͪͮͨ̈́̇̅͑͗̀̾̃ͬ̾̋͋̃̅ͣ̈͜͝i̢̛̛̠̦̮̮̥̙̳͉͔̘̦͕̰͍̥̥̞̳ͬ́ͮ̀ͬͦͩ̍͂͋͐̓̈́̌͊̚̕͟͞͞c̯ͮ͢o̬̭̟̪̭͂̇ͥ̊_̙ͣ_̛̹̠͓̠͍̣̟̬̟̙̝̒ͬͣͯ̋͒̅͡ ê̬͍̘͊͑ͭ͞ͅǹ̷̰̲̳̤̞̟̈ͣ̂̆ͧ̓̐̍̉̐_̴̛̖̮̤̍̏͜͞ ù̴̢͈͚̼̭̯̭̘̩̼̔̅ͪ̓̃_̷̷̶̵̛̮̞̳̥͉̝ͯ̆̀͆̌ͯ͘͜ņ̛̼͕ͣ̓̆͑͌ͬ̍̀ͨͦ̉̕a̶̧̻̠̮͔̻̟̻̞̥͕̳͆͐̓͋̂̋̓ͥͤ̋́ͦͣ̂̑͗̃͘͢͢͞͝ s̷͔̰̳̹͙̫ͬͫ̋͐̋͛̍ͬ̅ͬo̵̶̷̢̢̫̭̤̣̯̽̏̅̉ͫͫ́͆̈́͐͞_̟̼͖̞͉ͦ̓̀ͬ͋͠͝͞l̢̎̌͢͡ả̶̡̤̮͕̹̗͇̠̦͎̎ͦ͋͒͌̚ l̶̶̢̢͕͖͔͆́́̒̑ͣ͑ͩ̂̈͗ͅí̷̸̴̹͎̯̩ͧͭ͗͗̿͞͡n̸̻̬̳̈̉̊̽́ͬ̎ẹ̷̢̧͇͚̲͖̫̺̦̳̙̣̤͎̰̱͓̗̽͐̂͌̒ͤͨ̋͆̀̏̌͠a̰̦͉̱͛ͮ͒͂͘ q̵̡̢̛̛̗͕̳̥̗̜́̎̿ͩͭ̾̔́͒͟͞͠͠͞͝_̨̺͚̱̞ͯ̂͆̀͐̏͞ͅͅṳ̸̷̡̻̰̥̟̃̓ͣͥͭ̓̇_̸̡͍͚͙͚͉̮̩̯̞̙ͥͨ̍̿ͨ̇ͯ͑ͬ̓͐̈ͤ̕͢͝ė͉̤͙͋ͥ̽ͧ̃̄̓̐́̑͜͢ ṡ̴̴̛̼͓̙͎̺̳͔̙̹͇̪̙̏ͬ̈̓ͯͨ̃̆̍̏ͩ̄́̀̑́͜͢͢͠͝͠ữ̶̡̪̲̥̯͈̱̗̰̻̉̇ͮ̈̊̆̒̈́̈́͋͗̅̀̍͛͟͠͞͡ͅg̶̘̪̻͗͑i̵̶̢̡̧̺̬̞̯͈̤ͣ͛ͯͯ͛̒͌ͩ̿̎̈ͬ͌͘͢͜_̶̸̹̟̦̫̜̟̩̽͌̈́ͨͤ̓͝͞ề̴̷̸̯̱̟̳̹͉̭͗̆͌̊͡͝r̭̰͟è̢͍͇͍̜̪̙̘̼͖̞͈̮̮̮͎̙̍̄͆̓ͧͧ͒ q̴̨̥̈̐̚͟_̶̧̖̮̥͖͓̼̝ͨͭ̊̍͐̋̄ͥ͛͊̍ͣ͗̕͡͡ų̶̷̶̳̼̺͍͎̙̩͇̭̎̀̓̾͊ͯ̉ͅe̢̟̥͉̬͚̗̣͇͓̤ͦͬ́͊̑̅ͮ̎̄͌̌̌̄͞ ļ̸̴̸̡̛̛̹̘̖̠͇̝͖̫͕̩͔͚͉͑͒ͬ̿ͨ͆́̄̈ͦ̅̅̅͟͜͠a̷̪̙̮̗̩͚̹̫͔ͩ͊̄ͨ̀̌̓͌̂͢͜͝ p̳̘͍̆ͧ͛́̈ͭͭ͛ͯ̔̅͐̚͟_̞̒̈̚ṙ̖̟̜̩̙̟̺̻͚̑̇ͪ̓̽̏ͥơ̶̵̴̴̛̜̼̻͙̘̳̮̟̠̦̻̭͕̭̇͑̑̓̈͛ͧ͌̅̑̿ͫ͗̀͐̌̌͊̄̌ͣͫ͋ͬ̏͜͢p̥̤͓̋̑ͭ̋ͬ̓͘ͅi̶̡̺͚͈̺͕̠͈̺̣͋ͮ͆̉ͧͩͦͥ̓̕̕͟͜a̴̶̝̩̫̟̬̼̍̐ͪ́͐̽ͣ́̾̿̎̑̈͘͡ i̬̫̟n̷̿̓f̴̛͔̭̘̥̼̱̲̥̯̟̞̘̠̗̼͈͚̿ͦ̾̅̎ͣ̀̓̀̓̈̀ͤ̽ͬ̃͑̚͘͟͝͡͠͡͡͞ṙ̴̌̇̂͐̿a̺̟̟̥̰̦̩ͮ͌ͦͭ̀̋̄́ͭ̍̾͛͘͜͞ͅȩ̵̸̴̢̛̝̹̺̫̞̼̦̦̻͓̳̗̲̗͖̜̟̻̩̙͈͇ͩ̿͗ͭ̅ͯ̉̐̅͒̏́̓͘͢͢͡s̶̡̡̛̺̪̯̣͕̦̭͚̥͗́ͬ̀ͩͦ̚ͅt̡͎̣̠͎̖̯͚̅̏ͪͨͬ̐ͩͬ̃̿͋̍̚̚̚͘͜r̗̘̩̮͓͕͔̈̊̍̈́ͧ͟͞͝_̴̡̨̧̠͚̩̬̲͚͓̝͌͑̈́̏ͯ̈ͯͨ̀ͩ̊ͯ͊ͮ͢͠͞͠͠ū͎͉͖͖̹͙̑ͥ̑ͯ̅ͮ͊͗c̢̛͈͍͈̘͍͈͉̞̩̰̹̼̾̋ͯ͑͐̂͊̎ͮͭ̔͗ͥ̿̑͌̃ͫ̓̃̕͢͟͢͟͠͠ͅ_̗͕ͦ̉tͦű̌͢_̵̸̡̨̦̦͇̣̜͇̳͈ͫ̄̄͋͗̈́͒̌́̒ͭͩ̕͢͢͠ͅr͉̞̀ä̢̢̡̤͈̻̝̮͇̳̱̌̌͆̄̉ͯ͢_̧̘͉̙̥̗̮͋͒̀ͯͮ̽͌̓ͩ͢ h̷̵̢̧̧̟̮̮̬̙̭̻̙͈ͣ͂͛̏ͤ̂ͪ̏̽̔͊ͬ̄̀ͪͫ́̄̚͟ͅã̴̧̡͎̞̝̠̣̥̥͇̳̤͚̪͔̍̿̅̏͋̈ͫ̔̈ͥ͗̈́͋̀̍̎̾̚͡ͅ ş̻͎͟i̸̡̢̱͍͕̝̺̬͆́ͦ͗̐_̵̬͚̩̹̎́̾̏̊ͬͪͭ̚_̛̘̗͇͓̮͓͕͇̂͐̉ͭ̂̕̚͟d̷̶̶̡͓̮̳̥̟̪̮̳̪̟͉͗͌ͫͮͣ́̇ͥ͋̈̓̇̇ͮ̿̕͜ỏ̧͉͍͙͓̯͐̈́̆̀̅ d̡̛̩̝̻̘̃̓ͫ͂̓͗͊̑̇̚͜͡į̸̴̧̢̳̱͍͕̼͎͉̫̣͕̹̟͇͍̥̮͋̆̅ͧ͑̆̃͑̏̀͂̌̓ͣͤ̚͞ş̵̷̨̧͈̟̠̯̺̖̤̺̖̓̈́͂ͨ̋̌̌̒͋̑̓͋ͥ͛̓̏̊͌̓̋̓ͧ̓̍̇͘̕͜ͅt̳̂o͛r̴̢̢̢̼̜̺̬̠̳͖̪̺̘̬͉͙̟̳̲̊̅̒̂̃͂̊̓̑ͪ͊ͥ̀ͦ̂̕͘ͅs̢̖͝i̸̷̶̢̥̺͎͎̱͎͗̈́̓ͧ̈͆̔̊̉̐̎̃ͫ̉̌̎͒ͬ͐̂̚͟͡͡͡͡o̴̴̴̶̡͉̫͉̩̙͌̇ͧ͘͟͞ń̶̴̷̡͔̗͇̝̗̰̹͍̙͙͕̳͛͑ͬ͒̀̽ͪ̍̏̍̏̑ͯͭͨ̎ͩ̇͆͟͡͡a͉̻̪̝̹̬ͭ̈̎͛͐̀͊̑̀̈͋̔̏̾̚͟d̦̱͎́́̃ͣ̾͢ͅa̜͔̒ͬ͑͢͢_̶̛̣͖̦̫̝͍̗̠̰̝̯ͪ͌̌̽ͧͤ̿̽͌ͦͭ̀̔̚͞_ p̸̷̢̥͇̪͔̹̮ͧ̀ͪ́ͪ͗ͅ_̵̴̬̱͕̃̇͗́̓̋̌͋ͩ̉̿͊͢o̵̡̧̨̬̞̮̟̜̺̖͈͍͓̬̪͎̭͖̝ͯ͊̈́̊͂͊ͯ̽̿ͣͦ͂ͭ̿̿̌ͧ̉́̈́̕͜͟͞͞r̭̻̓́̄̾ͤ̽͘͜ l_̶̵̡̝̗͓̘̥͕̰̗͇̣̼̦̟̼̭͎͚̣̝̘͔̻̟̑̓͊́̿ͣ͑͊ͦͫ͗͛̍̃́͊̚̕͘̚ă̻̺͆͢ ą̸̧͔̳̮̺̰̠̼̮͍͎͈͓͓̯͔̻̿̏ͨ̇͌̎̀̃̽̔ͣͬ̆̉͋̍̈́̍̒͢͠͡ͅm̷̸̵̡̧̟̙̦͎̠̺̳̹̼̗͉̜͉̠͖͖̾̋̂̌̈́̊̊̀̄ͩ́͒̑̋̈ͧ̕͘͜͢͝ͅen̸̵̵̢̘̮̙͕̬͔͓͍̽̂̃̓ͧͨͥ͂ͩ̿͆̋̒̉̐͡ã̷̷̧͔͙̰͕̰̖̺̬̝̩̬̪̱̱ͣ́́͗ͣ͌ͧͩ́͆ͮ͆̚͡z̶̢̨̫̰̗̱͚̮̳̐̍ͭ̄̍̈̿̍͆ͥ͛ͤ̒̑͑͋ͣ̒͛ͧͫ͘̚͟͠á̡̫͈̪̲͓͚͇̪͇͎̈̂ͣ̇͒ͫ̿̊̾͊ͬ͗͡.̷̢̛̥͓̦̪̜͇̞̠͉̤̝̹̤̘̹͌ͯ̇ͦ̇ͩ̒̓ͮ̓̓̄͌̏̐̃̚͘͘̕͜͠͝",
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(NewsPanelActivity.this, TheEndNewActivity.class);
                                startActivity(intent);
                            }
                        });
                anchor.set(id11);

                // Mostrar notificación con solo el título
                showNotificationWithTitle("Nuevo aviso del bosque de Alfaguara");
            }
        });
        // Programar aparición única de la segunda noticia a los 5 segundos (prueba)
        newsTimerController.startOneShot(100000L);


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
    protected void onDestroy() {
        super.onDestroy();
        // Cancelar timer para evitar callbacks cuando la Activity ya no existe
        /*if (newsTimerController != null) {
            newsTimerController.stop();
            newsTimerController = null;
        }*/
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