package com.gimranov.zandy.app.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

public class Database {
	private static final String TAG = "com.gimranov.zandy.app.data.Database";
	
	public static final String[] ITEMCOLS = {"item_title", "item_type", 
		"item_content", "etag", "dirty", "_id", "item_key", "item_year", 
		"item_creator", "timestamp", "item_children"};
	public static final String[] COLLCOLS = {"collection_name", 
		"collection_parent", "etag", "dirty", "_id", "collection_key",
		"collection_size", "timestamp"};
	public static final String[] ATTCOLS = { "_id", "attachment_key",
		"item_key", "title", "filename", "url", "status", "etag",
		"dirty", "content" };
	public static final String[] REQUESTCOLS = {"_id", "uuid", "type",
		"query", "key", "method", "disposition", "if_match", "update_key",
		"update_type", "created", "last_attempt", "status", "body"};

	// the database version; increment to call update
	private static final int DATABASE_VERSION = 20;
	
	private static final String DATABASE_NAME = "Zotero";
	private final DatabaseOpenHelper mDatabaseOpenHelper;
	
	public Database(Context context) {
		mDatabaseOpenHelper = DatabaseOpenHelper.getHelper(context);
	}
	
	/**
	 * Deletes the entire contents of the database by dropping the tables and re-adding them
	 */
	public void resetAllData() {
		Log.d(TAG, "Dropping tables to reset database");
		String[] tables = {"collections", "items", "creators", "children", 
				"itemtocreators", "itemtocollections", "deleteditems", "attachments",
				"apirequests", "notes"};
		String[] args = {};
		for (int i = 0; i < tables.length; i++) {
			rawQuery("DROP TABLE IF EXISTS " + tables[i], args);
		}
		Log.d(TAG, "Recreating database tables");		
		SQLiteDatabase db = mDatabaseOpenHelper.getWritableDatabase();
		mDatabaseOpenHelper.onCreate(db);
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
			return null;
		} else if (!cursor.moveToFirst()) {
			cursor.close();
			return null;
		}
		return cursor;
	}
	
	public void beginTransaction() {
		SQLiteDatabase db = mDatabaseOpenHelper.getWritableDatabase();
		db.beginTransaction();
	}

	public void endTransaction() {
		SQLiteDatabase db = mDatabaseOpenHelper.getWritableDatabase();
		db.endTransaction();
	}

	public void setTransactionSuccessful() {
		SQLiteDatabase db = mDatabaseOpenHelper.getWritableDatabase();
		db.setTransactionSuccessful();
	}
	
	/**
	 * No-op.
	 */
	public void close() {
	}
	
	public SQLiteStatement compileStatement(String sql) throws SQLiteException {
		SQLiteDatabase db = mDatabaseOpenHelper.getWritableDatabase();
		return db.compileStatement(sql);
	}
	
	private static class DatabaseOpenHelper extends SQLiteOpenHelper {
		@SuppressWarnings("unused")
		private final Context mHelperContext;
		@SuppressWarnings("unused")
		private SQLiteDatabase mDatabase;
		
		private static DatabaseOpenHelper instance;
		
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
			+ "status string, "
			+ "content string, "
			+ "etag string, "
		    + "dirty string);";

		private static final String APIREQUESTS_CREATE =
				"create table apirequests"+ 
				" (_id integer primary key autoincrement, "
				+ "uuid string unique, "
				+ "type string, "
				+ "query string, "
				+ "key string, "
				+ "method string, "
				+ "disposition string, "
				+ "if_match string, "
				+ "update_key string, "
			    + "update_type string, "
			    + "created string, "
			    + "last_attempt string, "
			    + "status integer,"
			    + "body string);";
		
		/* We don't use this table right now */
		private static final String NOTES_CREATE =
			"create table notes"+ 
			" (_id integer primary key autoincrement, "
			+ "item_key string, "
			+ "note_key string not null, "
			+ "title string, "
			+ "filename string, "
			+ "url string, "
			+ "status string, "
			+ "content string, "
			+ "etag string);";
		
		DatabaseOpenHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			mHelperContext = context;
		}
		
		public static synchronized DatabaseOpenHelper getHelper(Context context) {
			if (instance == null)
				instance = new DatabaseOpenHelper(context);
			
			return instance;
		}

		@Override
		public void onCreate(SQLiteDatabase db) 
		{
			db.execSQL(COLLECTIONS_CREATE);
			db.execSQL(ITEMS_CREATE);
			db.execSQL(CREATORS_CREATE);
			db.execSQL(ITEM_TO_CREATORS_CREATE);
			db.execSQL(ITEM_TO_COLLECTIONS_CREATE);	
			db.execSQL(DELETED_ITEMS_CREATE);
			db.execSQL(ATTACHMENTS_CREATE);
			db.execSQL(NOTES_CREATE);
			db.execSQL(APIREQUESTS_CREATE);
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
							" add column item_children string;");
				}
				if (oldVersion == 17 && newVersion == 18) {
					db.execSQL("alter table attachments "+ 
							" add column etag string;");
					db.execSQL("alter table attachments "+ 
							" add column content string;");
					db.execSQL("alter table notes "+ 
							" add column etag string;");
					db.execSQL("alter table notes "+ 
							" add column content string;");
				}
				if (oldVersion == 18 && newVersion == 19) {
					db.execSQL("alter table attachments "+ 
							" add column dirty string;");
				}
				if (oldVersion == 19 && newVersion == 20) {
					db.execSQL(APIREQUESTS_CREATE);
				}
			}
		}
	}
}
