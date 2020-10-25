package com.tang.chinesepoem;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ListActivity extends AppCompatActivity implements Runnable{
    private String TAG = "ListActivity";
    ListView lv;
    TextView tv;
    SimpleAdapter listItemAdapter;
    ArrayList<HashMap<String, String>> listItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        lv = (ListView) findViewById(R.id.list_1);
        tv = (TextView) findViewById(R.id.search_1);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
        {
            actionBar.hide();
        }

        Thread thread = new Thread(this);
        thread.start();

        //点击进入诗词详情
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object itemAtPosition = lv.getItemAtPosition(position);
                HashMap<String,String> map = (HashMap<String,String>)itemAtPosition;
                String href = map.get("href");

                Intent intent = new Intent(ListActivity.this,MainActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("href",href);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });

        //搜索
        Button btn = (Button) findViewById(R.id.button_2);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(TextUtils.isEmpty(tv.getText())){
                    Toast t = Toast.makeText(ListActivity.this,"请先输入要搜索的诗词",Toast.LENGTH_SHORT);
                    t.show();
                }else{
                    new Thread(new Runnable(){
                        @Override
                        public void run() {
                            String searchword = tv.getText().toString();
                            searchPoem(searchword);
                        }
                    }).start();
                }
            }
        });
    }

    Handler handler = new Handler(){
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0) {
                //布局显示
                String[] data = (String[]) msg.obj;
                listItems = new ArrayList<HashMap<String, String>>();
                for(int i = 0;i<data.length;i++){
                    String[] splitdata = data[i].split("#");

                    HashMap<String,String> map = new HashMap<String,String>();
                    map.put("item_title",splitdata[0]);
                    map.put("item_content",splitdata[1]);
                    map.put("href",splitdata[2]);
                    listItems.add(map);
                }
                listItemAdapter = new SimpleAdapter(ListActivity.this,
                        listItems,
                        R.layout.list_poems,
                        new String[]{"item_title", "item_content","href"},
                        new int[]{R.id.item_title, R.id.item_content,R.id.href}
                );
                lv.setAdapter(listItemAdapter);
                TextView emptv = (TextView) findViewById(R.id.noresult);
                TextView seatv = (TextView) findViewById(R.id.search_1);
                String sw = seatv.getText().toString();
                emptv.setText("抱歉，搜索"+sw+"暂无结果。");
                lv.setEmptyView(emptv);
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
            }
        }
    };

    public void run(){
        new Thread(new Runnable(){
            @Override
            public void run() {
                showList();
            }
        }).start();
    }

    private  void showList(){
        URL url = null;
        InputStream in = null;
        try{
            String poemurl = "https://www.shicimingju.com";
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
            useJsoup(html,1);
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

    private void useJsoup(String html,int mark){
        Document doc = Jsoup.parse(html);
        Elements cards = doc.select("div.card").select(".shici_card");
        Log.i(TAG,"card "+cards.size());

        List<String> data = new ArrayList<String>();
        for (int i=0;i<cards.size();i++){
            String href = null;
            Elements title = cards.get(i).getElementsByClass("shici_list_main").select("h3");
            Elements content = cards.get(i).getElementsByClass("shici_content");

            //content中去除"收起""展开全文"
            String contentstr = content.text();
            contentstr = contentstr.replace("收起","");
            contentstr = contentstr.replace("展开全文","");

            //首页和搜索界面href的获取略有不同
            if(mark==1){
                href = title.select("a").first().attr("href");
            }else if(mark==2){
                href = title.select("a").attr("href");
            }

            String item = null;
            if(title.text().length()>0&contentstr.length()>0&href.length()>0){//判空
                item = title.text()+"#"+contentstr+"#"+href;
                data.add(item);
            }
        }
        String[] datastr = data.toArray(new String[]{});

        Message msg = handler.obtainMessage(0);
        msg.obj = datastr;
        handler.sendMessage(msg);

    }

    private void searchPoem(String searchword){
        URL url = null;
        InputStream in = null;
        try{
            String poemurl = "https://www.shicimingju.com/chaxun/all/"+searchword;
            Log.i(TAG,poemurl);

            //使用http.getInputStream()方法
            url = new URL(poemurl);
            HttpURLConnection http = (HttpURLConnection)url.openConnection();
            in = http.getInputStream();
            String html = inputStream2String(in);

            useJsoup(html,2);

        }catch (MalformedURLException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
