package com.example.alertaciudadana.Views;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.alertaciudadana.R;

public class NewsPanelActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_news_panel);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ConstraintLayout main = findViewById(R.id.main);

        // ancla inicial: debajo de textView3 (tal y como está en tu XML)
        int anchor = R.id.textView3;

        // Primera tarjeta: actualizamos 'anchor' con el id del card creado
        anchor = addNewsCard(main, anchor,
                "El ayuntamiento de Alfacar detecta sonidos y acontecimientos extraños en el bosque de Alfaguara",
                "Los cazadores y senderistas han avistado sonidos de animales desconocidos en el bosque y proximidades, los testigos a pesar de buscar, no han encontrado la fuente de los sonidos",
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // acción del botón "Detalles"
                        Intent intent = new Intent(NewsPanelActivity.this, FirstNewActivity.class);
                        startActivity(intent);
                    }
                });

        // Segunda tarjeta: se anclará debajo de la anterior porque usamos el anchor devuelto
        anchor = addNewsCard(main, anchor,
                "Mepicanlococo",
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // acción del botón "Detalles"

                    }
                });

        // Para añadir más tarjetas: repetir anchor = addNewsCard(main, anchor, title, body, listener);
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