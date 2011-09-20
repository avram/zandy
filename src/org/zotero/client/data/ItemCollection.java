package org.zotero.client.data;

import java.util.ArrayList;

import org.zotero.client.task.APIRequest;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;
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
	
	private static final String TAG = "org.zotero.client.data.ItemCollection";

	/**
	 * Queue of dirty collections to be sent to the server
	 */
	public static ArrayList<ItemCollection> queue;
	
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
	 * APIRequests for changes that need to be propagated to the server. Not sure when these will get done.
	 */
	private ArrayList<APIRequest> additions;
	private ArrayList<APIRequest> removals;
	
	public static Database db;
		
	public ItemCollection(String title) {
		setTitle(title);
		additions = new ArrayList<APIRequest>();
		removals = new ArrayList<APIRequest>();
		dirty = APIRequest.API_DIRTY;
	}

	public ItemCollection() {
		additions = new ArrayList<APIRequest>();
		removals = new ArrayList<APIRequest>();
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
	
	public String getEtag() {
		return etag;
	}
	
	public void setEtag(String etag) {
		this.etag = etag;
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
	public ItemCollection getParent() {
		if (parent != null) return parent;
		if (parentKey == "false") return null;
		if (parentKey != null) {
			 parent = load(parentKey);
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
		additions.clear();
		removals.clear();
		dirty = APIRequest.API_CLEAN;
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
		Log.d(TAG, "item added to collection");
		// Add this to the additions list if a change was made
		if (status) {
			additions.add(APIRequest.add(item, this));
		}
		return status;
	}
	
	/**
	 * Saves the collection metadata to the database.
	 * 
	 * Does nothing with the collection children.
	 */
	public void save() {
		ItemCollection existing = load(key);
		if (existing == null) {
			try {
				SQLiteStatement insert = db.compileStatement("insert or replace into collections " +
					"(collection_name, collection_key, collection_parent, etag, dirty, collection_size)" +
					" values (?, ?, ?, ?, ?, ?)");
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
				insert.executeInsert();
				insert.clearBindings();
				Log.d(TAG, "Saved collection with key: "+key);
			} catch (SQLiteException e) {
				Log.e(TAG, "Exception compiling or running insert statement", e);
				throw e;
			}
			// XXX we need a way to handle locally-created collections
			ItemCollection loaded = load(key);
			if (loaded == null) {
				Log.e(TAG, "Item didn't stick-- still nothing for key: "+key);
			} else {
				dbId = loaded.dbId;
			}
		} else {
			dbId = existing.dbId;
			try {
				SQLiteStatement update = db.compileStatement("update collections set " +
						"collection_name=?, etag=?, dirty=?, collection_size=?" +
						" where _id=?");
				update.bindString(1, title);
				if (etag == null) update.bindNull(2);
				else update.bindString(2, etag);
				if (dirty == null) update.bindNull(3);
				else update.bindString(3, dirty);
				update.bindLong(4, size);
				update.bindString(5, dbId);
				update.executeInsert();
				update.clearBindings();
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
	public void saveChildren() throws Exception {
		/* The size is about to be the size of the internal ArrayList, so
		 * set it now so it'll be propagated to the database if the collection
		 * is new.
		 * 
		 * Save it now-- to fix the size, and to make sure we have a database ID.
		 */
		size = this.size();
		save();
		
		Log.d(TAG,"Collection has dbid: "+dbId);

		/* The saving is implemented by removing all the records for this collection
		 * and saving them anew. This is a risky way to do things. One approach is to
		 * wrap the operation in a transaction, or we could try to keep track of changes.
		 */
		try {
			String[] cid = { this.dbId };
			db.rawQuery("delete from itemtocollections where collection_id=?", cid);
			for (Item i : this) {
				if (i.dbId == null) i.save();
				Log.d(TAG,"Item has dbid: "+i.dbId);
				Log.d(TAG, "saving a child item into the collection");
				String[] args = { this.dbId, i.dbId };
				Cursor cur = db.rawQuery(
						"insert into itemtocollections (collection_id, item_id) values (?, ?)", args);
				if (cur != null) cur.close();
			}
		} catch (Exception e) {
			Log.e(TAG, "Exception caught on saving collection children", e);
		}
		db.close();
	}
	
	/**
	 * Loads the Item members of the collection into the ArrayList<>
	 * 
	 */
	public void loadChildren() {
		if (dbId == null) save();
		Log.d(TAG, "Looking for the kids of a collection with id: "+dbId);
		
		String[] args = { dbId };
		Cursor cursor = db.rawQuery("SELECT item_title, item_type, item_content, etag, dirty, items._id, item_key" +
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
	public ArrayList<ItemCollection> getSubcollections() {
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
	public static ItemCollection load(String collKey) {
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
		return coll;
	}
}