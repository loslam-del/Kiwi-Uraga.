package com.kiwiuraga.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.*;
import android.net.Uri;
import android.graphics.BitmapFactory;
import android.graphics.Color;
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
    private android.graphics.Bitmap lastImage;
    private SharedPreferences prefs;
    private String selectedModel="";
    private int bg=Color.rgb(16,16,22), panel=Color.rgb(31,29,40), white=Color.WHITE, muted=0xffc8c3d2, accent=0xffbda5ff;

    int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    TextView label(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;}
    Button btn(String s){Button b=new Button(this);b.setText(s);return b;}
    EditText field(String hint,String value){EditText e=new EditText(this);e.setSingleLine(true);e.setHint(hint);e.setText(value);e.setTextColor(white);e.setHintTextColor(0xff888493);return e;}
    void gap(LinearLayout box,int h){View v=new View(this);box.addView(v,new LinearLayout.LayoutParams(1,dp(h)));}
    void card(LinearLayout parent,View child){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(8);child.setBackgroundColor(panel);child.setPadding(dp(12),dp(10),dp(12),dp(10));parent.addView(child,p);}
    @Override public void onCreate(Bundle b){
        super.onCreate(b); prefs=getSharedPreferences("kiwi",MODE_PRIVATE);
        getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);
        root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(16),dp(18),dp(16),dp(12));root.setBackgroundColor(bg);
        setContentView(root);
        TextView title=label("🥝 Kiwi Uraga",26,white);root.addView(title);
        TextView subtitle=label("HYBRID AI • LOCAL KNOWLEDGE • IMAGE LAB",10,accent);root.addView(subtitle);
        LinearLayout nav=new LinearLayout(this);nav.setOrientation(0);nav.setGravity(Gravity.CENTER);root.addView(nav);
        String[] names={"Чат","Фото","База","Настройки"};String[] ids={"chat","images","library","settings"};
        for(int i=0;i<names.length;i++){final String id=ids[i];Button btt=btn(names[i]);btt.setTextSize(11);nav.addView(btt,new LinearLayout.LayoutParams(0,-2,1));btt.setOnClickListener(v->show(id));}
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        content=new LinearLayout(this);content.setOrientation(1);sc.addView(content);
        status=label("Гибридный режим. Подключи API в настройках для облачного AI.",12,accent);root.addView(status);
        setupReader();show("chat");
    }
    void setupReader(){
        reader=new WebView(this);reader.getSettings().setJavaScriptEnabled(true);reader.getSettings().setDomStorageEnabled(true);reader.getSettings().setLoadsImagesAutomatically(false);
        reader.setWebViewClient(new WebViewClient(){
            @Override public void onPageFinished(WebView v,String url){if(!"about:blank".equals(url)){status.setText("Страница загружена, извлекаю текст…");v.evaluateJavascript("(function(){return JSON.stringify({title:document.title||location.hostname,url:location.href,text:document.body?document.body.innerText:''});})()",r->saveExtracted(r);}}
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
    void heading(String s){content.addView(label(s,20,white));gap(content,8);}
    void showChat(){
        heading("AI-ассистент");
        TextView hint=label("Онлайн: подключается к совместимому Chat Completions API. Офлайн: ищет ответы в твоих сохранённых материалах.",12,muted);content.addView(hint);
        chatBox=new LinearLayout(this);chatBox.setOrientation(1);content.addView(chatBox);
        String history=prefs.getString("history","");
        if(!history.isEmpty())for(String line:history.split("\\n\\n"))addMessage(line,false);
        chatInput=new EditText(this);chatInput.setHint("Напиши сообщение…");chatInput.setTextColor(white);chatInput.setHintTextColor(0xff888493);content.addView(chatInput);
        Button send=btn("Отправить");content.addView(send);send.setOnClickListener(v->sendChat());
        Button clear=btn("Очистить историю");content.addView(clear);clear.setOnClickListener(v->{prefs.edit().remove("history").apply();chatBox.removeAllViews();});
    }
    void addMessage(String s,boolean persist){
        TextView t=label(s,14,white);t.setPadding(dp(12),dp(10),dp(12),dp(10));t.setBackgroundColor(panel);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(6);chatBox.addView(t,p);
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
        content.addView(label("Генерация через подключённый Images API. Укажи модель и ключ в настройках.",12,muted));
        promptInput=new EditText(this);promptInput.setHint("Опиши изображение…");promptInput.setTextColor(white);promptInput.setHintTextColor(0xff888493);promptInput.setMinLines(3);promptInput.setGravity(Gravity.TOP);content.addView(promptInput);
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
        content.addView(label("Сохраняй статьи с сайтов. Материалы доступны офлайн и могут использоваться ассистентом как контекст.",12,muted));
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
        content.addView(label("Совместимый OpenAI-style API. Используй адрес и ключ своего провайдера. Ключ хранится в приватных настройках приложения.",12,muted));
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