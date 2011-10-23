package com.gimranov.zandy.app.data;

import java.util.ArrayList;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import com.gimranov.zandy.app.task.APIRequest;

import android.content.Context;
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
	
	/**
	 * Queue of attachments that need to be synced, because they're dirty
	 */
	public static ArrayList<Attachment> queue;
	
	/**
	 * APIRequest.API_DIRTY means that we'll try to push this version up to the server
	 */
	public String dirty;
	
	/**
	 * Zotero's JSON format for attachment / child information
	 * 
	 * linkMode:	O = file attachment, in ZFS?
	 * 				1 = link attachment?
	 */
	public JSONObject content;
	
	public static Database db;
	
	private static final String TAG = "com.gimranov.zandy.app.data.Attachment";
	
	public static final String ZFS_AVAILABLE = "Available for download";
	public static final String ZFS_LOCAL = "Downloaded";	
	public static final String UNKNOWN = "Status unknown";	

	public Attachment () {
		if (queue == null) queue = new ArrayList<Attachment>();
		parentKey = title = filename = url = status = etag = dirty = "";
		content = new JSONObject();
	}
	
	public Attachment(Context c, String type, String parentKey) {
		this();
		content = new JSONObject();
		try {
			content.put("itemType", type);
		} catch (JSONException e) {
			Log.d(TAG,"JSON exception caught setting itemType in Attachment constructor", e);
		}
		key = UUID.randomUUID().toString();
		this.parentKey = parentKey;
		dirty = APIRequest.API_NEW;
	}
	
	public String getType () {
		String type = "";
		try {
			type = content.getString("itemType");
			if (type.equals("attachment"))
				type = content.getString("mimeType");
			if (type.equals("note"))
				type = "note";
		} catch (JSONException e) {
			Log.e(TAG, "JSON exception parsing attachment content",e);
		}
		return type;
	}
	
	public void setNoteText (String text) {
		if (getType().equals("note")) {
			try {
				content.put("note", text);
			} catch (JSONException e) {
				Log.e(TAG, "JSON exception setting note text",e);
			}
		}
	}
	
	public void save() {
		Attachment existing = load(key);
		if (dbId == null && existing == null) {
			Log.d(TAG, "Saving new, with status: "+status);
			String[] args = { key, parentKey, title, filename, url, status, etag, dirty, content.toString() };
			Cursor cur = db
					.rawQuery(
							"insert into attachments (attachment_key, item_key, title, filename, url, status, etag, dirty, content) "
									+ "values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
							args);
			if (cur != null)
				cur.close();
			Attachment fromDB = load(key);
			dbId = fromDB.dbId;
		} else {
			Log.d(TAG, "Saving new, with status: "+status);
			if (dbId == null)
				dbId = existing.dbId;
			String[] args = { key, parentKey, title, filename, url, status, etag, dirty, content.toString(), dbId };
			Log.i(TAG, "Updating existing attachment");
			Cursor cur = db
					.rawQuery(
							"update attachments set attachment_key=?, item_key=?, title=?," +
							" filename=?, url=?, status=?, etag=?, dirty=?, " +
							" content=? "
									+ " where _id=?", args);
			if (cur != null)
				cur.close();
		}
		db.close();
	}
	
	/**
	 * Identifies dirty items in the database and queues them for syncing
	 */
	public static void queue() {
		Log.d(TAG, "Clearing attachment dirty queue before repopulation");
		queue.clear();
		Attachment attachment;
		String[] cols = Database.ATTCOLS;
		String[] args = { APIRequest.API_CLEAN };
		Cursor cur = db.query("attachments", cols, "dirty != ?", args, null, null,
				null, null);

		if (cur == null) {
			Log.d(TAG, "No dirty attachments found in database");
			queue.clear();
			return;
		}

		do {
			Log.d(TAG, "Adding attachment to dirty queue");
			attachment = load(cur);
			queue.add(attachment);
		} while (cur.moveToNext() != false);

		if (cur != null)
			cur.close();
	}

	/**
	 * Get an Attachment from the current cursor position.
	 * 
	 * Does not close cursor.
	 * 
	 * @param cur
	 * @return
	 */
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
		a.dirty = cur.getString(8);
		try {
			a.content = new JSONObject(cur.getString(9));
		} catch (JSONException e) {
			Log.e(TAG, "Caught JSON exception loading attachment from db", e);
		}
		return a;
	}
	
	public static Attachment load(String key) {
		String[] cols = Database.ATTCOLS;
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
			
		String[] cols = { "_id", "attachment_key", "item_key", "title", "filename", "url", "status", "etag", "dirty", "content" };
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
