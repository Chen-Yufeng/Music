package com.ifchan.music.helper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.ifchan.music.entity.Music;

/**
 * Created by daily on 11/14/17.
 */

public class DataBaseHelper extends SQLiteOpenHelper {
    private static final String NAME = "Musics.db";
    private static int VERSION = 1;
    private static DataBaseHelper sDataBaseHelper = null;
    private final String TAG = "@vir DBHelper";

    public static final String CREATE_BOOK = "create table Musics(" +
            "id integer primary key" +
            ",name text," +
            "singer text," +
            "path text)";

    public DataBaseHelper(Context context) {
        super(context, NAME, null, VERSION);
    }

    public static DataBaseHelper getInstance(Context context) {
        if (sDataBaseHelper == null) {
            sDataBaseHelper = new DataBaseHelper(context);
        }
        return sDataBaseHelper;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_BOOK);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void addMusic(int position, Music music, SQLiteDatabase db) {
        db.execSQL("insert into Musics (id, name, singer, path) values(?, ?, ?, ?)"
                , new String[]{Integer.toString(position), music.getName(), music.getSinger(),
                        music.getPath()});
    }

    public void delMusic(int position, SQLiteDatabase db) {
        db.execSQL("delete from Musics where id = ?", new String[]{Integer.toString(position)});
        //后续要优化（触发器？？？）
        db.execSQL("update Musics set id=id-1 where id > ?", new String[]{Integer.toString
                (position)});
    }

    public void updateMusic(int position, Music thisMusic, SQLiteDatabase db) {
        db.beginTransaction();
        db.execSQL("update Musics set name = ? where id = ?", new String[]{thisMusic.getName(),
                Integer.toString(position)});
        db.execSQL("update Musics set singer = ? where id = ?", new String[]{thisMusic.getSinger()
                , Integer.toString(position)});
        db.execSQL("update Musics set path = ? where id = ?", new String[]{thisMusic.getPath(),
                Integer.toString(position)});
        db.setTransactionSuccessful();
        db.endTransaction();
    }


    public void removeAllColumns(SQLiteDatabase db) {
        db.delete("Musics", null, null);
    }

}