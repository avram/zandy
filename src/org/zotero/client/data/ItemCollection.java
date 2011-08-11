package org.zotero.client.data;

import java.util.ArrayList;

import org.zotero.client.task.APIRequest;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
/**
 * Represents a Zotero collection of Item objects. Collections can
 * be iterated over, but it's quite likely that iteration will prove unstable
 * if you make changes to them while iterating.
 * 
 * To put items in collections, add them using the add(..) methods, and save the
 * collection.
 * 
 * @author ajlyon
 *
 */
public class ItemCollection extends ArrayList<Item> {
	/**
	 * What is this for?
	 */
	private static final long serialVersionUID = -4673800475017605707L;
	
	private static final String LOG = "org.zotero.client.data.ItemCollection";

	private String id;
	private String title;
	private String key;
	
	private String dbId;
	
	private String dirty;
	
	/**
	 * APIRequests for changes that need to be propagated to the server. Not sure when these will get done.
	 */
	private ArrayList<APIRequest> additions;
	private ArrayList<APIRequest> removals;
	
	public static Database db;
		
	public ItemCollection(String title) {
		setTitle(title);
	}
	
	/**
	 * Items are marked to be removed by setting them to null. When the collection is next saved,
	 * they will be removed from the database. We also call void remove(Item) to allow for queueing
	 * the action for application on the server, via the API.
	 */
	public boolean remove(Item item) {
		removals.add(APIRequest.remove(item, this));
		return true;
	}
	
	/* Getters and setters */
	public String getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		if (this.title != title) {
			this.title = title;
			this.dirty = APIRequest.API_DIRTY;
		}
	}

	/* These can't be propagated, so it only makes sense before the collection
	 * has been saved to the API. */
	public void setId(String id) {
		this.id = id;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getKey() {
		return key;
	}
	
	public boolean add(ArrayList<Item> items) {
		boolean status = super.addAll(items);
		// Add this to the additions list if a change was made
		if (status) {
			additions.add(APIRequest.add(items, this));
		}
		return status;
	}
	
	public boolean add(Item item) {
		boolean status = super.add(item);
		// Add this to the additions list if a change was made
		if (status) {
			additions.add(APIRequest.add(item, this));
		}
		return status;
	}
	
	/**
	 * Saves the collection metadata to the database.
	 */
	public void save() {
		if (dbId == null) {
			String[] args = { title, key, dirty };
			Cursor cur = db.rawQuery(
					"insert or replace into collections (collection_name, collection_key, dirty)" +
					"values (?, ?, ?)", args);
			if (cur != null) cur.close();
			// get the dbId
			String[] keyArr = { key };
			cur = db.rawQuery("select _id from collections where collection_key=?", keyArr);
			if(cur != null) {
				dbId = cur.getString(0);
				cur.close();
			} else {
				Log.e(LOG, "Failed to get dbId for saved collection");
			}
		} else {
			String[] args = { title, key, dirty, dbId };
			Cursor cur = db.rawQuery(
					"update collections set collection_name=?, collection_key=?, dirty=? " +
					" where _id=? limit 1", args);
			if (cur != null) cur.close();
		}
	}
	
	/**
	 * Saves the item-collection relationship
	 * @throws Exception	If we can't save the collection or children
	 */
	public void saveChildren() throws Exception {
		if (this.dbId == null) {
			// save the collection metadata first
			save();
			if (this.dbId == null) {
				throw new Exception();
			}
		}
		// Iterate through the kids and make sure they're all in the db
		for (Item i : this) {
			if (i.dbId == null) {
				// save the item first
				i.save();
				if (i.dbId == null) {
					throw new Exception();
				}
			}
		}
				
		// Now do the messy part as a transaction
		SQLiteDatabase real = db.beginTransaction();
		try {
			String[] cid = { this.dbId };
			real.rawQuery("delete from itemtocollections where collection_id=?", cid);
			for (Item i : this) {
				String[] args = { i.dbId, this.dbId };
				Cursor cur = real.rawQuery(
						"insert into itemtocollections (collection_id, item_id) values (?, ?)", args);
				if (cur != null) cur.close();
			}
			real.setTransactionSuccessful();
		} finally {
			real.endTransaction();
		}
	}
}