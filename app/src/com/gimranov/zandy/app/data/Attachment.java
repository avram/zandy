package com.gimranov.zandy.app.data;

import java.util.ArrayList;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.gimranov.zandy.app.task.APIRequest;

public class Attachment {

	public String key;
	public String parentKey;
	public String etag;
	public int status;
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
	
	private static final String TAG = "com.gimranov.zandy.app.data.Attachment";
	
	public static final int AVAILABLE = 1;
	public static final int LOCAL = 2;	
	public static final int UNKNOWN = 3;
	

	/* linkMode statuses from the Zotero client code
this.LINK_MODE_IMPORTED_FILE = 0;
this.LINK_MODE_IMPORTED_URL = 1;
this.LINK_MODE_LINKED_FILE = 2;
this.LINK_MODE_LINKED_URL = 3;
	 */
	
	public static final int MODE_LINKED_URL = 3;
	public static final int MODE_LINKED_FILE = 2;
	public static final int MODE_IMPORTED_URL = 1;
	public static final int MODE_IMPORTED_FILE = 0;

	public Attachment () {
		if (queue == null) queue = new ArrayList<Attachment>();
		parentKey = title = filename = url = etag = dirty = "";
		status = UNKNOWN;
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
		try {
			content.put("note", text);
		} catch (JSONException e) {
			Log.e(TAG, "JSON exception setting note text",e);
		}
	}
	
	public void save(Database db) {
		Attachment existing = load(key, db);
		if (dbId == null && existing == null) {
			Log.d(TAG, "Saving new, with status: "+status);
			String[] args = { key, parentKey, title, filename, url, Integer.toString(status), etag, dirty, content.toString() };
			Cursor cur = db
					.rawQuery(
							"insert into attachments (attachment_key, item_key, title, filename, url, status, etag, dirty, content) "
									+ "values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
							args);
			if (cur != null)
				cur.close();
			Attachment fromDB = load(key, db);
			dbId = fromDB.dbId;
		} else {
			Log.d(TAG, "Updating attachment, with status: "+status+" and fn: "+filename);
			if (dbId == null)
				dbId = existing.dbId;
			String[] args = { key, parentKey, title, filename, url, Integer.toString(status), etag, dirty, content.toString(), dbId };
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
	 * Deletes an attachment from the database, keeping a record of it in the deleteditems table
	 * We will then send out delete requests via the API to propagate the deletion
	 */
	public void delete(Database db) {
		String[] args = { dbId };
		db.rawQuery("delete from attachments where _id=?", args);
		// Don't prepare deletion requests for unsynced new attachments
		if (!APIRequest.API_NEW.equals(dirty)) {
			String[] args2 = { key, etag };
			db.rawQuery("insert into deleteditems (item_key, etag) values (?, ?)", args2);		
		}
	}
	
	/**
	 * Identifies dirty items in the database and queues them for syncing
	 */
	public static void queue(Database db) {
		if (queue == null) {
			// Initialize the queue if necessary
			queue = new ArrayList<Attachment>();
		}
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
		try {
			a.status = cur.getInt(6);
		} catch (Exception e) {
			a.status = UNKNOWN;
		}
		a.etag = cur.getString(7);
		a.dirty = cur.getString(8);
		try {
			a.content = new JSONObject(cur.getString(9));
		} catch (JSONException e) {
			Log.e(TAG, "Caught JSON exception loading attachment from db", e);
		}
		return a;
	}
	
	public static Attachment load(String key, Database db) {
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
	public static ArrayList<Attachment> forItem(Item item, Database db) {
		ArrayList<Attachment> list = new ArrayList<Attachment>();
		
		if (item.dbId == null) item.save(db);
		Log.d(TAG, "Looking for the kids of an item with key: "+item.getKey());
			
		String[] cols = { "_id", "attachment_key", "item_key", "title", "filename", "url", "status", "etag", "dirty", "content" };
		String[] args = { item.getKey() };
		Cursor cursor = db.query("attachments", cols, "item_key=?", args, null, null, null, null);
		
		if (cursor != null) {
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				Attachment a = Attachment.load(cursor);
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
