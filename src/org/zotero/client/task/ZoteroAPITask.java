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

package org.zotero.client.task;

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
import org.zotero.client.XMLResponseParser;
import org.zotero.client.data.Item;
import org.zotero.client.data.ItemCollection;

import android.os.AsyncTask;
import android.util.Log;

public class ZoteroAPITask extends AsyncTask<APIRequest, Integer, JSONArray[]> {
	
	private String key;
	
	public ZoteroAPITask(String key)
	{
		this.key = key;
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
        	try {
        		Log.i("org.zotero.client.task.ZoteroAPITask", "Executing API call: " + reqs[i].query);
        		
        		reqs[i].key = key;
        		
        		String strResponse = issue(reqs[i]);
				
				Log.i("org.zotero.client.task.ZoteroAPITask", "Succesfully retrieved API call: " + reqs[i].query);
				
			} catch (Exception e) {
				// TODO: determine if this is due to needing re-auth
				Log.e("org.zotero.client.task.ZoteroAPITask", "Failed to execute API call: " + reqs[i].query, e);
				return null;
			}
            publishProgress((int) ((i / (float) count) * 100));
        }
             
        return ret;
	}
	
	
	@Override
	protected void onPostExecute(JSONArray[] result) {
		// invoked on the UI thread
		
        if (result == null)
        {
        	Log.e("org.zotero.client.task.ZoteroAPITask", "Returned NULL; looks like a problem communicating with server; review stack trace.");
        	// there was an error
        	String text = "Error communicating with server.";

        	Log.i("org.zotero.client.task.ZoteroAPITask", text);
        } else {
        	Log.i("org.zotero.client.task.ZoteroAPITask", "Finished call, got something back!");
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
		if (method != "get"
			&& method != "post"
			&& method != "delete"
			&& method != "put"
		) {
			// TODO Throw an exception here.
			return null;
		}
		String resp = "";
		try {
			// Append content=json everywhere
			if (req.query.indexOf("?") != -1) {
				req.query += "&content=json";
			} else {
				req.query += "?content=json";
			}
			
			// Append the key, if defined, to all requests
			if (req.key != null && req.key != "") {
				req.query += "&key=" + req.key;

			}
			Log.i("org.zotero.client.task.ZoteroAPITask", "Request "+ req.method +": " + req.query);		

			URI uri = new URI(req.query);
			HttpClient client = new DefaultHttpClient();

			/* It would be good to rework this mess to be less repetitive */
			if (method == "post") {
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
					StringEntity entity = new StringEntity(req.body);
					request.setEntity(entity);
				}
				if (req.disposition == "xml") {
					InputStream in = client.execute(request).getEntity().getContent();
					XMLResponseParser parse = new XMLResponseParser(in);
					parse.parse();
					resp = "XML was parsed.";
				} else {
					resp = client.execute(request, new BasicResponseHandler());
				}
			} else if (method == "put") {
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
					StringEntity entity = new StringEntity(req.body);
					request.setEntity(entity);
				}
				if (req.disposition == "xml") {
					InputStream in = client.execute(request).getEntity().getContent();
					XMLResponseParser parse = new XMLResponseParser(in);
					parse.parse();
					resp = "XML was parsed.";
				} else {
					resp = client.execute(request, new BasicResponseHandler());
				}
			} else if (method == "delete") {
				HttpDelete request = new HttpDelete();
				request.setURI(uri);
				if(req.ifMatch != null) {
					request.setHeader("If-Match", req.ifMatch);
				}
				if (req.disposition == "xml") {
					InputStream in = client.execute(request).getEntity().getContent();
					XMLResponseParser parse = new XMLResponseParser(in);
					parse.parse();
					resp = "XML was parsed.";
				} else {
					resp = client.execute(request, new BasicResponseHandler());
				}
			} else {
				HttpGet request = new HttpGet();
				request.setURI(uri);
				if(req.contentType != null) {
					request.setHeader("Content-Type", req.contentType);
				}
				if (req.disposition == "xml") {
					HttpResponse hr = client.execute(request);
					HttpEntity en = hr.getEntity();
					InputStream in = en.getContent();
					XMLResponseParser parse = new XMLResponseParser(in);
					parse.parse();
					resp = "XML was parsed.";
				} else {
					resp = client.execute(request, new BasicResponseHandler());
				}
			}
			Log.i("org.zotero.client.task.ZoteroAPITask", "Response: " + resp);
		} catch (IOException e) {
			Log.e("org.zotero.client.task.ZoteroAPITask", "Connection error", e);
		} catch (URISyntaxException e) {
			Log.e("org.zotero.client.task.ZoteroAPITask", "URI error", e);
		}
		return resp;
	}	
	
	/**
	 * Removes specified item from the specified collection on the server.
	 * 
	 * @param item			Item to be removed. If null, the request is discarded.
	 * @param collection	Collection to remove the item from. Does not check
	 * 						if the item is a member before making request. If
	 * 						null, the request is discarded.
	 */
	public void removeItemFromCollection(Item item, ItemCollection collection) {
		
	}
}
