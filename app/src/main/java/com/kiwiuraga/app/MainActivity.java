package com.kiwiuraga.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.*;
import android.net.Uri;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.provider.MediaStore;
import android.database.Cursor;
import android.webkit.*;
import android.view.View;
import android.view.Gravity;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import android.util.Base64;

public class MainActivity extends Activity {
    private static final int PICK_MODEL=41;
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private LinearLayout root, content, chatBox, libraryBox;
    private TextView status, modelStatus, imageStatus, imagePreview;
    private EditText chatInput, urlInput, promptInput;
    private EditText baseInput,keyInput,chatModelInput,imageModelInput;
    private WebView reader;
    private android.graphics.Bitmap lastImage, mascot;
    private SharedPreferences prefs;
    private String selectedModel="";
    private int bg=0xff0b0812, panel=0xff1b1427, white=0xfffff7ff, muted=0xffb9a9c9, accent=0xffff8de1, soft=0xff30213f;

    int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    TextView label(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;}
    GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));d.setStroke(dp(1),0x667e548f);return d;}
    GradientDrawable gradient(int radius){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xffff9be7,0xffbd65dc,0xff7146b5});d.setCornerRadius(dp(radius));return d;}
    Button btn(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(white);b.setTextSize(14);b.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);b.setPadding(dp(16),dp(10),dp(16),dp(10));b.setMinHeight(dp(48));b.setElevation(0);b.setStateListAnimator(null);b.setBackground(gradient(18));return b;}
    EditText field(String hint,String value){EditText e=new EditText(this);e.setSingleLine(true);e.setHint(hint);e.setText(value);e.setTextColor(white);e.setHintTextColor(0xff9585a8);e.setTextSize(14);e.setPadding(dp(16),dp(13),dp(16),dp(13));e.setBackground(shape(0xff21182f,18));return e;}
    void gap(LinearLayout box,int h){View v=new View(this);box.addView(v,new LinearLayout.LayoutParams(1,dp(h)));}
    ImageView mascotView(int size){
        ImageView iv=new ImageView(this);
        try{if(mascot==null)mascot=BitmapFactory.decodeStream(getAssets().open("kiwi_avatar.jpg"));iv.setImageBitmap(mascot);}
        catch(Exception e){iv.setImageResource(android.R.drawable.sym_def_app_icon);}
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iv.setBackgroundColor(0xff292139);
        iv.setClipToOutline(true);
        iv.setContentDescription("Kiwi Araga mascot");
        iv.setLayoutParams(new LinearLayout.LayoutParams(dp(size),dp(size)));
        return iv;
    }
    void card(LinearLayout parent,View child){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(8);child.setBackground(shape(panel,18));child.setPadding(dp(12),dp(10),dp(12),dp(10));parent.addView(child,p);}
    @Override public void onCreate(Bundle b){
        super.onCreate(b); prefs=getSharedPreferences("kiwi",MODE_PRIVATE);
        getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);
        root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(18),dp(14),dp(18),dp(12));GradientDrawable backdrop=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xff1b0e27,0xff0c0914,0xff100d1c});root.setBackground(backdrop);
        setContentView(root);
        LinearLayout brand=new LinearLayout(this);brand.setOrientation(0);brand.setGravity(Gravity.CENTER_VERTICAL);brand.setPadding(dp(14),dp(12),dp(14),dp(12));brand.setBackground(shape(0xff21152f,26));LinearLayout.LayoutParams brandp=new LinearLayout.LayoutParams(-1,-2);brandp.bottomMargin=dp(14);root.addView(brand,brandp);
        ImageView brandMascot=mascotView(112);brandMascot.setScaleType(ImageView.ScaleType.FIT_CENTER);brand.addView(brandMascot);
        LinearLayout brandText=new LinearLayout(this);brandText.setOrientation(1);brandText.setGravity(Gravity.CENTER_VERTICAL);brandText.setPadding(dp(12),0,0,0);brand.addView(brandText,new LinearLayout.LayoutParams(0,-2,1));
        TextView eyebrow=label("YOUR PERSONAL AI",9,accent);eyebrow.setLetterSpacing(.18f);brandText.addView(eyebrow);
        TextView title=label("Kiwi Uraga",25,white);title.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);brandText.addView(title);
        TextView subtitle=label("いつも一緒に  ✦  KIWI ARAGA",10,muted);brandText.addView(subtitle);
        LinearLayout nav=new LinearLayout(this);nav.setOrientation(0);nav.setGravity(Gravity.CENTER);nav.setPadding(dp(5),dp(5),dp(5),dp(5));nav.setBackground(shape(0xff1a1324,22));LinearLayout.LayoutParams navp=new LinearLayout.LayoutParams(-1,-2);navp.bottomMargin=dp(8);root.addView(nav,navp);
        String[] names={"Чат","Фото","База","Настройки"};String[] ids={"chat","images","library","settings"};
        for(int i=0;i<names.length;i++){final String id=ids[i];Button btt=btn(names[i]);btt.setTextSize(11);btt.setPadding(dp(2),0,dp(2),0);btt.setMinHeight(dp(44));btt.setElevation(0);btt.setStateListAnimator(null);nav.addView(btt,new LinearLayout.LayoutParams(0,-2,1));btt.setOnClickListener(v->show(id));}
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        content=new LinearLayout(this);content.setOrientation(1);sc.addView(content);
        status=label("✦  ГОТОВА К РАБОТЕ  ✦",10,accent);status.setGravity(Gravity.CENTER);status.setPadding(0,dp(7),0,0);root.addView(status);
        setupReader();show("chat");
    }
    void setupReader(){
        reader=new WebView(this);reader.getSettings().setJavaScriptEnabled(true);reader.getSettings().setDomStorageEnabled(true);reader.getSettings().setLoadsImagesAutomatically(false);
        reader.setWebViewClient(new WebViewClient(){
            @Override public void onPageFinished(WebView v,String url){if(!"about:blank".equals(url)){status.setText("Страница загружена, извлекаю текст…");v.evaluateJavascript("(function(){return JSON.stringify({title:document.title||location.hostname,url:location.href,text:document.body?document.body.innerText:''});})()",r->saveExtracted(r));}}
            @Override public void onReceivedError(WebView v,WebResourceRequest req,WebResourceError err){if(req.isForMainFrame())status.setText("Ошибка загрузки: "+err.getDescription());}
        });
    }
    void show(String page){
        content.removeAllViews();
        if("chat".equals(page))showChat();
        else if("images".equals(page))showImages();
        else if("library".equals(page))showLibrary();
        else showSettings();
    }
    void heading(String s){
        LinearLayout row=new LinearLayout(this);row.setOrientation(0);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(13),dp(10),dp(13),dp(10));row.setBackground(shape(0xff21172e,20));LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.bottomMargin=dp(13);content.addView(row,rp);
        ImageView avatar=mascotView(54);avatar.setScaleType(ImageView.ScaleType.FIT_CENTER);row.addView(avatar);
        LinearLayout texts=new LinearLayout(this);texts.setOrientation(1);texts.setPadding(dp(10),0,0,0);row.addView(texts);
        TextView eyebrow=label("KIWI URAGA  /  SPACE",9,accent);eyebrow.setLetterSpacing(.14f);texts.addView(eyebrow);
        TextView t=label(s,19,white);t.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);texts.addView(t);
    }
    void showChat(){
        heading("AI-ассистент");
        TextView hint=label("Твоя личная AI-комната. Общайся с Киви, задавай вопросы и сохраняй идеи.",13,muted);hint.setPadding(dp(3),0,dp(3),dp(12));content.addView(hint);
        chatBox=new LinearLayout(this);chatBox.setOrientation(1);content.addView(chatBox);
        String history=prefs.getString("history","");
        if(!history.isEmpty())for(String line:history.split("\\n\\n"))addMessage(line,false);
        chatInput=new EditText(this);chatInput.setHint("Напиши сообщение…");chatInput.setTextColor(white);chatInput.setHintTextColor(0xff888493);chatInput.setTextSize(15);chatInput.setPadding(dp(15),dp(13),dp(15),dp(13));chatInput.setBackground(shape(0xff21182f,18));content.addView(chatInput);
        Button send=btn("Отправить");content.addView(send);send.setOnClickListener(v->sendChat());
        Button clear=btn("Очистить историю");content.addView(clear);clear.setOnClickListener(v->{prefs.edit().remove("history").apply();chatBox.removeAllViews();});
    }
    void addMessage(String s,boolean persist){
        LinearLayout row=new LinearLayout(this);row.setOrientation(0);row.setGravity(Gravity.TOP);
        boolean fromKiwi=s.startsWith("Kiwi:");
        if(fromKiwi){ImageView avatar=mascotView(38);LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(dp(38),dp(38));ap.rightMargin=dp(7);row.addView(avatar,ap);}
        TextView t=label(s,14,white);t.setPadding(dp(14),dp(12),dp(14),dp(12));t.setBackground(shape(fromKiwi?0xff24172f:0xff191522,18));
        row.addView(t,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(6);chatBox.addView(row,p);
        if(persist){String h=prefs.getString("history","");h+=(h.isEmpty()?"":"\n\n")+s;if(h.length()>30000)h=h.substring(h.length()-30000);prefs.edit().putString("history",h).apply();}
    }
    void sendChat(){
        String question=chatInput.getText().toString().trim();if(question.isEmpty())return;
        chatInput.setText("");addMessage("Ты: "+question,true);
        String key=prefs.getString("api_key",""),base=prefs.getString("base","https://api.openai.com/v1"),model=prefs.getString("chat_model","gpt-4o-mini");
        String knowledge=searchKnowledge(question);
        if(key.isEmpty()){String answer=knowledge.isEmpty()?"Офлайн-режим: API не подключён. Добавь ключ и модель в настройках, чтобы включить облачный AI.":"Нашёл в локальной базе:\n"+knowledge;addMessage("Kiwi: "+answer,true);return;}
        status.setText("AI отвечает…");
        io.execute(()->{
            try{
                JSONObject body=new JSONObject();body.put("model",model);body.put("temperature",0.7);
                JSONArray msgs=new JSONArray();JSONObject sys=new JSONObject();sys.put("role","system");sys.put("content","Ты Kiwi Uraga, полезный русскоязычный AI-ассистент. Отвечай ясно и честно. Если ниже есть локальные материалы, используй их как справочный контекст и не выдумывай факты.\nЛокальная база:\n"+knowledge);msgs.put(sys);
                JSONObject user=new JSONObject();user.put("role","user");user.put("content",question);msgs.put(user);body.put("messages",msgs);
                JSONObject response=postJson(base+"/chat/completions",key,body);
                String answer=response.getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content","Пустой ответ");
                runOnUiThread(()->{addMessage("Kiwi: "+answer,true);status.setText("Ответ получен.");});
            }catch(Exception e){runOnUiThread(()->{addMessage("Kiwi: Ошибка подключения: "+e.getMessage(),true);status.setText("Проверь API и интернет.");});}
        });
    }
    void showImages(){
        heading("Генератор изображений");
        TextView intro=label("Превращай идеи в изображения вместе с Киви.",13,muted);intro.setPadding(dp(3),0,dp(3),dp(12));content.addView(intro);
        promptInput=new EditText(this);promptInput.setHint("Опиши изображение…");promptInput.setTextColor(white);promptInput.setHintTextColor(0xff888493);promptInput.setBackground(shape(0xff261c35,16));promptInput.setPadding(dp(14),dp(10),dp(14),dp(10));promptInput.setMinLines(3);promptInput.setGravity(Gravity.TOP);content.addView(promptInput);
        Button gen=btn("Сгенерировать");content.addView(gen);gen.setOnClickListener(v->generateImage());
        imageStatus=label("Ожидаю запрос.",12,accent);content.addView(imageStatus);
        imagePreview=label("Здесь появится результат.",12,muted);content.addView(imagePreview);
        Button save=btn("Сохранить изображение");content.addView(save);save.setOnClickListener(v->saveImage());
        Button pick=btn("Подключить локальную модель");content.addView(pick);pick.setOnClickListener(v->pickModel());
        modelStatus=label(selectedModel.isEmpty()?"Локальная модель не выбрана.":"Файл: "+selectedModel,12,muted);content.addView(modelStatus);
        if(lastImage!=null){ImageView iv=new ImageView(this);iv.setImageBitmap(lastImage);iv.setAdjustViewBounds(true);content.addView(iv);}
    }
    void generateImage(){
        String prompt=promptInput.getText().toString().trim();if(prompt.isEmpty()){imageStatus.setText("Сначала напиши описание.");return;}
        String key=prefs.getString("api_key",""),base=prefs.getString("base","https://api.openai.com/v1"),model=prefs.getString("image_model","gpt-image-1");
        if(key.isEmpty()){imageStatus.setText("Добавь API-ключ в настройках.");return;}
        imageStatus.setText("Генерирую…");
        io.execute(()->{
            try{
                JSONObject body=new JSONObject();body.put("model",model);body.put("prompt",prompt);body.put("size","1024x1024");body.put("n",1);
                JSONObject result=postJson(base+"/images/generations",key,body);
                JSONObject item=result.getJSONArray("data").getJSONObject(0);
                byte[] bytes;
                if(item.has("b64_json"))bytes=Base64.decode(item.getString("b64_json"),Base64.DEFAULT);
                else if(item.has("url")){HttpURLConnection c=(HttpURLConnection)new URL(item.getString("url")).openConnection();c.setConnectTimeout(20000);c.setReadTimeout(60000);try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[8192];int n;while((n=in.read(buf))>0)out.write(buf,0,n);bytes=out.toByteArray();}finally{c.disconnect();}}
                else throw new Exception("API не вернул изображение");
                android.graphics.Bitmap bmp=BitmapFactory.decodeByteArray(bytes,0,bytes.length);if(bmp==null)throw new Exception("Не удалось декодировать изображение");
                lastImage=bmp;runOnUiThread(()->{imageStatus.setText("Готово. Изображение создано.");imagePreview.setText("Результат готов — нажми «Сохранить изображение».");show("images");});
            }catch(Exception e){runOnUiThread(()->imageStatus.setText("Ошибка генерации: "+e.getMessage()));}
        });
    }
    void saveImage(){
        if(lastImage==null){status.setText("Сначала создай изображение.");return;}
        try{
            String name="Kiwi_"+System.currentTimeMillis()+".png";
            android.content.ContentValues values=new android.content.ContentValues();values.put(MediaStore.Images.Media.DISPLAY_NAME,name);values.put(MediaStore.Images.Media.MIME_TYPE,"image/png");values.put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/KiwiUraga");
            Uri uri=getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
            try(OutputStream out=getContentResolver().openOutputStream(uri)){lastImage.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}
            status.setText("Сохранено в Pictures/KiwiUraga/"+name);
        }catch(Exception e){status.setText("Не удалось сохранить: "+e.getMessage());}
    }
    void showLibrary(){
        heading("Локальная база знаний");
        TextView intro=label("Твоя библиотека знаний — статьи и заметки, которые Киви сможет использовать даже офлайн.",13,muted);intro.setPadding(dp(3),0,dp(3),dp(12));content.addView(intro);
        urlInput=field("https://example.com/article","");content.addView(urlInput);
        Button load=btn("Загрузить страницу");content.addView(load);load.setOnClickListener(v->loadSite());
        libraryBox=new LinearLayout(this);libraryBox.setOrientation(1);content.addView(libraryBox);refreshLibrary();
        reader.setVisibility(View.VISIBLE);
        content.addView(reader,new LinearLayout.LayoutParams(1,1));
    }
    void loadSite(){
        String u=urlInput.getText().toString().trim();if(!(u.startsWith("https://")||u.startsWith("http://"))){status.setText("Нужна полная ссылка с https://");return;}
        status.setText("Загружаю сайт…");reader.loadUrl(u);
    }
    void saveExtracted(String encoded){
        try{
            Object dec=new JSONTokener(encoded).nextValue();if(!(dec instanceof String))throw new Exception("Ответ страницы пустой");
            JSONObject d=new JSONObject((String)dec);String title=d.optString("title","Без названия"),url=d.optString("url",""),body=d.optString("text","");
            body=body.replaceAll("[\\t\\x0B\\f\\r]+"," ").replaceAll(" {2,}"," ").trim();if(body.length()>500000)body=body.substring(0,500000);
            if(body.length()<40){status.setText("Мало текста: сайт может требовать вход или блокировать чтение.");return;}
            File dir=new File(getFilesDir(),"knowledge");dir.mkdirs();String stamp=new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(new Date());
            String safe=title.replaceAll("[^\\p{L}\\p{N}._-]+","_");File f=new File(dir,stamp+"_"+safe+".txt");
            try(FileWriter w=new FileWriter(f)){w.write(title+"\n"+url+"\n\n"+body);}
            status.setText("Сохранено: "+f.getName());refreshLibrary();
        }catch(Exception e){status.setText("Не удалось сохранить страницу: "+e.getMessage());}
    }
    void refreshLibrary(){
        if(libraryBox==null)return;libraryBox.removeAllViews();File dir=new File(getFilesDir(),"knowledge");File[] files=dir.listFiles();
        if(files==null||files.length==0){libraryBox.addView(label("Материалов пока нет.",13,muted));return;}
        Arrays.sort(files,Collections.reverseOrder());
        for(File f:files){Button b=btn(f.getName());libraryBox.addView(b);b.setOnClickListener(v->{try{byte[] data=readFile(f);new android.app.AlertDialog.Builder(this).setTitle(f.getName()).setMessage(new String(data,"UTF-8")).setPositiveButton("Закрыть",null).show();}catch(Exception e){status.setText("Ошибка чтения.");}});}
    }
    byte[] readFile(File f)throws Exception{try(FileInputStream in=new FileInputStream(f);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))>0)out.write(b,0,n);return out.toByteArray();}}
    String searchKnowledge(String q){
        File dir=new File(getFilesDir(),"knowledge");File[] files=dir.listFiles();if(files==null)return "";
        String[] terms=q.toLowerCase(Locale.ROOT).split("\\W+");StringBuilder result=new StringBuilder();
        for(File f:files){try{String s=new String(readFile(f),"UTF-8");String lower=s.toLowerCase(Locale.ROOT);int score=0;for(String term:terms)if(term.length()>2&&lower.contains(term))score++;
            if(score>0){result.append(s.substring(0,Math.min(s.length(),2500))).append("\n---\n");if(result.length()>7000)break;}
        }catch(Exception ignored){}}
        return result.toString();
    }
    void showSettings(){
        heading("Настройки подключения");
        TextView intro=label("Настрой подключение и выбери модели. API-ключ хранится в приватных настройках приложения.",13,muted);intro.setPadding(dp(3),0,dp(3),dp(12));content.addView(intro);
        baseInput=field("API base URL",prefs.getString("base","https://api.openai.com/v1"));content.addView(baseInput);
        keyInput=field("API key",prefs.getString("api_key",""));keyInput.setInputType( android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);content.addView(keyInput);
        chatModelInput=field("Chat model",prefs.getString("chat_model","gpt-4o-mini"));content.addView(chatModelInput);
        imageModelInput=field("Image model",prefs.getString("image_model","gpt-image-1"));content.addView(imageModelInput);
        Button save=btn("Сохранить подключения");content.addView(save);save.setOnClickListener(v->{
            String base=baseInput.getText().toString().trim();while(base.endsWith("/"))base=base.substring(0,base.length()-1);
            prefs.edit().putString("base",base).putString("api_key",keyInput.getText().toString().trim()).putString("chat_model",chatModelInput.getText().toString().trim()).putString("image_model",imageModelInput.getText().toString().trim()).apply();
            status.setText("Настройки сохранены.");new android.app.AlertDialog.Builder(this).setMessage("Подключения сохранены. Проверь, что провайдер поддерживает /chat/completions и /images/generations.").setPositiveButton("ОК",null).show();
        });
        Button test=btn("Проверить API");content.addView(test);test.setOnClickListener(v->testApi());
        gap(content,14);heading("Локальная модель");
        modelStatus=label(selectedModel.isEmpty()?"Файл модели не выбран.":"Выбран файл: "+selectedModel,12,muted);content.addView(modelStatus);
        Button pick=btn("Выбрать файл модели");content.addView(pick);pick.setOnClickListener(v->pickModel());
        content.addView(label("Импорт файла пока сохраняет модель в хранилище приложения. Для реального локального запуска нужна поддержка конкретного формата и архитектуры модели.",12,muted));
    }
    void testApi(){
        String base=baseInput.getText().toString().trim(),key=keyInput.getText().toString().trim(),model=chatModelInput.getText().toString().trim();
        status.setText("Проверяю API…");io.execute(()->{try{JSONObject b=new JSONObject();b.put("model",model);b.put("messages",new JSONArray().put(new JSONObject().put("role","user").put("content","Ответь одним словом: работает")));JSONObject r=postJson(base.replaceAll("/+$","")+"/chat/completions",key,b);String answer=r.getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content","OK");runOnUiThread(()->status.setText("API работает: "+answer));}catch(Exception e){runOnUiThread(()->status.setText("API недоступен: "+e.getMessage()));}});
    }
    JSONObject postJson(String endpoint,String key,JSONObject body)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(endpoint).openConnection();c.setRequestMethod("POST");c.setConnectTimeout(20000);c.setReadTimeout(120000);c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("Authorization","Bearer "+key);
        try(OutputStream o=c.getOutputStream()){o.write(body.toString().getBytes("UTF-8"));}
        int code=c.getResponseCode();InputStream stream=code>=200&&code<300?c.getInputStream():c.getErrorStream();String response;
        try(InputStream in=stream;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))>0)out.write(b,0,n);response=new String(out.toByteArray(),"UTF-8");}
        c.disconnect();if(code<200||code>=300)throw new Exception("HTTP "+code+": "+response.substring(0,Math.min(500,response.length())));
        return new JSONObject(response);
    }
    void pickModel(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK_MODEL);
    }
    @Override protected void onActivityResult(int req,int res,Intent data){
        super.onActivityResult(req,res,data);if(req!=PICK_MODEL||res!=RESULT_OK||data==null)return;
        Uri uri=data.getData();try{
            String name="model.bin";try(Cursor c=getContentResolver().query(uri,null,null,null,null)){if(c!=null&&c.moveToFirst()){int ix=c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);if(ix>=0)name=c.getString(ix);}}
            File dir=new File(getFilesDir(),"models");dir.mkdirs();File dest=new File(dir,name);
            try(InputStream in=getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(dest)){byte[] b=new byte[8192];int n;while((n=in.read(b))>0)out.write(b,0,n);}
            selectedModel=name;if(modelStatus!=null)modelStatus.setText("Файл сохранён: "+name+" ("+dest.length()/1024+" KB). Исполнение зависит от формата модели.");
            status.setText("Локальная модель импортирована.");
        }catch(Exception e){status.setText("Не удалось импортировать модель: "+e.getMessage());}
    }
    @Override protected void onDestroy(){io.shutdownNow();if(reader!=null)reader.destroy();super.onDestroy();}
}