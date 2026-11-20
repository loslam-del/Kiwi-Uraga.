package com.kiwiuraga.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.webkit.*;
import android.view.View;
import android.widget.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private LinearLayout root, savedList;
    private TextView status, modelStatus, preview;
    private EditText urlInput;
    private WebView reader;
    private static final int PICK_MODEL = 41;

    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }
    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); return t;
    }
    private Button button(String value) {
        Button b = new Button(this); b.setText(value); return b;
    }
    private void space(int h) { View v = new View(this); root.addView(v, new LinearLayout.LayoutParams(1, dp(h))); }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(16,16,22));
        getWindow().setNavigationBarColor(Color.rgb(16,16,22));
        root = new LinearLayout(this); root.setOrientation(1); root.setPadding(dp(20),dp(24),dp(20),dp(24)); root.setBackgroundColor(Color.rgb(16,16,22));
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.addView(root); setContentView(scroll);

        root.addView(text("🥝  Kiwi Uraga",27,Color.WHITE));
        TextView sub=text("LOCAL AI WORKSPACE",12,0xffb8a1ff); sub.setPadding(0,dp(4),0,dp(20)); root.addView(sub);

        root.addView(text("Подключение модели",20,Color.WHITE)); space(6);
        modelStatus=text("Модель пока не выбрана.",14,0xffc8c3d2); root.addView(modelStatus);
        Button choose=button("Подключить файл модели (.npz)"); root.addView(choose);
        choose.setOnClickListener(v -> { Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("*/*"); i.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i,PICK_MODEL); });
        space(18);

        root.addView(text("Импорт информации с сайта",20,Color.WHITE)); space(5);
        root.addView(text("Вставь ссылку — приложение попробует извлечь основной текст страницы и сохранить его локально.",13,0xffc8c3d2));
        urlInput=new EditText(this); urlInput.setSingleLine(true); urlInput.setHint("https://example.com/article"); urlInput.setTextColor(Color.WHITE); urlInput.setHintTextColor(0xff888493); urlInput.setInputType( android.text.InputType.TYPE_TEXT_VARIATION_URI); root.addView(urlInput);
        Button importButton=button("Загрузить страницу"); root.addView(importButton);
        status=text("Готово к импорту. Для загрузки сайта нужен интернет.",13,0xffb8a1ff); status.setPadding(0,dp(8),0,dp(8)); root.addView(status);
        preview=text("",13,0xffe5e1ed); root.addView(preview);
        importButton.setOnClickListener(v -> loadSite());

        reader=new WebView(this);
        reader.getSettings().setJavaScriptEnabled(true);
        reader.getSettings().setDomStorageEnabled(true);
        reader.getSettings().setLoadsImagesAutomatically(false);
        reader.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                if (!url.equals("about:blank")) {
                    status.setText("Страница открыта. Извлекаю текст…");
                    view.evaluateJavascript("(function(){return JSON.stringify({title:document.title||location.hostname,url:location.href,text:(document.body?document.body.innerText:'')});})()", result -> saveExtracted(result));
                }
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) status.setText("Не удалось загрузить страницу: " + error.getDescription());
            }
        });
        root.addView(reader,new LinearLayout.LayoutParams(1,1));
        space(18);
        root.addView(text("Сохранённые материалы",20,Color.WHITE));
        savedList=new LinearLayout(this); savedList.setOrientation(1); root.addView(savedList); refreshSaved();
        space(14);
        root.addView(text("Генерация изображений",20,Color.WHITE));
        root.addView(text("GAN-веса можно выбрать выше. Сам формат NPZ пока требует отдельного конвертера в мобильную модель; генерация станет активной после подключения совместимого runtime.",13,0xffc8c3d2));
    }

    private void loadSite() {
        String u=urlInput.getText().toString().trim();
        if (!(u.startsWith("https://") || u.startsWith("http://"))) { status.setText("Укажи полный адрес, начиная с https://"); return; }
        status.setText("Загружаю страницу…");
        reader.loadUrl(u);
    }

    private void saveExtracted(String encoded) {
        try {
            Object decoded = new org.json.JSONTokener(encoded).nextValue();
            if (!(decoded instanceof String)) throw new Exception("Не удалось прочитать ответ страницы");
            org.json.JSONObject data = new org.json.JSONObject((String)decoded);
            String title=data.optString("title","Без названия");
            String url=data.optString("url,"");
            String body=data.optString("text","");
            body=body.replaceAll("[\\t\\x0B\\f\\r]+"," ").replaceAll(" {2,}"," ").trim();
            if (body.length()>500000) body=body.substring(0,500000);
            if (body.length()<40) { status.setText("На странице почти нет доступного текста. Возможно, нужен вход, JavaScript или сайт блокирует просмотр."); return; }
            String safe=title.replaceAll("[^\\p{L}\\p{N}._-]+","_");
            String stamp=new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(new Date());
            File dir=new File(getFilesDir(),"knowledge"); dir.mkdirs();
            File out=new File(dir,stamp+"_"+safe+".txt");
            try (FileWriter w=new FileWriter(out)) { w.write(title+"\n"+url+"\n\n"+body); }
            status.setText("Сохранено локально: "+out.getName());
            preview.setText(title+"\n"+body.substring(0,Math.min(900,body.length())));
            refreshSaved();
        } catch(Exception e) { status.setText("Не удалось извлечь текст: "+e.getMessage()); }
    }

    private void refreshSaved() {
        if(savedList==null)return;
        savedList.removeAllViews();
        File dir=new File(getFilesDir(),"knowledge"); File[] files=dir.listFiles();
        if(files==null || files.length==0) { savedList.addView(text("Пока нет сохранённых страниц.",13,0xffaaa5b4)); return; }
        Arrays.sort(files,Collections.reverseOrder());
        for(File f:files) {
            TextView item=text("• "+f.getName(),13,0xffe5e1ed);
            item.setPadding(0,dp(7),0,dp(7));
            item.setOnClickListener(v -> {
                try { byte[] bytes=java.nio.file.Files.readAllBytes(f.toPath()); preview.setText(new String(bytes,java.nio.charset.StandardCharsets.UTF_8)); status.setText("Открыт локальный материал."); }
                catch(Exception e) { status.setText("Ошибка чтения материала."); }
            });
            savedList.addView(item);
        }
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==PICK_MODEL && resultCode==RESULT_OK && data!=null) {
            Uri uri=data.getData();
            try {
                String name="selected_model.npz";
                try(android.database.Cursor c=getContentResolver().query(uri,null,null,null,null)) {
                    if(c!=null && c.moveToFirst()) { int ix=c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME); if(ix>=0) name=c.getString(ix); }
                }
                File target=new File(getFilesDir(),name);
                try(InputStream in=getContentResolver().openInputStream(uri); OutputStream out=new FileOutputStream(target)) {
                    byte[] buf=new byte[8192]; int n; while((n=in.read(buf))>0)out.write(buf,0,n);
                }
                modelStatus.setText("Файл подключён: "+name+" ("+(target.length()/1024)+" KB). Для выполнения GAN требуется конвертация NPZ в мобильный формат.");
            } catch(Exception e) { modelStatus.setText("Не удалось скопировать файл модели: "+e.getMessage()); }
        }
    }

    @Override protected void onDestroy() { if(reader!=null) reader.destroy(); super.onDestroy(); }
}