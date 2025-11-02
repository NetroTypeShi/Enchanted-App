# Enchanted-App

## Cosas que hizo la IA

### Sistema de tarjetas: 
Opción A — simple y rápida: mantener la última tarjeta creada como ancla. Para eso modifica addNewsCard para que devuelva el CardView (o su id) o acepta un anchorId. Ejemplo de función que devuelve CardView:

Java
private CardView addNewsCard(ConstraintLayout main, String title, String body, View.OnClickListener listener) {
    // ... misma creación ...
    main.addView(card, cardLp);

    ConstraintSet cs = new ConstraintSet();
    cs.clone(main);
    cs.connect(card.getId(), ConstraintSet.TOP, R.id.textView3, ConstraintSet.BOTTOM, dpToPx(12));
    cs.connect(card.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, dpToPx(16));
    cs.connect(card.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, dpToPx(16));
    cs.applyTo(main);

    return card;
}
Y luego al crear varias:

Java
int anchorId = R.id.textView3;
for (News n : newsList) {
    CardView card = addNewsCardBelow(main, n.title, n.body, listener, anchorId);
    anchorId = card.getId(); // la próxima tarjeta se anclará debajo de esta
}

### Canal de notificaciones avanzado:
Crear el canal antes de enviar notificaciones (Android O / API 26+) En onCreate() crea el canal. Pega esto en onCreate (o en un helper llamado desde onCreate):
Java
private void createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        String name = "Noticias";
        String description = "Canal para notificaciones de noticias";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }
    }
}

### Chanel ID
private static final String CHANNEL_ID = "news_channel_id";

// en onCreate:
createNotificationChannel();

// helper:
private void showNotificationWithTitle(String title) {
    // permisos Android 13+ ya deben estar gestionados
    NotificationCompat.Builder builder = new NotificationCompat.Builder(NewsPanelActivity.this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // usa tu drawable si tienes
            .setContentTitle(title)
            .setContentText("") // vacío porque solo quieres título
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true);

    NotificationManagerCompat.from(NewsPanelActivity.this)
            .notify((int) (System.currentTimeMillis() % Integer.MAX_VALUE), builder.build());
}
