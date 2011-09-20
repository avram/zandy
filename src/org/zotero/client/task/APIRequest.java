package org.zotero.client.task;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;
import org.zotero.client.ServerCredentials;
import org.zotero.client.data.Item;
import org.zotero.client.data.ItemCollection;

import android.util.Log;

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
	private static final String TAG = "org.zotero.client.task.APIRequest";

	
	public static final String API_DIRTY =	"Unsynced change";
	public static final String API_MISSING ="Partial data";
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
			Log.e("org.zotero.client.task.APIRequest", "Error setting body for item", e);
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
			JSONObject obj = new JSONObject();
			for (Item i : items) {
				obj.accumulate("items", i.getContent());
			}
			body = obj.toString(4);
		} catch (JSONException e) {
			Log.e("org.zotero.client.task.APIRequest", "Error setting body for items", e);
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
		APIRequest templ = new APIRequest(ServerCredentials.COLLECTIONS + "/"
									+ collection.getKey() + "/items/" + item.getKey(),
								"DELETE",
								null);
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
		APIRequest templ = new APIRequest(ServerCredentials.COLLECTIONS + "/"
									+ collection.getKey() + "/items",
								"POST",
								null);
		for (Item i : items) {
			templ.body += i.getKey() + " ";
		}
		
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
		APIRequest templ = new APIRequest(ServerCredentials.ITEMS,
								"POST",
								null);
		templ.setBody(items);
		templ.disposition = "xml";
		
		return templ;
	}
	
	/**
	 * Craft a request to update an item on the server
	 * Does not refresh eTag
	 * 
	 * @param item
	 * @return
	 */
	public static APIRequest update(Item item) {
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
}
