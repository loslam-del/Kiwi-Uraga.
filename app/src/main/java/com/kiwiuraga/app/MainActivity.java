package com.kiwiuraga.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.graphics.Typeface;

public class MainActivity extends Activity {
    private LinearLayout gallery;
    private TextView status;
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(16,16,22));
        getWindow().setNavigationBarColor(Color.rgb(16,16,22));
        LinearLayout root = new LinearLayout(this); root.setOrientation(1); root.setPadding(24,28,24,20); root.setBackgroundColor(Color.rgb(16,16,22));
        TextView title = new TextView(this); title.setText("🥝  Kiwi Uraga"); title.setTextColor(Color.WHITE); title.setTextSize(27); title.setTypeface(null, Typeface.BOLD); root.addView(title);
        TextView subtitle = new TextView(this); subtitle.setText("OFFLINE AI IMAGE GENERATOR"); subtitle.setTextColor(0xffb8a1ff); subtitle.setTextSize(12); subtitle.setPadding(0,5,0,24); root.addView(subtitle);
        TextView heading = new TextView(this); heading.setText("Генерация изображений"); heading.setTextColor(Color.WHITE); heading.setTextSize(20); root.addView(heading);
        status = new TextView(this); status.setText("Интерфейс готов. Для генерации подключим файл весов GAN."); status.setTextColor(0xffbdb8ca); status.setPadding(0,12,0,12); root.addView(status);
        Button generate = new Button(this); generate.setText("Сгенерировать"); root.addView(generate);
        generate.setOnClickListener(v -> status.setText("Файл весов GAN ещё не встроен. Следующий шаг — перенести gan_weights.npz в формат модели приложения."));
        TextView galleryTitle = new TextView(this); galleryTitle.setText("Моя галерея"); galleryTitle.setTextColor(Color.WHITE); galleryTitle.setTextSize(20); galleryTitle.setPadding(0,28,0,12); root.addView(galleryTitle);
        gallery = new LinearLayout(this); gallery.setOrientation(1); root.addView(gallery);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.addView(root); setContentView(scroll);
    }
}
