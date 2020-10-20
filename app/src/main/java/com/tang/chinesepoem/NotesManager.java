package com.tang.chinesepoem;

import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class NotesManager {

    private DBHelper dbHelper;
    private String tb_name;
    private String TAG = "NotesManager";

    public NotesManager(Context context){
        dbHelper = new DBHelper(context);
        tb_name = DBHelper.TB_NAME;
    }

    public void add(NotesItem item){
        Log.i(TAG,"add "+item.getId()+" "+item.getTime()+" "+item.getContent());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", item.getId());
        values.put("time", item.getTime());
        values.put("content", item.getContent());
        db.insert(tb_name, null, values);
        db.close();
    }

    public void delete(int id,String time){
        Log.i(TAG,"delete "+id+" "+time);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(tb_name, "ID=? AND TIME=?", new String[]{String.valueOf(id),time});
        db.close();
    }

    public List<NotesItem> listAll(int id){
        List<NotesItem> notesList = null;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(tb_name, null, "ID="+id, null, null, null, "TIME DESC");
        if(cursor!=null){
            notesList = new ArrayList<NotesItem>();
            while(cursor.moveToNext()){
                NotesItem item = new NotesItem();
                item.setId(cursor.getInt(cursor.getColumnIndex("ID")));
                item.setTime(cursor.getString(cursor.getColumnIndex("TIME")));
                item.setContent(cursor.getString(cursor.getColumnIndex("CONTENT")));

                notesList.add(item);
            }
            cursor.close();
        }
        db.close();
        return notesList;

    }
}
