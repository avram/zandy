package com.gimranov.zandy.client.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

public class Database {
	private static final String TAG = "com.gimranov.zandy.client.data.Database";
	
	public static final String[] ITEMCOLS = {"item_title", "item_type", "item_content", "etag", "dirty", "_id", "item_key", "item_year", "item_creator", "timestamp", "children"};
	public static final String[] COLLCOLS = {"collection_name", "collection_parent", "etag", "dirty", "_id", "collection_key, collection_size", "timestamp"};
	// the database version; increment to call update
	private static final int DATABASE_VERSION = 17;
	
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
			cursor.close();
			return null;
		}
		return cursor;
	}
	
	public Cursor query(String table, String[] columns, String selection,
			String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
		SQLiteDatabase db = mDatabaseOpenHelper.getWritableDatabase();
		Cursor cursor = db.query(table, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
		if (cursor == null) {
			return null;
		} else if (!cursor.moveToFirst()) {
			cursor.close();
			return null;
		}
		return cursor;
	}
		
	public Cursor rawQuery(String selection, String[] args) {
		Log.d(TAG, "Query: "+selection);
		SQLiteDatabase db = mDatabaseOpenHelper.getWritableDatabase();
		Cursor cursor = db.rawQuery(selection, args);
		if (cursor == null) {
			Log.d(TAG, "Null cursor.");
			return null;
		} else if (!cursor.moveToFirst()) {
			Log.d(TAG, "Cursor can't be moved");
			cursor.close();
			return null;
		}
		return cursor;
	}
	
	public SQLiteDatabase beginTransaction() {
		SQLiteDatabase db = mDatabaseOpenHelper.getWritableDatabase();
		db.beginTransaction();
		return db;
	}
		
	public void close() {
		mDatabaseOpenHelper.close();
	}
	
	public SQLiteStatement compileStatement(String sql) throws SQLiteException {
		SQLiteDatabase db = mDatabaseOpenHelper.getWritableDatabase();
		return db.compileStatement(sql);
	}
	
	private static class DatabaseOpenHelper extends SQLiteOpenHelper {
		private final Context mHelperContext;
		private SQLiteDatabase mDatabase;
		
		// table creation statements
		// for temp table creation to work, must have (_id as first field
		private static final String COLLECTIONS_CREATE =
			"create table collections"+ 
			" (_id integer primary key autoincrement, " +
			"collection_name text not null, " +
			"collection_key string unique, " +
			"collection_parent string, " +
			"collection_type text, " +
			"collection_size int, " +
			"etag string, " +
			"dirty string, " +
			"timestamp string);";

		private static final String ITEMS_CREATE =
			"create table items"+ 
			" (_id integer primary key autoincrement, " +
			"item_key string unique, " +
			"item_title string not null, " +
			"etag string, " +
			"item_type string not null, " +
			"item_content string," +
			"item_year string," +
			"item_creator string," +
			"item_children string," +
			"dirty string, " +
			"timestamp string);";
		
		private static final String CREATORS_CREATE =
			"create table creators"+ 
			" (_id integer primary key autoincrement, " +
			"name string, " +
			"firstName string, " +
			"lastName string, " +
			"creatorType string );";

		private static final String CHILDREN_CREATE =
			"create table children"+ 
			" (_id integer primary key autoincrement, " +
			"type string, " +
			"content string, " +
			"key string );";
		
		private static final String ITEM_TO_CREATORS_CREATE =
			"create table itemtocreators"+ 
			" (_id integer primary key autoincrement, "
			+ "creator_id int not null, item_id int not null);";
		
		private static final String ITEM_TO_COLLECTIONS_CREATE =
			"create table itemtocollections"+ 
			" (_id integer primary key autoincrement, "
			+ "collection_id int not null, item_id int not null);";

		private static final String DELETED_ITEMS_CREATE =
			"create table deleteditems"+ 
			" (_id integer primary key autoincrement, "
			+ "item_key string not null, etag string not null);";

		private static final String ATTACHMENTS_CREATE =
			"create table attachments"+ 
			" (_id integer primary key autoincrement, "
			+ "item_key string not null, "
			+ "attachment_key string not null, "
			+ "title string, "
			+ "filename string, "
			+ "url string, "
			+ "status string);";

		private static final String NOTES_CREATE =
			"create table notes"+ 
			" (_id integer primary key autoincrement, "
			+ "item_key string, "
			+ "note_key string not null, "
			+ "title string, "
			+ "filename string, "
			+ "url string, "
			+ "status string);";
		
		DatabaseOpenHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			mHelperContext = context;
		}

		@Override
		public void onCreate(SQLiteDatabase db) 
		{
			db.execSQL(COLLECTIONS_CREATE);
			db.execSQL(ITEMS_CREATE);
			db.execSQL(CREATORS_CREATE);
			db.execSQL(CHILDREN_CREATE);
			db.execSQL(ITEM_TO_CREATORS_CREATE);
			db.execSQL(ITEM_TO_COLLECTIONS_CREATE);	
			db.execSQL(DELETED_ITEMS_CREATE);
			db.execSQL(ATTACHMENTS_CREATE);
			db.execSQL(NOTES_CREATE);
		}
		
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, 
				int newVersion) {
			if (oldVersion < 14) {
				Log.w(TAG, 
						"Upgrading database from version " + 
						oldVersion + " to " + newVersion + 
				", which will destroy all old data.");
				db.execSQL("DROP TABLE IF EXISTS collections");
				db.execSQL("DROP TABLE IF EXISTS items");
				db.execSQL("DROP TABLE IF EXISTS creators");
				db.execSQL("DROP TABLE IF EXISTS children");
				db.execSQL("DROP TABLE IF EXISTS itemtocreators");
				db.execSQL("DROP TABLE IF EXISTS itemtocollections");
				onCreate(db);
			} else {
				if (oldVersion == 14 && newVersion == 15) {
					// here, we just added a table
					db.execSQL(DELETED_ITEMS_CREATE);
				}
				if (oldVersion == 15 && newVersion > 15) {
					// here, we just added a table
					db.execSQL("create table if not exists deleteditems"+ 
							" (_id integer primary key autoincrement, "
							+ "item_key int not null, etag int not null);");
				}
				if (oldVersion < 17 && newVersion == 17) {
					db.execSQL(ATTACHMENTS_CREATE);
					db.execSQL(NOTES_CREATE);
					db.execSQL("alter table items "+ 
							" add column children string;");
				}
			}
		}
	}
}
