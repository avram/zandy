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

import java.util.ArrayList;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.gimranov.zandy.app.ServerCredentials;
import com.gimranov.zandy.app.XMLResponseParser;
import com.gimranov.zandy.app.data.Attachment;
import com.gimranov.zandy.app.data.Database;
import com.gimranov.zandy.app.data.Item;

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
	
	public ArrayList<APIRequest> deletions;
	public ArrayList<APIRequest> queue;
	
	public int syncMode = -1;
	
	public static final int AUTO_SYNC_STALE_COLLECTIONS = 1;

	public boolean autoMode = false;
	
	private Database db;
	private ServerCredentials cred;
	
	private Handler handler;

	public ZoteroAPITask(Context c)
	{
		queue = new ArrayList<APIRequest>();
		cred = new ServerCredentials(c);
		/* TODO reenable in a working way 
		if (settings.getBoolean("sync_aggressively", false))
			syncMode = AUTO_SYNC_STALE_COLLECTIONS;
		*/
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
	
	@SuppressWarnings("unused")
	public Message doFetch(APIRequest... reqs)
	{	
		int count = reqs.length;
		        
        for (int i = 0; i < count; i++) {
        	if (reqs[i] == null) {
        		Log.d(TAG, "Skipping null request");
        		continue;
        	}
        	
        	// Just in case we missed something, we fix the user ID right here too,
        	// and we set the key as well.
        	reqs[i] = cred.prep(reqs[i]);
        	
        	try {
        		Log.i(TAG, "Executing API call: " + reqs[i].query);
        		reqs[i].issue(db, cred);
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
        	// XXX This is disabled for now
        	if (false && !XMLResponseParser.followNext) {
            	ArrayList<APIRequest> toRemove = new ArrayList<APIRequest>();
        		for (APIRequest r : queue) {
        			if (r.type != APIRequest.ITEMS_CHILDREN) {
        				Log.d(TAG, "Removing request from queue since last page had old items: "+r.query);
        				toRemove.add(r);
        			}
        		}
        		queue.removeAll(toRemove);
        	}
        	Log.i(TAG, "Starting queued requests: " + queue.size() + " requests");
    		APIRequest[] templ = { };
    		APIRequest[] requests = queue.toArray(templ);
        	queue.clear();
        	Log.i(TAG, "Queue size now: "+queue.size());
        	// XXX I suspect that this calling of doFetch from doFetch might be the cause of our
        	// out-of-memory situations. We may be able to accomplish the same thing by expecting
        	// the code listening to our handler to fetch again if QUEUED_MORE is received. In that
        	// case, we could just save our queue here and really return.
        	
        	// XXX Test: Here, we try to use doInBackground instead
    		doInBackground(requests);
    		
    		// Return a message with the number of requests added to the queue
        	Message msg = Message.obtain();
        	msg.arg1 = APIRequest.QUEUED_MORE;
        	msg.arg2 = requests.length;
        	return msg;
        }

        
    	// Here's where we tie in to periodic housekeeping syncs        
    	// If we're already in auto mode (that is, here), just move on
    	if (autoMode) {
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
    		list.add(cred.prep(APIRequest.update(i)));
    	}
    	
    	for (Attachment a : Attachment.queue) {
    		list.add(cred.prep(APIRequest.update(a, db)));
    	}
    	
    	// This queue has deletions, collection memberships, and failing requests
    	// We may want to filter it in the future
    	list.addAll(APIRequest.queue(db));
    	
    	// We're in auto mode...
    	autoMode = true;
    	doInBackground(list.toArray(templ));

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
}
