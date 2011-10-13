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
package com.gimranov.zandy.client;

import java.io.InputStream;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import com.gimranov.zandy.client.data.Database;
import com.gimranov.zandy.client.data.Item;
import com.gimranov.zandy.client.data.ItemCollection;
import com.gimranov.zandy.client.task.APIRequest;

import android.sax.Element;
import android.sax.ElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Log;
import android.util.Xml;

public class XMLResponseParser extends DefaultHandler {
	private static final String TAG = "com.gimranov.zandy.client.XMLResponseParser";
	
	private InputStream input;
	private Item item;
	private ItemCollection collection;
	private ItemCollection parent;
	private String updateType;
	private String updateKey;
	private boolean items = false;
	
	public static ArrayList<APIRequest> queue;
	
	public static Database db;

	public static final int MODE_ITEMS = 1; 
	public static final int MODE_ITEM = 2;
	public static final int MODE_ITEM_CHILDREN = 8;
	public static final int MODE_COLLECTIONS = 3;
	public static final int MODE_COLLECTION = 4;
	public static final int MODE_COLLECTION_ITEMS = 5;	
	public static final int MODE_ENTRY = 6;	
	public static final int MODE_FEED = 7;		

	static final String ATOM_NAMESPACE = "http://www.w3.org/2005/Atom";
	static final String Z_NAMESPACE = "http://zotero.org/ns/api";

	public XMLResponseParser(InputStream in) {
		input = in;
		// Initialize the request queue if needed
		if (queue == null) queue = new ArrayList<APIRequest>();
	}
	
	public void update(String type, String key) {
		updateType = type;
		updateKey = key;
	}
	
	public void parse(int mode, String url) {		
		Element entry;
		RootElement root;
		// we have a different root for indiv. items
		if (mode == MODE_FEED) {
			root = new RootElement(ATOM_NAMESPACE, "feed");
	        entry = root.getChild(ATOM_NAMESPACE, "entry");
		} else {
			// MODE_ITEM, MODE_COLLECTION
			Log.d(TAG, "entry mode");
			root = new RootElement(ATOM_NAMESPACE, "entry");
			entry = (Element) root;
		}
		
		if (mode == MODE_FEED) {
	        root.getChild(ATOM_NAMESPACE, "link").setStartElementListener(new StartElementListener(){
	            public void start(Attributes attributes) {
	            	String rel = "";
	            	String href = "";
	            	int length = attributes.getLength();
	            	// I shouldn't have to walk through, but the namespacing isn't working here
	            	for (int i = 0; i < length; i++) {
	            		if (attributes.getQName(i) == "rel") rel = attributes.getValue(i);
	            		if (attributes.getQName(i) == "href") href = attributes.getValue(i);
	            	}
	    			// We try to get a parent collection if necessary / possible
	            	if (rel.contains("self")) {
	    				// Try to get a parent collection
	            		int colloc = href.indexOf("/collections/");
	            		int itemloc = href.indexOf("/items");
	            		// Our URL looks like this:
	            		// 		https://api.zotero.org/users/5770/collections/2AJUSIU9/items?content=json
	            		if (colloc != -1 && itemloc != -1) {
	            			// The string "/collections/" is thirteen characters long
	            			String id = href.substring(colloc+13, itemloc);
	    					Log.d(TAG, "Collection key: "+id);
	    					parent = ItemCollection.load(id);
	    				} else {
	    					Log.d(TAG, "Key extraction failed from root; maybe this isn't a collection listing?");
	    				}
	            	}
	    			// If there are more items, queue them up to be handled too
	            	if (rel.contains("next")) {
    					Log.e(TAG, "Found continuation: "+href);
	            		APIRequest req = new APIRequest(href, "get", null);
	        			req.disposition = "xml";
	        			queue.add(req);
	            	}
	            	
	            	Log.i(TAG, "link-tag-parsed: rel: "+rel+" => "+href);
	            }
	        });
		}
		
        entry.setElementListener(new ElementListener() {
            public void start(Attributes attributes) {
            	item = new Item();
            	collection = new ItemCollection();
            	if (parent != null) parent.add(item);
            	Log.i(TAG, "new entry");
            }

            public void end() {
            	if (items == true) {
            		if (updateKey != null && updateType != null && updateType.equals("item")) {
            			// We have an incoming new version of an item
            			Item existing = Item.load(updateKey);
            			if (existing != null) {
            				Log.d(TAG, "Updating newly created item to replace temporary key: " 
            							+ updateKey + " => " + item.getKey() + "");
            				existing.setKey(item.getKey());
            				existing.dirty = APIRequest.API_CLEAN;
            				existing.save();
            			}
            		} else {
	            		item.dirty = APIRequest.API_CLEAN;
	            		item.save();
            		}
                	Log.i(TAG, "Done parsing item entry.");
            		return;
            	}
            	
            	if (items == false) {
            		if (updateKey != null && updateType != null && updateType.equals("collection")) {
            			// We have an incoming new version of a collection
            			ItemCollection existing = ItemCollection.load(updateKey);
            			if (existing != null) {
            				Log.d(TAG, "Updating newly created collection to replace temporary key: " 
            							+ updateKey + " => " + collection.getKey() + "");
            				existing.setKey(collection.getKey());
            				existing.dirty = APIRequest.API_CLEAN;
            				existing.save();
            			}
                    	Log.i(TAG, "Done parsing new collection entry.");            			
            			// We don't need to load again, since a new collection can't be stale
            			return;
            		}
            		
            		ItemCollection ic = ItemCollection.load(collection.getKey());
            		if (ic != null) {
            			if (!ic.getTimestamp()
            				.equals(collection.
            						getTimestamp())) {
            				// In this case, we have data, but we should refresh it
            				collection.dirty = APIRequest.API_STALE;
            			} else {
            				// Collection hasn't changed!
            				collection = ic;
            			}
            		} else {
            			// This means that we haven't seen the collection before, so it must be
            			// a new one, and we don't have contents for it.
            			collection.dirty = APIRequest.API_MISSING;
            		}
    				Log.d(TAG, "Status: "+collection.dirty+" for "+collection.getTitle());
            		collection.save();
                	Log.i(TAG, "Done parsing a collection entry.");
                	return;
            	}
            }
        });
        entry.getChild(ATOM_NAMESPACE, "title").setEndTextElementListener(new EndTextElementListener(){
            public void end(String body) {
            	item.setTitle(body);
            	collection.setTitle(body);
            	Log.i(TAG, body);
            }
        });
        entry.getChild(Z_NAMESPACE, "key").setEndTextElementListener(new EndTextElementListener(){
            public void end(String body) {
            	item.setKey(body);
            	collection.setKey(body);
            	Log.i(TAG, body);
            }
        });
        entry.getChild(ATOM_NAMESPACE, "updated").setEndTextElementListener(new EndTextElementListener(){
            public void end(String body) {
            	item.setTimestamp(body);
            	collection.setTimestamp(body);
            	Log.i(TAG, body);
            }
        });
        entry.getChild(Z_NAMESPACE, "itemType").setEndTextElementListener(new EndTextElementListener(){
            public void end(String body) {
            	item.setType(body);
            	items = true;
            	Log.i(TAG, body);
            }
        });
        entry.getChild(Z_NAMESPACE, "numChildren").setEndTextElementListener(new EndTextElementListener(){
            public void end(String body) {
            	item.setChildren(body);
            	Log.i(TAG, body);
            }
        });
        entry.getChild(Z_NAMESPACE, "year").setEndTextElementListener(new EndTextElementListener(){
            public void end(String body) {
            	item.setYear(body);
            	Log.i(TAG, body);
            }
        });
        entry.getChild(Z_NAMESPACE, "creatorSummary").setEndTextElementListener(new EndTextElementListener(){
            public void end(String body) {
            	item.setCreatorSummary(body);
            	Log.i(TAG, body);
            }
        });
        entry.getChild(ATOM_NAMESPACE, "id").setEndTextElementListener(new EndTextElementListener(){
            public void end(String body) {
            	item.setId(body);
            	collection.setId(body);
            	Log.i(TAG, body);
            }
        });
        entry.getChild(ATOM_NAMESPACE, "content").setStartElementListener(new StartElementListener(){
            public void start(Attributes attributes) {
            	String etag = attributes.getValue(Z_NAMESPACE, "etag");
            	item.setEtag(etag);
            	collection.setEtag(etag);
            	Log.i(TAG, etag);
            }
        });
        entry.getChild(ATOM_NAMESPACE, "content").setEndTextElementListener(new EndTextElementListener(){
            public void end(String body) {
            	try {
            		JSONObject obj = new JSONObject(body);
            		try {
            			collection.setParent(obj.getString("parent"));
            		} catch (JSONException e) {
                		Log.e(TAG, "collection parent not found in JSON; not a collection?");
            		}
            		item.setContent(obj);
            	} catch (JSONException e) {
            		Log.e(TAG, "content", e);
            	}
            	Log.i(TAG, body);
            }
        });
        try {
            Xml.parse(this.input, Xml.Encoding.UTF_8, root.getContentHandler());
            if (parent != null) {
            	parent.saveChildren();
            	parent.markClean();
            	parent.save();
            }
            db.close();
        } catch (Exception e) {
        	throw new RuntimeException(e);
        }
    }
}
