package com.example.chrome1.sqliteHelper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

//数据库
public class MySqliteHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "FastGathering.db";  // 数据库名称
    public static final int DATABASE_VERSION = 1;          //数据库版本

    public MySqliteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //记录上传记录
        db.execSQL("create table records(id integer primary key autoincrement,phone text,send_phone text,context text,time TIMESTAMP default CURRENT_TIMESTAMP)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
