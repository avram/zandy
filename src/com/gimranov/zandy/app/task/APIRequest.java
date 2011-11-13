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

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.database.Cursor;
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
 * See http://www.zotero.org/support/dev/server_api for information.
 * 
 * @author ajlyon
 *
 */
public class APIRequest {
	private static final String TAG = "com.gimranov.zandy.app.task.APIRequest";

	// XXX i18n
	public static final String API_DIRTY =	"Unsynced change";
	public static final String API_NEW =	"New item / collection";
	public static final String API_MISSING ="Partial data";
	public static final String API_STALE = "Stale data";
	public static final String API_WIP  = 	"Sync attempted";
	public static final String API_CLEAN =	"No unsynced change";	
	
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
	 * Database query to run upon success
	 */
	public String onSuccess;
	
	/**
	 * Arguments to database query to run upon success
	 */
	public String[] onSuccessArgs;
	
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
	}
	
	/**
	 * Runs the request's onSuccess query.
	 * This should later be implemented as a callback, to get more flexibility
	 * @param db
	 */
	public void onSuccess(Database db) {
		if (onSuccess != null) {
			Log.d(TAG, "Running onSuccess for request");
			db.rawQuery(onSuccess, onSuccessArgs);
		}
	}
	
	/**
	 * Runs the request's onFailure query.
	 * This should later be implemented as a callback, to get more flexibility
	 * @param db
	 */
	public void onFailure(Database db) {
		// TODO
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
		
		templ.onSuccess = "DELETE FROM itemtocollections where collection_id=? AND item_id=?";
		String[] args = {collection.dbId, item.dbId};
		templ.onSuccessArgs = args;
		
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
			list.add(templ);
		} while (cur.moveToNext() != false);
		cur.close();
		
		db.rawQuery("delete from deleteditems", args);
		db.close();
		return list;
	}
}
