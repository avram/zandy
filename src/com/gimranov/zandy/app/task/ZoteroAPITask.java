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

package com.gimranov.zandy.app.task;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

import com.gimranov.zandy.app.ServerCredentials;
import com.gimranov.zandy.app.XMLResponseParser;
import com.gimranov.zandy.app.data.Attachment;
import com.gimranov.zandy.app.data.Database;
import com.gimranov.zandy.app.data.Item;
import com.gimranov.zandy.app.data.ItemCollection;

/**
 * Executes one or more API requests asynchronously.
 * 
 * Steps in migration:
 *  1. Move the logic on what kind of request is handled how into the APIRequest itself
 *  2. Throw exceptions when we have errors
 *  3. Call the handlers when provided
 *  4. Move aggressive syncing logic out of ZoteroAPITask itself; it should be elsewhere.
 * 
 * @author ajlyon
 *
 */
public class ZoteroAPITask extends AsyncTask<APIRequest, Message, Message> {
	private static final String TAG = "com.gimranov.zandy.app.task.ZoteroAPITask";
		
	private String key;
	private String userID;
	
	public ArrayList<APIRequest> deletions;
	public ArrayList<APIRequest> queue;
	
	public int syncMode = -1;
	
	public static final int AUTO_SYNC_STALE_COLLECTIONS = 1;

	public boolean autoMode = false;
	
	private Database db;
	
	private Handler handler;
	
	public ZoteroAPITask(String key)
	{
		this.queue = new ArrayList<APIRequest>();
		this.key = key;
	}

	public ZoteroAPITask(Context c)
	{
		this.queue = new ArrayList<APIRequest>();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
		userID = settings.getString("user_id", null);
		key = settings.getString("user_key", null);
		if (settings.getBoolean("sync_aggressively", false))
			syncMode = AUTO_SYNC_STALE_COLLECTIONS;
		deletions = APIRequest.delete(c);
		db = new Database(c);
	}
	
	public void setHandler(Handler h) {
		handler = h;
	}
	
	private Handler getHandler() {
		if (handler == null) {
			handler = new Handler() {};
		}
		return handler;
	}

	@Override
	protected Message doInBackground(APIRequest... params) {
        return doFetch(params);
	} 
	
	public Message doFetch(APIRequest... reqs)
	{	
		int count = reqs.length;
		        
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
        		
        		issue(reqs[i], db, handler);
				
				Log.i(TAG, "Succesfully retrieved API call: " + reqs[i].query);
				reqs[i].succeeded(db);
				
			} catch (APIException e) {
				Log.e(TAG, "Failed to execute API call: " + e.request.query, e);
				e.request.status = APIRequest.REQ_FAILING + e.request.getHttpStatus();
				e.request.save(db);
				Message msg = Message.obtain();
				msg.arg1 = APIRequest.ERROR_UNKNOWN + e.request.getHttpStatus();
				return msg;
			}
			
        	// The XML parser's queue is simply from following continuations in the paged
        	// feed. We shouldn't split out its requests...
	    	if (XMLResponseParser.queue != null && !XMLResponseParser.queue.isEmpty()) {
	        	Log.i(TAG, "Finished call, but adding " +
	        				XMLResponseParser.queue.size() +
	        				" items to queue.");
	    		queue.addAll(XMLResponseParser.queue); 
	    		XMLResponseParser.queue.clear();
	    	} else {
	        	Log.i(TAG, "Finished call, and parser's request queue is empty");
	    	}
        }
        
        // 
        if (queue.size() > 0) {
        	// If the last batch saw unchanged items, don't follow the Atom
        	// continuations; just run the child requests
        	if (!XMLResponseParser.followNext) {
        		for (APIRequest r : queue) {
        			if (r.type != APIRequest.ITEMS_CHILDREN) {
        				Log.d(TAG, "Removing request from queue since last page had old items: "+r.query);
        				queue.remove(r);
        			}
        		}
        	}
        	Log.i(TAG, "Starting queued requests: " + queue.size() + " requests");
    		APIRequest[] templ = { };
    		APIRequest[] requests = queue.toArray(templ);
        	queue.clear();
        	Log.i(TAG, "Now: "+queue.size());
        	// XXX I suspect that this calling of doFetch from doFetch might be the cause of our
        	// out-of-memory situations. We may be able to accomplish the same thing by expecting
        	// the code listening to our handler to fetch again if QUEUED_MORE is received. In that
        	// case, we could just save our queue here and really return.
    		this.doFetch(requests);
    		
    		// Return a message with the number of requests added to the queue
        	Message msg = Message.obtain();
        	msg.arg1 = APIRequest.QUEUED_MORE;
        	msg.arg2 = requests.length;
        	return msg;
        }

        
    	// Here's where we tie in to periodic housekeeping syncs        
    	// If we're already in auto mode (that is, here), just move on
    	if (this.autoMode) {
    		Message msg = Message.obtain();
    		msg.arg1 = APIRequest.UPDATED_DATA;
    		return msg;
    	}
    	
    	Log.d(TAG, "Sending local changes");
    	Item.queue(db);
    	Attachment.queue(db);
    	
    	APIRequest[] templ = {};
    	
    	ArrayList<APIRequest> list = new ArrayList<APIRequest>();
    	for (Item i : Item.queue) {
    		list.add(ServerCredentials.prep(userID, 
    				APIRequest.update(i)));
    	}
    	
    	for (Attachment a : Attachment.queue) {
    		list.add(ServerCredentials.prep(userID, 
    				APIRequest.update(a, db)));
    	}
    	
    	// This queue has deletions, collection memberships, and failing requests
    	// We may want to filter it in the future
    	list.addAll(APIRequest.queue(db));
    	
    	// We're in auto mode...
    	this.autoMode = true;
    	this.doInBackground(list.toArray(templ));

    	// Return a message noting that we've queued more requests
		Message msg = Message.obtain();
    	msg.arg1 = APIRequest.QUEUED_MORE;
    	msg.arg2 = list.size();
    	return msg;
	}
	
	
	@Override
	protected void onPostExecute(Message result) {
		getHandler().sendMessage(result);
	}
	
	
	/**
	 * Executes the specified APIRequest and handles the response
	 * 
	 * This is done synchronously; use the the AsyncTask interface for calls
	 * from the UI thread.
	 * 
	 * @param req
	 * @return
	 * @throws APIException 
	 */
	public static String issue(APIRequest req, Database db, Handler handler) throws APIException {
		// Check that the method makes sense
		String method = req.method == null ? null : req.method.toLowerCase();
		if (!"get".equals(method)
			&& !"post".equals(method)
			&& !"delete".equals(method)
			&& !"put".equals(method)
		) {
			Log.d(TAG, "Invalid method: "+method + " in request "+req.query);
			throw new APIException(APIException.INVALID_METHOD, req);
		}
		String resp = "";
		try {
			XMLResponseParser parse = new XMLResponseParser();
			
			// Append the key, if defined, to all requests
			if (req.key != null && req.key != "") {
				String symbol = req.query.indexOf("?") == -1 ? "?" : "&";
				req.query += symbol + "key=" + req.key;

			}

			Log.i(TAG, "Request "+ req.method +": " + req.query);		

			URI uri = new URI(req.query);
			HttpClient client = new DefaultHttpClient();
			// The default implementation includes an Expect: header, which
			// confuses the Zotero servers.
			client.getParams().setParameter("http.protocol.expect-continue", false);
			// We also need to send our data nice and raw.
			client.getParams().setParameter("http.protocol.content-charset", "UTF-8");

			/* It would be good to rework this mess to be less repetitive */
			// TODO As soon as we put the full request info into APIRequest, we
			// can simplify this. In fact, we can make APIRequest run itself using
			// its own methods and just spit out a response.
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
					
					// Set the status to the received code
					req.status = code;
					
					if (code < 400) {
						HttpEntity he = hr.getEntity();
						InputStream in = he.getContent();
						parse.setInputStream(in);
						if (req.updateKey != null && req.updateType != null)
							parse.update(req.updateType, req.updateKey);
						// The response on POST in XML mode (new item) is a feed
						parse.parse(XMLResponseParser.MODE_FEED, uri.toString(), db);
					} else {
						ByteArrayOutputStream ostream = new ByteArrayOutputStream();
						hr.getEntity().writeTo(ostream);
						Log.e(TAG,"Error Body: "+ ostream.toString());
						Log.e(TAG,"Post Body:"+ req.body);
						throw new APIException(APIException.HTTP_ERROR, ostream.toString(), req);
					}
				} else {
					BasicResponseHandler brh = new BasicResponseHandler();
					try {
						resp = client.execute(request, brh);
						req.getHandler().onComplete(req);
					} catch (ClientProtocolException e) {
						Log.e(TAG, "Exception thrown issuing POST request: ", e);
						Log.e(TAG,"Post Body: "+ req.body);
						req.getHandler().onError(req, e);
						throw new APIException(APIException.HTTP_ERROR, req);
					}
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
					
					// Set the status to the received code
					req.status = code;
					
					if (code < 400) {
						HttpEntity he = hr.getEntity();
						InputStream in = he.getContent();
						parse.setInputStream(in);
						parse.parse(XMLResponseParser.MODE_ENTRY, uri.toString(), db);
						req.getHandler().onComplete(req);
					} else {
						ByteArrayOutputStream ostream = new ByteArrayOutputStream();
						hr.getEntity().writeTo(ostream);
						Log.e(TAG,"Error Body: "+ ostream.toString());
						Log.e(TAG,"Put Body:"+ req.body);
						req.getHandler().onError(req, APIRequest.HTTP_ERROR_UNSPECIFIED);
						// "Precondition Failed"
						// The item changed server-side, so we have a conflict to resolve...
						// Cf. issue #18
						if (code == 412) {
							Log.e(TAG, "Dirtied item with server-side changes as well");
							req.getHandler().onError(req, APIRequest.HTTP_ERROR_CONFLICT);
						}
						throw new APIException(APIException.HTTP_ERROR, ostream.toString(), req);
					}
				} else {
					BasicResponseHandler brh = new BasicResponseHandler();
					try {
						resp = client.execute(request, brh);
						req.getHandler().onComplete(req);
					} catch (ClientProtocolException e) {
						Log.e(TAG, "Exception thrown issuing PUT request: ", e);
						req.getHandler().onError(req, e);
						throw new APIException(APIException.HTTP_ERROR, req);
					}
				}
			} else if (method.equals("delete")) {
				HttpDelete request = new HttpDelete();
				request.setURI(uri);
				if(req.ifMatch != null) {
					request.setHeader("If-Match", req.ifMatch);
				}
				
				BasicResponseHandler brh = new BasicResponseHandler();
				try {
					resp = client.execute(request, brh);
					req.getHandler().onComplete(req);
				} catch (ClientProtocolException e) {
					Log.e(TAG, "Exception thrown issuing DELETE request: ", e);
					req.getHandler().onError(req, e);
					throw new APIException(APIException.HTTP_ERROR, req);
				}
			} else {
				HttpGet request = new HttpGet();
				request.setURI(uri);
				if(req.contentType != null) {
					request.setHeader("Content-Type", req.contentType);
				}
				if (req.disposition.equals("xml")) {
					HttpResponse hr = client.execute(request);
					
					int code = hr.getStatusLine().getStatusCode();
					Log.d(TAG, code + " : "+ hr.getStatusLine().getReasonPhrase());
					
					// Set the status to the received code
					req.status = code;
					
					if (code < 400) {
						HttpEntity he = hr.getEntity();
						InputStream in = he.getContent();
						parse.setInputStream(in);
						// We can tell from the URL whether we have a single item or a feed
						int mode = (uri.toString().indexOf("/items?") == -1
										&& uri.toString().indexOf("/top?") == -1
										&& uri.toString().indexOf("/collections?") == -1
										&& uri.toString().indexOf("/children?") == -1) ?
											XMLResponseParser.MODE_ENTRY : XMLResponseParser.MODE_FEED;
						parse.parse(mode, uri.toString(), db);
						req.getHandler().onComplete(req);
						
					} else {
						ByteArrayOutputStream ostream = new ByteArrayOutputStream();
						hr.getEntity().writeTo(ostream);
						
						Log.e(TAG,"Error Body: "+ ostream.toString());
						Log.e(TAG,"Put Body:"+ req.body);
						
						// "Precondition Failed"
						// The item changed server-side, so we have a conflict to resolve...
						if (code == 412) {
							Log.e(TAG, "Dirtied item with server-side changes as well");
							req.getHandler().onError(req, APIRequest.HTTP_ERROR_CONFLICT);
						} else {
							req.getHandler().onError(req, APIRequest.HTTP_ERROR_UNSPECIFIED);
						}
						
						throw new APIException(APIException.HTTP_ERROR, ostream.toString(), req);
					}
				} else {
					BasicResponseHandler brh = new BasicResponseHandler();
					try {
						resp = client.execute(request, brh);
						req.getHandler().onComplete(req);
					} catch (ClientProtocolException e) {
						Log.e(TAG, "Exception thrown issuing GET request: ", e);
						req.getHandler().onError(req, e);
						throw new APIException(APIException.HTTP_ERROR, req);
					}
				}
			}
			Log.i(TAG, "Response: " + resp);
		} catch (IOException e) {
			Log.e(TAG, "Connection error", e);
			req.getHandler().onError(req, e);
			throw new APIException(APIException.HTTP_ERROR, req);
		} catch (URISyntaxException e) {
			Log.e(TAG, "URI error", e);
			req.getHandler().onError(req, e);
			throw new APIException(APIException.INVALID_URI, req);
		}
		return resp;
	}
}
