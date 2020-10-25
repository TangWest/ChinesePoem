package com.tang.chinesepoem;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements Runnable{

    private String TAG = "MainActivity";
    TextView tv1;
    TextView tv2;
    ListView lv;
    SimpleAdapter listItemAdapter;
    ArrayList<HashMap<String, String>> listItems;
    String href = "/chaxun/list/23309.html";
    private int id = 23309;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            finish();
            return;
        }
        setContentView(R.layout.activity_main);
        tv1 = findViewById(R.id.main_text_1);
        tv2 = findViewById(R.id.main_text_2);
        lv = (ListView) findViewById(R.id.list_2);

        tv1.setMovementMethod(ScrollingMovementMethod.getInstance());
        tv2.setMovementMethod(ScrollingMovementMethod.getInstance());

        //获得href及id
        Intent intent = getIntent();
        if(intent!=null){
            Bundle bundle = intent.getExtras();
            if(bundle!=null){
                href = bundle.getString("href");
                if(href!=null){
                    String idstr = href.replaceAll("[^\\d]","");
                    id = Integer.parseInt(idstr);
                    Log.i(TAG,"id from bundle "+id);
                }else{
                    id = 23309;
                    href = "/chaxun/list/23309.html";
                    Log.i(TAG,"id default "+id+" href default "+href);
                }
            }
        }

        //隐藏标题栏
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
        {
            actionBar.hide();
        }

        Thread thread = new Thread(this);
        thread.start();

        //跳转页面
        final FloatingActionButton fb1 = (FloatingActionButton) findViewById(R.id.fbutton_1);
        fb1.setOnClickListener(new View.OnClickListener() {
            Intent intent = new Intent();
            @Override
            public void onClick(View view) {
                intent.setClass(MainActivity.this, ListActivity.class);
                startActivity(intent);
            }
        });

        //发送笔记
        Button btn = (Button) findViewById(R.id.button_1);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addOneNote();
            }
        });


        //长按笔记
        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long longid) {
                //数据库删除
                Object itemAtPosition = lv.getItemAtPosition(position);
                HashMap<String,String> map = (HashMap<String,String>)itemAtPosition;
                String itemtime = map.get("note_time");
                delNote(id,itemtime);
                //视图删除
                listItems.remove(position);
                listItemAdapter.notifyDataSetChanged();
                return true;//长按之后不执行点击事件
            }
        });
    }

    Handler handler = new Handler(){
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0) {
                String str = (String) msg.obj;
                tv1.setText(Html.fromHtml(str));
            }
            if(msg.what == 1) {
                String str = (String) msg.obj;
                if(str.length()>0){
                    tv2.setText(Html.fromHtml(str));
                }else {
                    tv2.setText("暂无赏析。");
                }
            }
            if(msg.what == 2) {
                //布局显示
                String[] data = (String[]) msg.obj;
                listItems = new ArrayList<HashMap<String, String>>();
                for(int i = 0;i<data.length;i++){
                    String[] splitdata = data[i].split("#");

                    HashMap<String,String> map = new HashMap<String,String>();
                    map.put("note_content",splitdata[0]);
                    map.put("note_time",splitdata[1]);
                    listItems.add(map);
                }
                listItemAdapter = new SimpleAdapter(MainActivity.this,
                        listItems,
                        R.layout.list_notes,
                        new String[]{"note_content", "note_time"},
                        new int[]{R.id.note_content, R.id.note_time}
                );
                lv.setAdapter(listItemAdapter);
            }
        }
    };

    public void run(){
        new Thread(new Runnable(){
            @Override
            public void run() {
                showPoemDetail(href);
            }
        }).start();

    }

    private  void showPoemDetail(String h){
        URL url = null;
        InputStream in = null;
        try{
            String poemurl = "https://www.shicimingju.com"+h;
            Log.i(TAG,poemurl);

            //使用http.getInputStream()方法
            url = new URL(poemurl);
            HttpURLConnection http = (HttpURLConnection)url.openConnection();
            in = http.getInputStream();
            String html = inputStream2String(in);

//            //直接用Jsoup.parse()
//            Document htmldoc = Jsoup.parse(new URL(poemurl).openStream(), "UTF-8", poemurl); //非GBK GB2312编码
//            String html = htmldoc.text();

//            Log.i(TAG,"html "+html);
            showPoem(html);
            showDigest(html);
            showNotes();
        }catch (MalformedURLException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private String inputStream2String(InputStream inputStream) throws IOException {
        final int bufferSize = 1024;
        final char[] buffer = new char[bufferSize];
        final StringBuilder out = new StringBuilder();
        Reader in = new InputStreamReader(inputStream,"UTF-8");
        while (true){
            int rsz = in.read(buffer,0,buffer.length);
            if(rsz < 0)
                break;
            out.append(buffer,0,rsz);
        }
        inputStream.close();
        return out.toString();
    }

    private void showPoem(String html) throws UnsupportedEncodingException {
        Document doc = Jsoup.parse(html);
        Elements title = doc.select("h1#zs_title");
        Elements nz = doc.getElementsByClass("niandai_zuozhe");
        Elements contents = doc.getElementsByClass("item_content").select("div");

        String titlestr = title.text();
        String nzstr = nz.text();
        //内容逢句号等 换行
        String contentsstr = contents.text();
        String[] s= {"。","？","！","；"};
        for(int i=0;i<s.length;i++){
            contentsstr = contentsstr.replace(s[i], s[i]+"<br>");
        }

        String poemstr = "<font size='1' style='font-family:SimSun'><big><b>"+titlestr+"</b></big></font><br>"
                +"<font size='2' style='font-family:SimSun'><b>"+nzstr+"</b></font><br>"
                +"<font size='3' style='font-family:SimSun'><big><b>"+contentsstr+"</b></big></font>";
        Message msg = handler.obtainMessage(0);
        msg.obj = poemstr;
        handler.sendMessage(msg);
    }
    private void showDigest(String html) throws UnsupportedEncodingException {
        Document doc = Jsoup.parse(html);
        Elements digest = doc.getElementsByClass("shangxi_content");

        String digeststr = digest.text();
        Message msg = handler.obtainMessage(1);
        msg.obj = digeststr;
        handler.sendMessage(msg);
    }

    //展示笔记
    private void showNotes(){
        List<String> data = new ArrayList<String>();
        NotesManager notesManager = new NotesManager(MainActivity.this);
        for(NotesItem notesItem : notesManager.listAll(id)){
            data.add(notesItem.getContent() + "#" + notesItem.getTime());
        }
        String[] datastr = data.toArray(new String[]{});

        Message msg = handler.obtainMessage(2);
        msg.obj = datastr;
        handler.sendMessage(msg);
    }

    //添加笔记
    private void addOneNote(){
        //获取当前时间
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());
        String time = date.toString();
        //获取笔记内容
        TextView et = (TextView) findViewById(R.id.main_edittext_1);
        String content = et.getText().toString();

        NotesManager notesManager = new NotesManager(MainActivity.this);
        NotesItem notesItem = new NotesItem(id,time,content);
        notesManager.add(notesItem);

        et.setText("");
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
        showNotes();
    }

    private void delNote(int noteid,String time){
        NotesManager notesManager = new NotesManager(MainActivity.this);
        notesManager.delete(noteid,time);
    }
}