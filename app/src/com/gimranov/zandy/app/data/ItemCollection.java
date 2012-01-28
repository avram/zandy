/*******************************************************************************
 * This file is part of Zandy.
 * 
 * Zandy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Zandy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with Zandy.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.gimranov.zandy.app.data;

import java.util.ArrayList;
import java.util.HashSet;

import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import com.gimranov.zandy.app.task.APIRequest;

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
public class ItemCollection extends HashSet<Item> {
	/**
	 * What is this for?
	 */
	private static final long serialVersionUID = -4673800475017605707L;
	
	private static final String TAG = "com.gimranov.zandy.app.data.ItemCollection";

	/**
	 * Queue of dirty collections to be sent to the server
	 */
	public static ArrayList<ItemCollection> queue = new ArrayList<ItemCollection>();
	
	private String id;
	private String title;
	private String key;
	private String etag;
	
	/**
	 * Subcollections of this collection. This is accessed through
	 * a lazy getter, which caches the value. This may at times be
	 * incorrect, since we don't repopulate this to reflect changes
	 * after first populating it.
	 */
	private ArrayList<ItemCollection> subcollections;
	
	/**
	 * This is an approximate size that we have from the database-- it may be outdated
	 * at times, but it's often better than wading through the database and figuring out
	 * the size that way.
	 * 
	 * This size is updated only when loading children;
	 */
	private int size;
	
	private ItemCollection parent;
	private String parentKey;

	public String dbId;
	
	public String dirty;
	
	/**
	 * Timestamp of last update from server; this is an Atom-formatted
	 * timestamp
	 */
	private String timestamp;
			
	public ItemCollection(String title) {
		setTitle(title);
		dirty = APIRequest.API_DIRTY;
	}

	public ItemCollection() {
	}
	
	/**
	 * We call void remove(Item) to allow for queueing
	 * the action for application on the server, via the API.
	 * 
	 * When fromAPI is not true, queues a collection membership
	 * request for the server as well.
	 * 
	 * @param item
	 * @param fromAPI	False for collection memberships we receive from the server
	 * @param db
	 */
	public boolean remove(Item item, boolean fromAPI, Database db) {
		String[] args = {dbId, item.dbId};
		db.rawQuery("delete from itemtocollections where collection_id=? and item_id=?", args);
		
		if (!fromAPI) {
			APIRequest req = APIRequest.remove(item, this);
			req.status = APIRequest.REQ_NEW;
			req.save(db);
		}
		super.remove(item);
		return true;
	}
		
	/* Getters and setters */
	public String getId() {
		return id;
	}
	
	public String getEtag() {
		return etag;
	}
	
	public void setEtag(String etag) {
		this.etag = etag;
	}

	public String getTimestamp() {
		return timestamp;
	}
	
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
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
	
	/* I'm not sure how easy this is to propagate to the API */
	public ItemCollection getParent(Database db) {
		if (parent != null) return parent;
		if (parentKey == "false") return null;
		if (parentKey != null) {
			 parent = load(parentKey, db);
		}
		return parent;
	}

	public void setParent(ItemCollection parent) {
		this.parentKey = parent.getKey();
		this.parent = parent;
	}

	public void setParent(String parentKey) {
		this.parentKey = parentKey;
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

	/**
	 * Returns the size, not as measured, but as listed in DB
	 * @return	
	 */
	public int getSize() {
		return size;
	}
	
	/**
	 * Marks the collection as clean and clears the pending additions and 
	 * removals.
	 * 
	 * Note that dirty markings don't matter until saved to the DB, so
	 * this should be followed by a save.
	 */
	public void markClean() {
		dirty = APIRequest.API_CLEAN;
	}
	
	public boolean add(Item item, Database db) {
		return add(item, false, db);
	}
	
	/**
	 * Adds the specified item to this collection.
	 * 
	 * When fromAPI is not true, queues a collection membership
	 * request for the server as well.
	 * 
	 * @param item
	 * @param fromAPI	False for collection memberships we receive from the server
	 * @param db
	 * @return		Whether this is a new item for the collection
	 */
	public boolean add(Item item, boolean fromAPI, Database db) {
		for (Item i : this) {
			if(i.equals(item)) {
				Log.d(TAG, "Item already in collection");
				return false;
			}
		}
		
		super.add(item);
		Log.d(TAG, "Item added to collection");
		if (!fromAPI) {
			Log.d(TAG, "Saving new collection membership request to database");
			APIRequest req = APIRequest.add(item, this);
			req.status = APIRequest.REQ_NEW;
			req.save(db);
		}
		return true;
	}
	
	/**
	 * Returns ArrayList of Item objects not in the specified ArrayList of item keys. Used for determining
	 * when items have been deleted from a collection.
	 */
	public ArrayList<Item> notInKeys(ArrayList<String> keys) {
		ArrayList<Item> notThere = new ArrayList<Item>();
		
		for (Item i : this) {
			if (!keys.contains(i.getKey()))
				notThere.add(i);
		}
		
		return notThere;
	}
	
	/**
	 * Saves the collection metadata to the database.
	 * 
	 * Does nothing with the collection children.
	 */
	public void save(Database db) {
		ItemCollection existing = load(key, db);
		if (existing == null) {
			try {
				SQLiteStatement insert = db.compileStatement("insert or replace into collections " +
					"(collection_name, collection_key, collection_parent, etag, dirty, collection_size, timestamp)" +
					" values (?, ?, ?, ?, ?, ?, ?)");
				// Why, oh why does bind* use 1-based indexing? And cur.get* uses 0-based!
				insert.bindString(1, title);
				if (key == null) insert.bindNull(2);
				else insert.bindString(2, key);
				if (parentKey == null) insert.bindNull(3);
				else insert.bindString(3, parentKey);
				if (etag == null) insert.bindNull(4);
				else insert.bindString(4, etag);
				if (dirty == null) insert.bindNull(5);
				else insert.bindString(5, dirty);
				insert.bindLong(6, size);
				if (timestamp == null) insert.bindNull(7);
				else insert.bindString(7, timestamp);
				insert.executeInsert();
				insert.clearBindings();
				insert.close();
				Log.d(TAG, "Saved collection with key: "+key);
			} catch (SQLiteException e) {
				Log.e(TAG, "Exception compiling or running insert statement", e);
				throw e;
			}
			// XXX we need a way to handle locally-created collections
			ItemCollection loaded = load(key, db);
			if (loaded == null) {
				Log.e(TAG, "Item didn't stick-- still nothing for key: "+key);
			} else {
				dbId = loaded.dbId;
			}
		} else {
			dbId = existing.dbId;
			try {
				SQLiteStatement update = db.compileStatement("update collections set " +
						"collection_name=?, etag=?, dirty=?, collection_size=?, timestamp=?" +
						" where _id=?");
				update.bindString(1, title);
				if (etag == null) update.bindNull(2);
				else update.bindString(2, etag);
				if (dirty == null) update.bindNull(3);
				else update.bindString(3, dirty);
				update.bindLong(4, size);
				if (timestamp == null) update.bindNull(5);
				else update.bindString(5, timestamp);
				update.bindString(6, dbId);
				update.executeInsert();
				update.clearBindings();
				update.close();
				Log.i(TAG, "Updating existing collection.");
			} catch (SQLiteException e) {
				Log.e(TAG, "Exception compiling or running update statement", e);
			}
		}
		db.close();
	}
	
	/**
	 * Saves the item-collection relationship. This saves the collection
	 * itself as well.
	 * @throws Exception	If we can't save the collection or children
	 */
	public void saveChildren(Database db) {
		/* The size is about to be the size of the internal ArrayList, so
		 * set it now so it'll be propagated to the database if the collection
		 * is new.
		 * 
		 * Save it now-- to fix the size, and to make sure we have a database ID.
		 */
		
		loadChildren(db);
		
		Log.d(TAG,"Collection has dbid: "+dbId);

		/* The saving is implemented by removing all the records for this collection
		 * and saving them anew. This is a risky way to do things. One approach is to
		 * wrap the operation in a transaction, or we could try to keep track of changes.
		 */
		
		HashSet<String> keys = new HashSet<String>();
		for (Item i : this) {
			if (i.dbId == null) i.save(db);
			keys.add(i.dbId);
		}
		
		db.beginTransaction();
		try {
			String[] cid = { this.dbId };
			db.rawQuery("delete from itemtocollections where collection_id=?", cid);
			for (String i : keys) {
				String[] args = { this.dbId, i };
				db.rawQuery(
						"insert into itemtocollections (collection_id, item_id) values (?, ?)", args);
			}
			db.setTransactionSuccessful();
		} catch (Exception e) {
			Log.e(TAG, "Exception caught on saving collection children", e);
		} finally {
			db.endTransaction();
		}
		
		// We can now get a proper and total count
		String[] args = { this.dbId };
		Cursor cur = db.rawQuery(
				"select count(distinct item_id) from itemtocollections where collection_id=?", args);
		cur.moveToFirst();
		if(!cur.isAfterLast()) this.size = cur.getInt(0);
		if (cur != null) cur.close();

		save(db);
	}
	
	/**
	 * Loads the Item members of the collection into the ArrayList<>
	 * 
	 */
	public void loadChildren(Database db) {
		if (dbId == null) save(db);
		Log.d(TAG, "Looking for the kids of a collection with id: "+dbId);
		
		String[] args = { dbId };
		Cursor cursor = db.rawQuery("SELECT item_title, item_type, item_content, etag, dirty, items._id, item_key, item_year, item_creator, items.timestamp, item_children" +
				" FROM items, itemtocollections WHERE items._id = item_id AND collection_id=? ORDER BY item_title",
				args);
		if (cursor != null) {
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				Item i = Item.load(cursor);
				Log.d(TAG,"Adding an item to the collection: "+i.getTitle());
				size = this.size();
				if (i != null) super.add(i);
				cursor.moveToNext();
			}
			cursor.close();
		} else {
			Log.d(TAG,"Cursor was null, so we still didn't get kids for the collection!");
		}
	}
	
	/**
	 * Gets the subcollections of the current collection.
	 * 
	 * This is a lazy getter and won't check again after the first time.
	 * @return
	 */
	public ArrayList<ItemCollection> getSubcollections(Database db) {
		if (this.subcollections != null) return this.subcollections;
		
		this.subcollections = new ArrayList<ItemCollection>();

		String[] args = { this.key };
		Cursor cur = db.query("collections", Database.COLLCOLS, "collection_parent=?", args, null, null, null, null);

		if (cur == null) {
			Log.d(TAG,"No subcollections found for collection: " + this.title);
			return this.subcollections;
		}
		
		do {
			ItemCollection collection = load(cur);
			if (collection == null) {
				Log.e(TAG, "Got a null collection when loading from cursor, in getSubcollections");
				continue;
			}
			Log.d(TAG,"Found subcollection: " + collection.title);
			this.subcollections.add(collection);
		} while (cur.moveToNext() != false);
		
		if (cur != null) cur.close();
		return this.subcollections;
	}
	
	/**
	 * Loads and returns an ItemCollection for the specified collection key
	 * Returns null when no match is found for the specified collKey
	 * 
	 * @param collKey
	 * @return
	 */
	public static ItemCollection load(String collKey, Database db) {
		if (collKey == null) return null;
		String[] cols = Database.COLLCOLS;
		String[] args = { collKey };
		Log.i(TAG, "Loading collection with key: "+collKey);
		Cursor cur = db.query("collections", cols, "collection_key=?", args, null, null, null, null);

		ItemCollection coll = load(cur);
		if (coll == null) Log.i(TAG, "Null collection loaded!");
		if (cur != null) cur.close();
		return coll;
	}
	
	/**
	 * Loads a collection from the specified Cursor, where the cursor was created using
	 * the recommended query in Database.COLLCOLS
	 * 
	 * Returns null when the specified cursor is null.
	 * 
	 * Does not close the cursor!
	 * 
	 * @param cur
	 * @return			An ItemCollection object for the current row of the Cursor
	 */
	public static ItemCollection load(Cursor cur) {
		ItemCollection coll = new ItemCollection();
		if (cur == null) {
			return null;
		}
		
		coll.setTitle(cur.getString(0));
		coll.setParent(cur.getString(1));
		coll.etag = cur.getString(2);
		coll.dirty = cur.getString(3);
		coll.dbId = cur.getString(4);
		coll.setKey(cur.getString(5));
		coll.size = cur.getInt(6);
		coll.timestamp = cur.getString(7);
		return coll;
	}
	
	/**
	 * Identifies stale or missing collections in the database and queues them for syncing
	 */
	public static void queue(Database db) {
		Log.d(TAG,"Clearing dirty queue before repopulation");		
		queue.clear();
		ItemCollection coll;
		String[] cols = Database.COLLCOLS;
		String[] args = { APIRequest.API_CLEAN };
		Cursor cur = db.query("collections", cols, "dirty!=?", args, null, null, null, null);
		
		if (cur == null) {
			Log.d(TAG,"No dirty items found in database");
			queue.clear();
			return;
		}
		
		do {
			Log.d(TAG,"Adding collection to dirty queue");
			coll = load(cur);
			queue.add(coll);
		} while (cur.moveToNext() != false);
		
		if (cur != null) cur.close();
	}

	/**
	 * Gives us ItemCollection objects to feed into something like UI
	 * @return
	 */
	public static ArrayList<ItemCollection> getCollections(Database db) {
		ArrayList<ItemCollection> collections = new ArrayList<ItemCollection>();
		ItemCollection coll;
		String[] cols = Database.COLLCOLS;
		Cursor cur = db.query("collections", cols, "", null, null, null, "collection_name", null);
		
		if (cur == null) {
			Log.d(TAG,"No collections found in database");
			return collections;
		}
		
		do {
			Log.d(TAG,"Adding collection to collection list");
			coll = load(cur);
			collections.add(coll);
		} while (cur.moveToNext() != false);
		if (cur != null) cur.close();
		
		return collections;
	}
	
	/**
	 * Gives us ItemCollection objects containing given item
	 * to feed into something like UI
	 * @return
	 */
	public static ArrayList<ItemCollection> getCollections(Item i, Database db) {
		ArrayList<ItemCollection> collections = new ArrayList<ItemCollection>();
		ItemCollection coll;
		String[] args = { i.dbId };
		Cursor cursor = db.rawQuery("SELECT collection_name, collection_parent," +
				" etag, dirty, collections._id, collection_key, collection_size," +
				" timestamp FROM collections, itemtocollections" +
				" WHERE collections._id = collection_id AND item_id=?" +
				"	 ORDER BY collection_name",
				args);
		
		if (cursor == null) {
			Log.d(TAG,"No collections found for item");
			return collections;
		}
		
		do {
			Log.d(TAG,"Adding collection to collection list");
			coll = load(cursor);
			collections.add(coll);
		} while (cursor.moveToNext() != false);
		if (cursor != null) cursor.close();
		
		return collections;
	}
	
	/**
	 * Gives us count of ItemCollection objects containing given item
	 * to feed into something like UI
	 * @return
	 */
	public static int getCollectionCount(Item i, Database db) {
		String[] args = { i.dbId };
		Cursor cursor = db.rawQuery("SELECT COUNT(*) " +
				" FROM collections, itemtocollections" +
				" WHERE collections._id = collection_id AND item_id=?",
				args);
		
		if (cursor == null) {
			Log.d(TAG,"No collections found for item");
			return 0;
		}
		
		int count = cursor.getInt(0);
		if (cursor != null) cursor.close();
		return count;
	}
}
