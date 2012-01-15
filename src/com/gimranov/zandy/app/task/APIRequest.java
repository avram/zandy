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
package com.gimranov.zandy.app.task;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.Log;

import com.gimranov.zandy.app.ServerCredentials;
import com.gimranov.zandy.app.data.Attachment;
import com.gimranov.zandy.app.data.Database;
import com.gimranov.zandy.app.data.Item;
import com.gimranov.zandy.app.data.ItemCollection;

/**
 * Represents a request to the Zotero API. These can be consumed by
 * other things like ZoteroAPITask. These should be queued up for many purposes.
 * 
 * The APIRequest should include the HttpPost / HttpGet / etc. that it needs
 * to be executed, and optionally a callback to be called when it completes.
 * 
 * See http://www.zotero.org/support/dev/server_api for information.
 * 
 * @author ajlyon
 *
 */
public class APIRequest {
	private static final String TAG = "com.gimranov.zandy.app.task.APIRequest";

	/**
	 * Statuses used for items and collections. They are currently strings, but
	 * they should change to integers. These statuses may be stored in the database.
	 */
	// XXX i18n
	public static final String API_DIRTY =	"Unsynced change";
	public static final String API_NEW =	"New item / collection";
	public static final String API_MISSING ="Partial data";
	public static final String API_STALE = "Stale data";
	public static final String API_WIP  = 	"Sync attempted";
	public static final String API_CLEAN =	"No unsynced change";	
	
	/**
	 * These are constants represented by integers.
	 * 
	 * The above should be moving down here some time.
	 */
	/**
	 * HTTP response codes that we are used to
	 */
	public static final int HTTP_ERROR_CONFLICT = 412;
	public static final int HTTP_ERROR_UNSPECIFIED = 400;
	/**
	 * The following are used when passing things back to the UI
	 * from the API request service / thread.
	 */
	/** Used to indicate database data has changed. */
	public static final int UPDATED_DATA	= 1000;
	/** Current set of requests completed. */
	public static final int BATCH_DONE		= 2000;
	/** Used to indicate database data has changed. */
	public static final int ERROR_UNKNOWN	= 4000;
	/** Queued more requests */
	public static final int QUEUED_MORE		= 3000;
	
	/**
	 * Request types
	 */
	public static final int ITEMS_ALL				= 10000;
	public static final int ITEMS_FOR_COLLECTION	= 10001;
	public static final int ITEMS_CHILDREN			= 10002;

	public static final int COLLECTIONS_ALL			= 20000;

	public static final int ITEM_FIELDS				= 30000;
	public static final int CREATOR_TYPES			= 30001;
	public static final int ITEM_FIELDS_L10N		= 30002;
	public static final int CREATOR_TYPES_L10N		= 30003;
	
	/**
	 * Request status for use within the database
	 */
	public static final int REQ_NEW					= 40000;
	public static final int REQ_FAILING				= 41000;
	
	/**
	 * Type of request we're sending. This should be one of
	 * the request types listed above.
	 */
	public int type;
	
	/**
	 * Callback handler
	 */
	private APIEvent handler;
	
	/**
	 * Base query to send.
	 */
	public String query;
	/**
	 * API key used to make request. Can be omitted for requests that don't need one.
	 */
	public String key;
	/**
	 * One of GET, PUT, POST, DELETE.
	 */
	public String method;
	/**
	 * Response disposition: xml or raw
	 */
	public String disposition;
	
	/**
	 * Used when sending JSON in POST and PUT requests.
	 */
	public String contentType = "application/json";
	/**
	 * Optional token to avoid accidentally sending one request twice. The
	 * server will decline to carry out a second request with the same writeToken
	 * for a single API key in a several-hour period.
	 */
	public String writeToken;
	/**
	 * The eTag received from the server when requesting an item. We can make changes
	 * (delete, update) to an item only while the tag is valid; if the item changes
	 * server-side, our request will be declined until we request the item anew and get
	 * a new valid eTag.
	 */
	public String ifMatch;
	/**
	 * Request body, generally JSON. 
	 */
	public String body;
	
	/**
	 * The temporary key (UUID) that the request is based on.
	 */
	public String updateKey;
	
	/**
	 * Type of object we expect to get. This and the updateKey are used to update
	 * the UUIDs / local keys of locally-created items. I know, it's a hack.
	 */
	public String updateType;
	
	/**
	 * Status code for the request. Codes should be constants defined in APIRequest;
	 * take the REQ_* code and add the response code if applicable.
	 */
	public int status;

	/**
	 * UUID for this request. We use this for DB lookups and as the write token when
	 * appropriate. Every request should have one.
	 */
	private String uuid;
	
	/**
	 * Timestamp when this request was first created.
	 */
	private Date created;
	
	/**
	 * Timestamp when this request was last attempted to be run.
	 */
	private Date lastAttempt;
		
	/**
	 * Creates a basic APIRequest item. Augment the item using instance methods for more complex
	 * requests, or pass it to ZoteroAPITask for simpler ones.
	 * 
	 * @param query		Fragment being requested, like /items
	 * @param method	GET, POST, PUT, or DELETE
	 * @param key		Can be null, if you're planning on making requests that don't need a key.
	 */
	public APIRequest(String query, String method, String key) {
		this.query = query;
		this.method = method;
		this.key = key;
		// default to XML processing
		this.disposition = "xml";
		this.uuid = UUID.randomUUID().toString();
		created = new Date();
	}
	
	/**
	 * Load an APIRequest from its serialized form in the database
	 * 	public static final String[] REQUESTCOLS = {"_id", "uuid", "type",
		"query", "key", "method", "disposition", "if_match", "update_key",
		"update_type", "created", "last_attempt", "status"};
	 * @param uuid
	 * @throws APIException If the provided uuid isn't in the database
	 */
	public APIRequest(Cursor cur) {
		// N.B.: getString and such use 0-based indexing
		this.uuid = cur.getString(1);
		this.type = cur.getInt(2);
		this.query = cur.getString(3);
		this.key = cur.getString(4);
		this.method = cur.getString(5);
		this.disposition = cur.getString(6);
		this.ifMatch = cur.getString(7);
		this.updateKey = cur.getString(8);
		this.updateType = cur.getString(9);
		this.created = new Date();
		this.created.setTime(cur.getLong(10));
		this.lastAttempt = new Date();
		this.lastAttempt.setTime(cur.getLong(11));
		this.status = cur.getInt(12);
	}
	
	/**
	 * Saves the APIRequest's basic info to the database. Does not maintain handler information.
	 * @param db
	 */
	public void save(Database db) {
		try {
			SQLiteStatement insert = db.compileStatement("insert or replace into collections " +
				"(uuid, type, query, key, method, disposition, if_match, update_key, update_type " +
				"created, last_attempt, status)" +
				" values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			// Why, oh why does bind* use 1-based indexing? And cur.get* uses 0-based!
			insert.bindString(1, uuid);
			insert.bindLong(2, (long) type);
			
			String createdUnix = Long.toString(created.getTime());
			String lastAttemptUnix = Long.toString(lastAttempt.getTime());
			String status = Integer.toString(this.status);
			
			// Iterate through null-allowed strings and bind them
			String[] strings = {query, method, disposition, ifMatch, updateKey, updateType,
					createdUnix, lastAttemptUnix, status};
			for (int i = 0; i < strings.length; i++) {
				if (strings[i] == null) insert.bindNull(3+i);
				else insert.bindString(3+i, strings[i]);
			}
			
			insert.executeInsert();
			insert.clearBindings();
			insert.close();
			Log.d(TAG, "Saved collection with key: "+key);
		} catch (SQLiteException e) {
			Log.e(TAG, "Exception compiling or running insert statement", e);
			throw e;
		}
	}
	
	/**
	 * Getter for the request's implementation of the APIEvent interface,
	 * used for call-backs, usually tying into the UI.
	 * 
	 * Returns a no-op, logging handler if none specified
	 * 
	 * @return
	 */
	public APIEvent getHandler() {
		if (handler == null) {
			/*
			 * We have to fall back on a no-op event handler to prevent null exceptions
			 */
			return new APIEvent() {
				@Override
				public void onComplete(APIRequest request) {
					Log.d(TAG, "onComplete called but no handler");
				}
				@Override
				public void onUpdate(APIRequest request) {
					Log.d(TAG, "onUpdate called but no handler");				
				}
				@Override
				public void onError(APIRequest request, Exception exception) {
					Log.d(TAG, "onError called but no handler");					
				}
				@Override
				public void onError(APIRequest request, int error) {
					Log.d(TAG, "onError called but no handler");
				}
			};
		}
		return handler;
	}

	public void setHandler(APIEvent handler) {
		if (this.handler == null) {
			this.handler = handler;
			return;
		}
		
		Log.e(TAG, "APIEvent handler for request cannot be replaced");
	}
	
	/**
	 * Set an Android standard handler to be used for the APIEvents
	 * @param handler
	 */
	public void setHandler(Handler handler) {
		final Handler mHandler = handler;
		if (this.handler == null) {
			this.handler = new APIEvent() {
				@Override
				public void onComplete(APIRequest request) {
					mHandler.sendEmptyMessage(BATCH_DONE);
				}
				@Override
				public void onUpdate(APIRequest request) {
					mHandler.sendEmptyMessage(UPDATED_DATA);				
				}
				@Override
				public void onError(APIRequest request, Exception exception) {
					mHandler.sendEmptyMessage(ERROR_UNKNOWN);					
				}
				@Override
				public void onError(APIRequest request, int error) {
					mHandler.sendEmptyMessage(ERROR_UNKNOWN);
				}
			};
			return;
		}
		Log.e(TAG, "APIEvent handler for request cannot be replaced");
	}

	/**
	 * Populates the body with a JSON representation of 
	 * the specified item.
	 * 
	 * Use this for updating items, i.e.:
	 *     PUT /users/1/items/ABCD2345 
	 * @param item		Item to put in the body.
	 */
	public void setBody(Item item) {
		try {
			body = item.getContent().toString(4);
		} catch (JSONException e) {
			Log.e(TAG, "Error setting body for item", e);
		}
	}
	
	/**
	 * Populates the body with a JSON representation of the specified
	 * items.
	 * 
	 * Use this for creating new items, i.e.:
	 *     POST /users/1/items
	 * @param items
	 */
	public void setBody(ArrayList<Item> items) {
		try {
			JSONArray array = new JSONArray();
			for (Item i : items) {
				JSONObject jItem = i.getContent();
				array.put(jItem);
			}
			JSONObject obj = new JSONObject();
			obj.put("items", array);
			body = obj.toString(4);
		} catch (JSONException e) {
			Log.e(TAG, "Error setting body for items", e);
		}
	}
	
	/**
	 * Populates the body with a JSON representation of specified
	 * attachments; note that this is will not work with non-note
	 * attachments until the server API supports them.
	 * 
	 * @param attachments
	 */
	public void setBodyWithNotes(ArrayList<Attachment> attachments) {
		try {
			JSONArray array = new JSONArray();
			for (Attachment a : attachments) {
				JSONObject jAtt = a.content;
				array.put(jAtt);
			}
			JSONObject obj = new JSONObject();
			obj.put("items", array);
			body = obj.toString(4);
		} catch (JSONException e) {
			Log.e(TAG, "Error setting body for attachments", e);
		}
	}
	
	/**
	 * Getter for the request's UUID
	 * @return
	 */
	public String getUuid() {
		return uuid;
	}
	
	/**
	 * Sets the HTTP response code portion of the request's status
	 * 
	 * @param code
	 * @return		The new status
	 */
	public int setHttpStatus(int code) {
		status = (status - status % 1000) + code;
		return status;
	}
	
	/**
	 * Gets the HTTP response code portion of the request's status;
	 * returns 0 if there was no code set.
	 */
	public int getHttpStatus() {
		return status % 1000;
	}
	
	/**
	 * Record a failed attempt to run the request.
	 * 
	 * Saves the APIRequest in its current state.
	 * 
	 * @param db	Database object
	 * @return	Date object with new lastAttempt value
	 */
	public Date recordAttempt(Database db) {
		lastAttempt = new Date();
		save(db);
		return lastAttempt;
	}
	
	/**
	 * To be called when the request succeeds. Currently just
	 * deletes the corresponding row from the database.
	 * 
	 * @param db	Database object
	 */
	public void succeeded(Database db) {
		// We can short-circuit here if the status doesn't
		// indicate that the request was ever in the database.
		if (status < 10000) return;
		
		String[] args = { uuid };
		db.rawQuery("delete from apirequests where uuid=?", args);
	}
	
	/** NEXT SECTION: Static methods for generating APIRequests */
	
	/**
	 * Produces an API request for the items in a specified collection.
	 * 
	 * @param collection	The collection to fetch
	 * @param c				Context
	 */
	public static APIRequest fetchItems(ItemCollection collection, Context c) {
		APIRequest req = new APIRequest(ServerCredentials.APIBASE
       			+ ServerCredentials.prep(c, ServerCredentials.COLLECTIONS)
       			+"/"+collection.getKey()+"/items", "get", null);
		req.disposition = "xml";
		req.type = APIRequest.ITEMS_FOR_COLLECTION;
		return req;
	}
	
	/**
	 * Produces an API request for all items
	 * 
	 * @param c				Context
	 */
	public static APIRequest fetchItems(Context c) {
		APIRequest req = new APIRequest(ServerCredentials.APIBASE
       			+ ServerCredentials.prep(c, ServerCredentials.ITEMS)
       			+"/top", "get", null);
		req.disposition = "xml";
		req.type = APIRequest.ITEMS_ALL;
		return req;
	}
	
	/**
	 * Produces an API request for all collections
	 * 
	 * @param c				Context
	 */
	public static APIRequest fetchCollections(Context c) {
		APIRequest req = new APIRequest(ServerCredentials.APIBASE 
    			+ ServerCredentials.prep(c, ServerCredentials.COLLECTIONS),
    			"get", null);
		req.disposition = "xml";
		req.type = APIRequest.COLLECTIONS_ALL;
		return req;
	}
	
	/**
	 * Produces an API request to remove the specified item from the collection.
	 * This request always needs a key, but it isn't set automatically and should
	 * be set by whatever consumes this request.
	 * 
	 * From the API docs:
	 *   DELETE /users/1/collections/QRST9876/items/ABCD2345
	 * 
	 * @param item
	 * @param collection
	 * @return
	 */
	public static APIRequest remove(Item item, ItemCollection collection) {
		APIRequest templ = new APIRequest(ServerCredentials.APIBASE
									+ ServerCredentials.COLLECTIONS + "/"
									+ collection.getKey() + "/items/" + item.getKey(),
								"DELETE",
								null);
		templ.disposition = "none";
		
		return templ;
	}
	
	/**
	 * Produces an API request to add the specified items to the collection.
	 * This request always needs a key, but it isn't set automatically and should
	 * be set by whatever consumes this request.
	 * 
	 * From the API docs:
	 *   POST /users/1/collections/QRST9876/items
	 *   
	 *   ABCD2345 FBCD2335
	 * 
	 * @param items
	 * @param collection
	 * @return
	 */
	public static APIRequest add(ArrayList<Item> items, ItemCollection collection) {
		APIRequest templ = new APIRequest(ServerCredentials.APIBASE
									+ ServerCredentials.COLLECTIONS
									+ "/"
									+ collection.getKey() + "/items",
								"POST",
								null);
		templ.body = "";
		for (Item i : items) {
			templ.body += i.getKey() + " ";
		}
		templ.disposition = "none";
		return templ;
	}
	
	/**
	 * Craft a request to add a single item to the server
	 * 
	 * @param item
	 * @param collection
	 * @return
	 */
	public static APIRequest add(Item item, ItemCollection collection) {
		ArrayList<Item> items = new ArrayList<Item>();
		items.add(item);
		return add(items, collection);
	}
	
	/**
	 * Craft a request to add items to the server
	 * This does not attempt to update them, just add them.
	 * 
	 * @param items
	 * @return
	 */
	public static APIRequest add(ArrayList<Item> items) {
		APIRequest templ = new APIRequest(ServerCredentials.APIBASE
								+ ServerCredentials.ITEMS,
								"POST",
								null);
		templ.setBody(items);
		templ.disposition = "xml";
		templ.updateType = "item";
		// TODO this needs to be reworked to send all the keys. Or the whole system
		// needs to be reworked.
		Log.d(TAG, "Using the templ key of the first new item for now...");
		templ.updateKey = items.get(0).getKey();
		
		return templ;
	}
	
	/**
	 * Craft a request to add child items (notes, attachments) to the server
	 * This does not attempt to update them, just add them.
	 * 
	 * @param item The parent item of the attachments
	 * @param attachments
	 * @return
	 */
	public static APIRequest add(Item item, ArrayList<Attachment> attachments) {
		APIRequest templ = new APIRequest(ServerCredentials.APIBASE
								+ ServerCredentials.ITEMS
								+ "/" + item.getKey() + "/children",
								"POST",
								null);
		templ.setBodyWithNotes(attachments);
		templ.disposition = "xml";
		templ.updateType = "attachment";
		// TODO this needs to be reworked to send all the keys. Or the whole system
		// needs to be reworked.
		Log.d(TAG, "Using the templ key of the first new attachment for now...");
		templ.updateKey = attachments.get(0).key;
		
		return templ;
	}	

	/**
	 * Craft a request for the children of the specified item
	 * @param item
	 * @return
	 */
	public static APIRequest children(Item item) {
		APIRequest templ = new APIRequest(ServerCredentials.APIBASE
								+ ServerCredentials.ITEMS+"/"+item.getKey()+"/children",
								"GET",
								null);
		templ.disposition = "xml";
		return templ;
	}
	
	/**
	 * Craft a request to update an attachment on the server
	 * Does not refresh eTag
	 * 
	 * @param attachment
	 * @return
	 */
	public static APIRequest update(Attachment attachment, Database db) {
		Log.d(TAG, "Attachment key pre-update: "+attachment.key);
		// If we have an attachment marked as new, update it
		if (attachment.key.length() > 10) {	
			Item item = Item.load(attachment.parentKey, db);
			ArrayList<Attachment> aL = new ArrayList<Attachment>();
			aL.add(attachment);
			if (item == null) {
				Log.e(TAG, "Orphaned attachment with key: "+attachment.key);
				attachment.delete(db);
				// send something, so we don't get errors elsewhere
				return new APIRequest(ServerCredentials.APIBASE, "GET", null);
			}
			return add(item, aL);
		}
		
		APIRequest templ = new APIRequest(ServerCredentials.APIBASE
								+ ServerCredentials.ITEMS+"/" + attachment.key,
								"PUT",
								null);
		try {
			templ.body = attachment.content.toString(4);
		} catch (JSONException e) {
			Log.e(TAG, "JSON exception setting body for attachment update: "+attachment.key,e);
		}
		templ.ifMatch = '"' + attachment.etag + '"';
		templ.disposition = "xml";
		
		return templ;
	}

	/**
	 * Craft a request to update an attachment on the server
	 * Does not refresh eTag
	 * 
	 * @param item
	 * @return
	 */
	public static APIRequest update(Item item) {
		// If we have an item markes as new, update it
		if (item.getKey().length() > 10) {			
			ArrayList<Item> mAL = new ArrayList<Item>();
			mAL.add(item);
			return add(mAL);
		}
		
		APIRequest templ = new APIRequest(ServerCredentials.APIBASE
								+ ServerCredentials.ITEMS+"/"+item.getKey(),
								"PUT",
								null);
		templ.setBody(item);
		templ.ifMatch = '"' + item.getEtag() + '"';
		Log.d(TAG,"etag: "+item.getEtag());
		templ.disposition = "xml";
		
		return templ;
	}
	
	/**
	 * Produces API requests to delete queued items from the server.
	 * This request always needs a key.
	 * 
	 * From the API docs:
	 *   DELETE /users/1/items/ABCD2345
	 *   If-Match: "8e984e9b2a8fb560b0085b40f6c2c2b7"
	 * 
	 * @param c
	 * @return
	 */
	public static ArrayList<APIRequest> delete(Context c) {
		ArrayList<APIRequest> list = new ArrayList<APIRequest>();
		Database db = new Database(c);
		String[] args = {};
		Cursor cur = db.rawQuery("select item_key, etag from deleteditems", args);
		if (cur == null) {
			db.close();
			Log.d(TAG, "No deleted items found in database");
			return list;
		}

		do {
			APIRequest templ = new APIRequest(ServerCredentials.APIBASE
					+ ServerCredentials.ITEMS + "/" + cur.getString(0),
				"DELETE",
				null);
			templ.disposition = "none";
			templ.ifMatch = cur.getString(1);
			Log.d(TAG, "Adding deleted item: "+cur.getString(0) + " : " + templ.ifMatch);
			templ.save(db);
			list.add(templ);
		} while (cur.moveToNext() != false);
		cur.close();
		
		db.rawQuery("delete from deleteditems", args);
		db.close();
		return list;
	}
	
	/**
	 * Returns array of APIRequest objects from the database
	 * @return
	 */
	public static APIRequest[] queue(Database db) {
		ArrayList<APIRequest> list = new ArrayList<APIRequest>();
		String[] cols = Database.REQUESTCOLS;
		String[] args = { };
		APIRequest[] templ = {};

		Cursor cur = db.query("apirequests", cols, "", args, null, null,
				null, null);
		if (cur == null) return templ;
		
		do {
			APIRequest req = new APIRequest(cur);
			list.add(req);
		} while (cur.moveToNext() != false);
		
		return list.toArray(templ);
	}
}
