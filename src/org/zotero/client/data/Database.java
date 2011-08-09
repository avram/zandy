package org.zotero.client.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;

public class Database {
	private static final String TAG = "org.zotero.client.data.Database";
	
	// the database version; increment to call update
	private static final int DATABASE_VERSION = 2;
	
	private static final String DATABASE_NAME = "Zotero";
	private final DatabaseOpenHelper mDatabaseOpenHelper;
	
	public Database(Context context) {
		mDatabaseOpenHelper = new DatabaseOpenHelper(context);
	}
	
	public Cursor query(String selection, String[] selectionArgs, String[] columns) {
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		Cursor cursor = builder.query(mDatabaseOpenHelper.getWritableDatabase(),
					columns, selection, selectionArgs, null, null, null);

		if (cursor == null) {
			return null;
		} else if (!cursor.moveToFirst()) {
			return null;
		}
		return cursor;
	}
	
	public Cursor rawQuery(String selection, String[] selectionArgs) {
		SQLiteDatabase db = mDatabaseOpenHelper.getWritableDatabase();
		Cursor cursor = db.rawQuery(selection, selectionArgs);

		if (cursor == null) {
			return null;
		} else if (!cursor.moveToFirst()) {
			cursor.close();
			return null;
		}
		return cursor;
	}
	
	public void close() {
		mDatabaseOpenHelper.close();
	}
	
	private static class DatabaseOpenHelper extends SQLiteOpenHelper {
		
		private final Context mHelperContext;
		private SQLiteDatabase mDatabase;
		
		// table creation statements
		// for temp table creation to work, must have (_id as first field
		private static final String COLLECTIONS_CREATE =
			"create table collections"+ 
			" (_id bigint primary key, "
			+ "collection_name text not null, collection_type text not null, collection_size int not null);";
		
		private static final String ITEMS_CREATE =
			"create table items"+ 
			" (_id bigint primary key, "
			+ "collection_id int not null, item_title string not null," +
					"item_type string not null, item_content string);";
		
		private static final String CREATORS_CREATE =
			"create table creators"+ 
			" (_id integer primary key autoincrement, "
			+ "creator_id string not null);";

		private static final String ITEM_TO_CREATORS_CREATE =
			"create table itemtocreators"+ 
			" (_id integer primary key autoincrement, "
			+ "creator_id int not null, item_id int not null);";
		
		private static final String ITEM_TO_COLLECTIONS_CREATE =
			"create table itemtocollections"+ 
			" (_id integer primary key autoincrement, "
			+ "collection_id int not null, item_id int not null);";
		
		DatabaseOpenHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			mHelperContext = context;
		}

		@Override
		public void onCreate(SQLiteDatabase db) 
		{
			mDatabase = db;
			db.execSQL(COLLECTIONS_CREATE);
			db.execSQL(ITEMS_CREATE);
			db.execSQL(CREATORS_CREATE);
			db.execSQL(ITEM_TO_CREATORS_CREATE);
			db.execSQL(ITEM_TO_COLLECTIONS_CREATE);	
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, 
				int newVersion) {
			Log.w(TAG, 
					"Upgrading database from version " + 
					oldVersion + " to " + newVersion + 
			", which will destroy all old data.");
			db.execSQL("DROP TABLE IF EXISTS collections");
			db.execSQL("DROP TABLE IF EXISTS items");
			db.execSQL("DROP TABLE IF EXISTS creators");
			db.execSQL("DROP TABLE IF EXISTS itemtocreators");
			db.execSQL("DROP TABLE IF EXISTS itemtocollections");
			onCreate(db);
		}
	}
}
