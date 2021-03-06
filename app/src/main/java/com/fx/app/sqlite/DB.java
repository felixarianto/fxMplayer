package com.fx.app.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DB extends SQLiteOpenHelper implements DatabaseErrorHandler {

    protected DB(Context context, String name, int version) {
        super(context, name, null, version, new DatabaseErrorHandler() {
            @Override
            public void onCorruption(SQLiteDatabase sqLiteDatabase) {
                Log.e("DB", "CORRUPT" + sqLiteDatabase.getPath());
            }
        });
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE FILE(" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                "path TEXT NOT NULL," +
                "last_update TEXT NOT NULL" +
                ")");

        db.execSQL("CREATE TABLE FILE_TRACK(" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                "file_id INTEGER NOT NULL," +
                "title TEXT NOT NULL," +
                "sq_no INTEGER NOT NULL," +
                "sc_time INTEGER NOT NULL," +
                "ec_time INTEGER NOT NULL" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int last, int current) {

    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        super.onDowngrade(db, oldVersion, newVersion);
    }

    @Override
    public void onCorruption(SQLiteDatabase sqLiteDatabase) {

    }

    /*
     * SINGLETON
     */

    protected static DB db;
    public static void create(Context context, String p_name, int p_version) {
        if (db == null) {
            db = new DB(context, p_name, p_version);
        }
    }

    public static long insert(String p_table, ContentValues p_values) {
        return db.getWritableDatabase().insertWithOnConflict(p_table, null, p_values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public static long update(String p_table, ContentValues p_values, String p_where) {
        return db.getWritableDatabase().update(p_table, p_values, p_where, null);
    }

    public static long delete(String p_table, String p_where) {
        return db.getWritableDatabase().delete(p_table, p_where, null);
    }

    public static Cursor query(String p_table, String[] p_columns, String p_where, String p_group, String p_order, String p_limit) {
        return db.getReadableDatabase().query(p_table, p_columns, p_where, null, null, null, p_order, p_limit);
    }

    public static Cursor query(String p_query) {
        return db.getReadableDatabase().rawQuery(p_query, null);
    }

}