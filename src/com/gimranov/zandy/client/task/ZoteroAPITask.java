/*
 *  Zandy
 *  Based in part on Mendroid, Copyright 2011 Martin Paul Eve <martin@martineve.com>
 *
 *  This file is part of Zandy.
 *
 *  Zandy is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *   
 *  Zandy is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Zandy.  If not, see <http://www.gnu.org/licenses/>.
 *  
 */

package com.gimranov.zandy.client.task;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.CursorAdapter;

import com.gimranov.zandy.client.ServerCredentials;
import com.gimranov.zandy.client.XMLResponseParser;
import com.gimranov.zandy.client.data.Item;
import com.gimranov.zandy.client.data.ItemCollection;

public class ZoteroAPITask extends AsyncTask<APIRequest, Integer, JSONArray[]> {
	private static final String TAG = "com.gimranov.zandy.client.task.ZoteroAPITask";
		
	private String key;
	private CursorAdapter adapter;
	private String userID;
	
	public int syncMode = -1;
	
	public static final int AUTO_SYNC_STALE_COLLECTIONS = 1;

	public boolean autoMode = false;
	
	public ZoteroAPITask(String key)
	{
		this.key = key;
	}

	public ZoteroAPITask(Context c)
	{
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
		userID = settings.getString("user_id", null);
		key = settings.getString("user_key", null);
		if (settings.getBoolean("sync_aggressively", false))
			syncMode = AUTO_SYNC_STALE_COLLECTIONS;
	}

	public ZoteroAPITask(Context c, CursorAdapter adapter)
	{
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
		userID = settings.getString("user_id", null);
		key = settings.getString("user_key", null);
		if (settings.getBoolean("sync_aggressively", false))
			syncMode = AUTO_SYNC_STALE_COLLECTIONS;
	}
	
	public ZoteroAPITask(String key, CursorAdapter adapter)
	{
		this.key = key;
		this.adapter = adapter;
	}
	
	@Override
	protected JSONArray[] doInBackground(APIRequest... params) {
        return doFetch(params);
	} 
	
	public JSONArray[] doFetch(APIRequest... reqs)
	{	
		int count = reqs.length;
		
		JSONArray[] ret = new JSONArray[count];
        
        for (int i = 0; i < count; i++) {
        	if (reqs[i] == null) {
        		Log.d(TAG, "Skipping null request");
        		continue;
        	}
        	
        	// Just in case we missed something, we fix the user ID right here too
        	if (userID != null) reqs[i] = ServerCredentials.prep(userID, reqs[i]);
        	
        	try {
        		Log.i(TAG, "Executing API call: " + reqs[i].query);
        		
        		reqs[i].key = key;
        		
        		issue(reqs[i]);
				
				Log.i(TAG, "Succesfully retrieved API call: " + reqs[i].query);
				
			} catch (Exception e) {
				// TODO: determine if this is due to needing re-auth
				Log.e(TAG, "Failed to execute API call: " + reqs[i].query, e);
				return null;
			}
	    	if (XMLResponseParser.queue != null && !XMLResponseParser.queue.isEmpty()) {
	        	Log.i(TAG, "Finished call, but trying to add from parser's request queue");
	    		APIRequest[] templ = { };
	    		APIRequest[] queue = XMLResponseParser.queue.toArray(templ); 
	    		XMLResponseParser.queue.clear();
	    		this.doInBackground(queue);
	    	} else {
	        	Log.i(TAG, "Finished call, and parser's request queue is empty");
	        	
	        	// Here's where we'd tie in to periodic housekeeping syncs
	        	// If we're already in auto mode (that is, here), just move on
	        	if (this.autoMode) continue;
	        	Log.d(TAG, "Preparing item sync");
	        	Item.queue();
	        	int length = Item.queue.size();
	        	length += ItemCollection.additions.size();
	        	length += ItemCollection.removals.size();
	        	int basicLength = length;
	        	// We pref this off
	        	if (syncMode == AUTO_SYNC_STALE_COLLECTIONS) {
		        	ItemCollection.queue();
		        	length += ItemCollection.queue.size();
	        	}
	        	APIRequest[] mReqs = new APIRequest[length];
	        	for (int j = 0; j < basicLength; j++) {
	        		if (j < Item.queue.size()) {
	        			Log.d(TAG, "Queueing dirty item ("+j+"): "+Item.queue.get(j).getTitle());
	        			mReqs[j] = ServerCredentials.prep(userID, APIRequest.update(Item.queue.get(j)));
	        		} else if (j < Item.queue.size() + ItemCollection.additions.size()) {
	        			Log.d(TAG, "Queueing new collection membership ("+j+")");
	        			mReqs[j] = ServerCredentials.prep(userID,
	        							ItemCollection.additions.get(j
	        								- Item.queue.size()));
	        		} else if (j < Item.queue.size() 
	        					+ ItemCollection.additions.size() 
	        					+ ItemCollection.removals.size()) {
	        			Log.d(TAG, "Queueing removed collection membership ("+j+")");
	        			mReqs[j] = ServerCredentials.prep(userID,
	        						ItemCollection.additions.get(j 
	        								- Item.queue.size() 
	        								- ItemCollection.additions.size()));
	        		}
	        		// We'll clear the collection change queues; we may need to re-add failed requests later
	        		ItemCollection.additions.clear();
	        		ItemCollection.removals.clear();
	        	}
	        	
	        	// We pref this off
	        	if (syncMode == AUTO_SYNC_STALE_COLLECTIONS) {
		        	for (int j = 0; j < ItemCollection.queue.size(); j++) {
		       			Log.d(TAG, "Syncing dirty or stale collection: "+ItemCollection.queue.get(j).getTitle());
		        		mReqs[basicLength + j] = new APIRequest(ServerCredentials.APIBASE
									+ ServerCredentials.prep(userID, ServerCredentials.COLLECTIONS)
									+"/"+ItemCollection.queue.get(j).getKey() + "/items",
									"get",
									key);
		        	}
	        	}
	        	// We're in auto mode...
	        	this.autoMode = true;
	        	this.doInBackground(mReqs);
	    	}
            publishProgress((int) ((i / (float) count) * 100));
        }
             
        return ret;
	}
	
	
	@Override
	protected void onPostExecute(JSONArray[] result) {
		// invoked on the UI thread

    		if (result == null) {
	        	Log.e(TAG, "Returned NULL; looks like a problem communicating with server; review stack trace.");
	        	// there was an error
	        	String text = "Error communicating with server.";	
	        	Log.i(TAG, text);
    		} else {
	        	if (this.adapter != null) {
		        	this.adapter.notifyDataSetChanged();
		        	Log.i(TAG, "Finished call, notified parent adapter!");
	        	} else {
		        	Log.i(TAG, "Finished call successfully, but nobody to notify");        		
	        	}
    		}
    }
	
	/**
	 * Executes the specified APIRequest and handles the response
	 * 
	 * This is done synchronously; use the the AsyncTask interface for calls
	 * from the UI thread.
	 * 
	 * @param req
	 * @return
	 */
	public static String issue(APIRequest req) {
		// Check that the method makes sense
		String method = req.method.toLowerCase();
		if (!method.equals("get")
			&& !method.equals("post")
			&& !method.equals("delete")
			&& !method.equals("put")
		) {
			// TODO Throw an exception here.
			Log.e(TAG, "Invalid method: "+method);
			return null;
		}
		String resp = "";
		try {
			// Append content=json everywhere, if we don't have it yet
			if (req.query.indexOf("content=json") == -1) {
				if (req.query.indexOf("?") != -1) {
					req.query += "&content=json";
				} else {
					req.query += "?content=json";
				}
			}
			
			// Append the key, if defined, to all requests
			if (req.key != null && req.key != "") {
				req.query += "&key=" + req.key;

			}
			if (method.equals("put")) {
				req.query = req.query.replace("content=json&", "");
			}
			Log.i(TAG, "Request "+ req.method +": " + req.query);		

			URI uri = new URI(req.query);
			HttpClient client = new DefaultHttpClient();
			// The default implementation includes an Expect: header, which
			// confuses the Zotero servers.
			//((AbstractHttpClient) client).removeRequestInterceptorByClass(
			//		org.apache.http.protocol.RequestExpectContinue.class);
			client.getParams().setParameter("http.protocol.expect-continue", false);
			// We also need to send our data nice and raw.
			client.getParams().setParameter("http.protocol.content-charset", "UTF-8");

			/* It would be good to rework this mess to be less repetitive */
			if (method.equals("post")) {
				HttpPost request = new HttpPost();
				request.setURI(uri);
				
				// Set headers if necessary
				if(req.ifMatch != null) {
					request.setHeader("If-Match", req.ifMatch);
				}
				if(req.contentType != null) {
					request.setHeader("Content-Type", req.contentType);
				}
				if(req.body != null) {
					Log.d(TAG, "Post body: "+req.body);
					// Force the encoding here
					StringEntity entity = new StringEntity(req.body,"UTF-8");
					request.setEntity(entity);
				}
				if (req.disposition.equals("xml")) {
					HttpResponse hr = client.execute(request);
					int code = hr.getStatusLine().getStatusCode();
					Log.d(TAG, code + " : "+ hr.getStatusLine().getReasonPhrase());
					if (code < 400) {
						HttpEntity he = hr.getEntity();
						InputStream in = he.getContent();
						XMLResponseParser parse = new XMLResponseParser(in);
						if (req.updateKey != null && req.updateType != null)
							parse.update(req.updateType, req.updateKey);
						// The response on POST in XML mode (new item) is a feed
						parse.parse(XMLResponseParser.MODE_FEED, uri.toString());
						resp = "XML was parsed.";
					} else {
						Log.e(TAG, "Not parsing non-XML response, code >= 400");
						ByteArrayOutputStream ostream = new ByteArrayOutputStream();
						hr.getEntity().writeTo(ostream);
						Log.e(TAG,"Error Body: "+ ostream.toString());
						Log.e(TAG,"Post Body:"+ req.body);
					}
				} else {
					resp = client.execute(request, new BasicResponseHandler());
				}
			} else if (method.equals("put")) {
				HttpPut request = new HttpPut();
				request.setURI(uri);
				
				// Set headers if necessary
				if(req.ifMatch != null) {
					request.setHeader("If-Match", req.ifMatch);
				}
				if(req.contentType != null) {
					request.setHeader("Content-Type", req.contentType);
				}
				if(req.body != null) {
					// Force the encoding here
					StringEntity entity = new StringEntity(req.body,"UTF-8");
					request.setEntity(entity);
				}
				if (req.disposition.equals("xml")) {
					HttpResponse hr = client.execute(request);
					int code = hr.getStatusLine().getStatusCode();
					Log.d(TAG, code + " : "+ hr.getStatusLine().getReasonPhrase());
					if (code < 400) {
						HttpEntity he = hr.getEntity();
						InputStream in = he.getContent();
						XMLResponseParser parse = new XMLResponseParser(in);
						parse.parse(XMLResponseParser.MODE_ENTRY, uri.toString());
						resp = "XML was parsed.";
					} else {
						Log.e(TAG, "Not parsing non-XML response, code >= 400");
						ByteArrayOutputStream ostream = new ByteArrayOutputStream();
						hr.getEntity().writeTo(ostream);
						Log.e(TAG,"Error Body: "+ ostream.toString());
						Log.e(TAG,"Put Body:"+ req.body);
						
						// "Precondition Failed"
						// The item changed server-side, so we have a conflict to resolve...
						// XXX This is a hard problem.
						if (code == 412) {
							Log.e(TAG, "Skipping dirtied item with server-side changes as well");
						}
					}
				} else {
					resp = client.execute(request, new BasicResponseHandler());
				}
			} else if (method.equals("delete")) {
				HttpDelete request = new HttpDelete();
				request.setURI(uri);
				if(req.ifMatch != null) {
					request.setHeader("If-Match", req.ifMatch);
				}
				resp = client.execute(request, new BasicResponseHandler());
			} else {
				HttpGet request = new HttpGet();
				request.setURI(uri);
				if(req.contentType != null) {
					request.setHeader("Content-Type", req.contentType);
				}
				if (req.disposition.equals("xml")) {
					HttpResponse hr = client.execute(request);
					HttpEntity en = hr.getEntity();
					InputStream in = en.getContent();
					XMLResponseParser parse = new XMLResponseParser(in);
					// We can tell from the URL whether we have a single item or a feed
					int mode = (uri.toString().indexOf("/items?") == -1
									&& uri.toString().indexOf("/top?") == -1
									&& uri.toString().indexOf("/collections?") == -1) ?
										XMLResponseParser.MODE_ENTRY : XMLResponseParser.MODE_FEED;
					parse.parse(mode, uri.toString());
					resp = "XML was parsed.";
				} else {
					resp = client.execute(request, new BasicResponseHandler());
				}
			}
			Log.i(TAG, "Response: " + resp);
		} catch (IOException e) {
			Log.e(TAG, "Connection error", e);
		} catch (URISyntaxException e) {
			Log.e(TAG, "URI error", e);
		}
		return resp;
	}
}
