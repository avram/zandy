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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.zotero.client.ServerCredentials;
import org.zotero.client.XMLResponseParser;

import android.os.AsyncTask;
import android.util.Log;

public class ZoteroAPITask extends AsyncTask<String, Integer, JSONArray[]> {
	
	private String key;
	
	public ZoteroAPITask(String key)
	{
		this.key = key;
	}

	@Override
	protected JSONArray[] doInBackground(String... params) {
        return doFetch(params);
	} 
	
	public JSONArray[] doFetch(String... urls)
	{	
		int count = urls.length;
		
		JSONArray[] ret = new JSONArray[count];

        
        for (int i = 0; i < count; i++) {
        	try {
        		Log.i("org.zotero.client.task.ZoteroAPITask", "Executing API call: " + urls[i]);

        		// Pretty hacky-- we just append the key and JSON content reques
        		if (urls[i].indexOf("?") == -1)
        			urls[i] += "?";
        		else
        			urls[i] += "&";
        		urls[i] += "key=" + this.key + "&content=json";
        		
        		if (urls[i].startsWith("/users/5770"+ServerCredentials.ITEMS)
        				|| urls[i].startsWith("/users/5770"+ServerCredentials.COLLECTIONS)
        				|| urls[i].startsWith("/users/5770"+ServerCredentials.TAGS)
        				|| urls[i].startsWith("/users/5770"+ServerCredentials.GROUPS)) {
        			// We have an XML-formatted response-- treat it as such
        			getParsedResponse(ServerCredentials.APIBASE + urls[i]);
        			continue;
        		}
        		
        		// The remaining requests types are JSON-based
        		String strResponse = getResponse(ServerCredentials.APIBASE + urls[i]);
				
				if(!strResponse.replace("\n", "").startsWith("["))
				{
					// wrap in JSONArray delimiters
					strResponse = "[" + strResponse + "]";
				}
				
				ret[i] = new JSONArray(strResponse);
				
				Log.i("org.zotero.client.task.ZoteroAPITask", "Succesfully retrieved API call: " + urls[i]);
				
			} catch (Exception e) {
				// TODO: determine if this is due to needing re-auth
				Log.e("org.zotero.client.task.ZoteroAPITask", "Failed to execute API call: " + urls[i], e);
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
	
	/** Gets a response from the API server */
	/* This is for JSON calls, I suppose */
	public String getResponse(String strURL) throws Exception {
		Log.i("org.zotero.client.task.ZoteroAPITask", "Requesting: " + strURL);
		URI uri = new URI(strURL);
		HttpClient client = new DefaultHttpClient();
		HttpGet request = new HttpGet();
		request.setURI(uri);
		String content = client.execute(request, new BasicResponseHandler());
		Log.i("org.zotero.client.task.ZoteroAPITask", "Response: " + content);
		return content;
	}

	/** Gets a response from the API server */
	public void getParsedResponse(String strURL) throws Exception {
		Log.i("org.zotero.client.task.ZoteroAPITask", "Requesting parsed: " + strURL);
		URL url = new URL(strURL);
		HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();

		InputStream in = urlConnection.getInputStream();

		XMLResponseParser parse = new XMLResponseParser(in);
		parse.parse();
		urlConnection.disconnect();
	}
}
