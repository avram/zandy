package com.gimranov.zandy.client.data;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.database.Cursor;
import android.util.Log;

public class Attachment {

	public String key;
	public String parentKey;
	public String etag;
	public String status;
	public String dbId;
	public String title;
	public String filename;
	public String url;
	public JSONObject content;
	
	public static Database db;
	
	private static final String TAG = "com.gimranov.zandy.client.data.Child";
	
	public static final String ZFS_AVAILABLE = "Available for download";
	public static final String ZFS_LOCAL = "Downloaded";	
	public static final String UNKNOWN = "Status unknown";	

	public Attachment () {
		parentKey = title = filename = url = status = etag = "";
		content = new JSONObject();
	}
	
	public void save() {
		Attachment existing = load(key);
		if (dbId == null && existing == null) {
			String[] args = { key, parentKey, title, filename, url, status, etag, content.toString() };
			Cursor cur = db
					.rawQuery(
							"insert into attachments (attachment_key, item_key, title, filename, url, status, etag, content) "
									+ "values (?, ?, ?, ?, ?, ?, ?, ?)",
							args);
			if (cur != null)
				cur.close();
			Attachment fromDB = load(key);
			dbId = fromDB.dbId;
		} else {
			if (dbId == null)
				dbId = existing.dbId;
			String[] args = { key, parentKey, title, filename, url, status, etag, content.toString(), dbId };
			Log.i(TAG, "Updating existing attachment");
			Cursor cur = db
					.rawQuery(
							"update attachments set attachment_key=?, item_key=?, title=?," +
							" filename=?, url=?, status=?, etag=?," +
							" content=? "
									+ " where _id=?", args);
			if (cur != null)
				cur.close();
		}
	}

	public static Attachment load(Cursor cur) {
		if (cur == null) return null;
		
		Attachment a = new Attachment();
		a.dbId = cur.getString(0);
		a.key = cur.getString(1);
		a.parentKey = cur.getString(2);
		a.title = cur.getString(3);
		a.filename = cur.getString(4);
		a.url = cur.getString(5);
		a.status = cur.getString(6);
		a.etag = cur.getString(7);
		try {
			a.content = new JSONObject(cur.getString(8));
		} catch (JSONException e) {
			Log.e(TAG, "Caught JSON exception loading attachment from db", e);
		}
		return a;
	}
	
	public static Attachment load(String key) {
		String[] cols = { "_id", "attachment_key", "item_key", "title", "filename", "url", "status", "etag", "content" };
		String[] args = { key };
		Cursor cur = db.query("attachments", cols, "attachment_key=?", args, null, null, null, null);
		Attachment a = load(cur);
		if (cur != null) cur.close();
		return a;
	}
	
	/**
	 * Provides ArrayList of Attachments for a given Item
	 * Useful for powering UI
	 * 
	 * We can also use this to trigger background syncs
	 * 
	 * @param item
	 * @return
	 */
	public static ArrayList<Attachment> forItem(Item item) {
		ArrayList<Attachment> list = new ArrayList<Attachment>();
		
		if (item.dbId == null) item.save();
		Log.d(TAG, "Looking for the kids of an item with key: "+item.getKey());
			
		String[] cols = { "_id", "attachment_key", "item_key", "title", "filename", "url", "status", "etag", "content" };
		String[] args = { item.getKey() };
		Cursor cursor = db.query("attachments", cols, "item_key=?", args, null, null, null, null);
		
		if (cursor != null) {
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				Attachment a = Attachment.load(cursor);
				Log.d(TAG,"Found attachment: "+a.title);
				list.add(a);
				cursor.moveToNext();
			}
			cursor.close();
		} else {
			Log.d(TAG,"Cursor was null, so we still didn't get attachments for the item!");
		}
		
		return list;
	}
	
}
