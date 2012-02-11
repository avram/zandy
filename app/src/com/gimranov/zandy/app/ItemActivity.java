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
package com.gimranov.zandy.app;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.gimranov.zandy.app.data.Database;
import com.gimranov.zandy.app.data.Item;
import com.gimranov.zandy.app.data.ItemAdapter;
import com.gimranov.zandy.app.data.ItemCollection;
import com.gimranov.zandy.app.task.APIEvent;
import com.gimranov.zandy.app.task.APIRequest;
import com.gimranov.zandy.app.task.ZoteroAPITask;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class ItemActivity extends ListActivity {

	private static final String TAG = "com.gimranov.zandy.app.ItemActivity";
	
	static final int DIALOG_VIEW = 0;
	static final int DIALOG_NEW = 1;
	static final int DIALOG_SORT = 2;
	static final int DIALOG_IDENTIFIER = 3;
	static final int DIALOG_PROGRESS = 6;

	/**
	 * Allowed sort orderings
	 */
	static final String[] SORTS = {
		"item_year, item_title",
		"item_creator, item_year",
		"item_title, item_year",
		"timestamp ASC, item_title"
	};

	/**
	 * Strings providing the names of each ordering, respectively
	 */
	static final int[] SORT_NAMES = {
		R.string.sort_year_title,
		R.string.sort_creator_year,
		R.string.sort_title_year,
		R.string.sort_modified_title
	};
	
	private String collectionKey;
	private String query;
	private Database db;
		
	private ProgressDialog mProgressDialog;
	private ProgressThread progressThread;
	
	public String sortBy = "item_year, item_title";
	
	final Handler syncHandler = new Handler() {
		public void handleMessage(Message msg) {
			Log.d(TAG,"received message: "+msg.arg1);
			refreshView();
			
			if (msg.arg1 == APIRequest.UPDATED_DATA) {
				refreshView();
				return;
			}
			
			if (msg.arg1 == APIRequest.QUEUED_MORE) {
				Toast.makeText(getApplicationContext(),
						getResources().getString(R.string.sync_queued_more, msg.arg2), 
        				Toast.LENGTH_SHORT).show();
				return;
			}
			
			if (msg.arg1 == APIRequest.BATCH_DONE) {
				Toast.makeText(getApplicationContext(),
						getResources().getString(R.string.sync_complete), 
        				Toast.LENGTH_SHORT).show();
				return;
			}
			
			if (msg.arg1 == APIRequest.ERROR_UNKNOWN) {
				Toast.makeText(getApplicationContext(),
						getResources().getString(R.string.sync_error), 
        				Toast.LENGTH_SHORT).show();
				return;
			}
		}
	};
	
    private APIEvent mEvent = new APIEvent() {
		private int updates = 0;
		
		@Override
		public void onComplete(APIRequest request) {
			Message msg = syncHandler.obtainMessage();
			msg.arg1 = APIRequest.BATCH_DONE;
			syncHandler.sendMessage(msg);
			Log.d(TAG, "fired oncomplete");
		}

		@Override
		public void onUpdate(APIRequest request) {
			updates++;
			
			if (updates % 10 == 0) {
				Message msg = syncHandler.obtainMessage();
				msg.arg1 = APIRequest.UPDATED_DATA;
				syncHandler.sendMessage(msg);
			} else {
				// do nothing
			}
		}

		@Override
		public void onError(APIRequest request, Exception exception) {
			Log.e(TAG, "APIException caught", exception);
			Message msg = syncHandler.obtainMessage();
			msg.arg1 = APIRequest.ERROR_UNKNOWN;
			syncHandler.sendMessage(msg);
		}

		@Override
		public void onError(APIRequest request, int error) {
			Log.e(TAG, "API error caught");
			Message msg = syncHandler.obtainMessage();
			msg.arg1 = APIRequest.ERROR_UNKNOWN;
			syncHandler.sendMessage(msg);
		}
	};

	protected Bundle b = new Bundle();
	
	/**
	 * Refreshes the current list adapter
	 */
	private void refreshView() {
		ItemAdapter adapter = (ItemAdapter) getListAdapter();
		Cursor newCursor = prepareCursor();
		adapter.changeCursor(newCursor);
		adapter.notifyDataSetChanged();
		Log.d(TAG, "refreshing view on request");
	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        db = new Database(this);
		
        setContentView(R.layout.items);

        Intent intent = getIntent();
        collectionKey = intent.getStringExtra("com.gimranov.zandy.app.collectionKey");

        ItemCollection coll = ItemCollection.load(collectionKey, db);
       	APIRequest req;

        if (coll != null) {        	
       		req = APIRequest.fetchItems(coll, false, 
       				new ServerCredentials(this));
        } else {        	
       		req = APIRequest.fetchItems(false, 
       				new ServerCredentials(this));
        }

        prepareAdapter();
        
		ItemAdapter adapter = (ItemAdapter) getListAdapter();
		Cursor cur = adapter.getCursor();
		
		if (intent.getBooleanExtra("com.gimranov.zandy.app.rerequest", false)
				|| cur == null
				|| cur.getCount() == 0) {
			
        	if (!ServerCredentials.check(getBaseContext())) {
            	Toast.makeText(getBaseContext(), getResources().getString(R.string.sync_log_in_first), 
        				Toast.LENGTH_SHORT).show();
            	return;
        	}
        	
    		Toast.makeText(this,
    				getResources().getString(R.string.collection_empty),
    				Toast.LENGTH_SHORT).show();
			Log.d(TAG, "Running a request to populate missing items");
    		ZoteroAPITask task = new ZoteroAPITask(this);
    		req.setHandler(mEvent);
    		task.execute(req);

		}        
        
        ListView lv = getListView();
        lv.setOnItemClickListener(new OnItemClickListener() {
        	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
     			// If we have a click on an item, do something...
        		ItemAdapter adapter = (ItemAdapter) parent.getAdapter();
        		Cursor cur = adapter.getCursor();
        		// Place the cursor at the selected item
        		if (cur.moveToPosition(position)) {
        			// and load an activity for the item
        			Item item = Item.load(cur);
        			
        			Log.d(TAG, "Loading item data with key: "+item.getKey());
    				// We create and issue a specified intent with the necessary data
    		    	Intent i = new Intent(getBaseContext(), ItemDataActivity.class);
    		    	i.putExtra("com.gimranov.zandy.app.itemKey", item.getKey());
    		    	i.putExtra("com.gimranov.zandy.app.itemDbId", item.dbId);
    		    	startActivity(i);
        		} else {
        			// failed to move cursor-- show a toast
            		TextView tvTitle = (TextView)view.findViewById(R.id.item_title);
            		Toast.makeText(getApplicationContext(),
            				getResources().getString(R.string.cant_open_item, tvTitle.getText()), 
            				Toast.LENGTH_SHORT).show();
        		}
        	}
        });
    }

	protected void onResume() {
		ItemAdapter adapter = (ItemAdapter) getListAdapter();
		adapter.changeCursor(prepareCursor());
    	super.onResume();
    }
    
    public void onDestroy() {
		ItemAdapter adapter = (ItemAdapter) getListAdapter();
		Cursor cur = adapter.getCursor();
		if(cur != null) cur.close();
		if (db != null) db.close();
		super.onDestroy();
    }
    
    private void prepareAdapter() {
		ItemAdapter adapter = new ItemAdapter(this, prepareCursor());
        setListAdapter(adapter);
    }
    
    private Cursor prepareCursor() {
    	Cursor cursor;
        // Be ready for a search
        Intent intent = getIntent();
        
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
        	query = intent.getStringExtra(SearchManager.QUERY);
        	cursor = getCursor(query);
          	this.setTitle(getResources().getString(R.string.search_results, query));
        } else if (query != null) {
           	cursor = getCursor(query);
          	this.setTitle(getResources().getString(R.string.search_results, query));
        } else if (intent.getStringExtra("com.gimranov.zandy.app.tag") != null) {
        	String tag = intent.getStringExtra("com.gimranov.zandy.app.tag");
        	Query q = new Query();
        	q.set("tag", tag);
        	cursor = getCursor(q);
        	this.setTitle(getResources().getString(R.string.tag_viewing_items, tag));
     	} else {
	        collectionKey = intent.getStringExtra("com.gimranov.zandy.app.collectionKey");

	        ItemCollection coll;

	        if (collectionKey != null) {
	        	coll = ItemCollection.load(collectionKey, db);
	        	cursor = getCursor(coll);
	        	this.setTitle(coll.getTitle());
	        } else {
	        	cursor = getCursor();
	        	this.setTitle(getResources().getString(R.string.all_items));
	        }
        }
        return cursor;
    }
    
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_NEW:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(getResources().getString(R.string.item_type))
					// XXX i18n
		    	    .setItems(Item.ITEM_TYPES_EN, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int pos) {
		    	            Item item = new Item(getBaseContext(), Item.ITEM_TYPES[pos]);
		    	            item.dirty = APIRequest.API_DIRTY;
		    	            item.save(db);
		    	            if (collectionKey != null) {
		    	            	ItemCollection coll = ItemCollection.load(collectionKey, db);
		    	            	if (coll != null) {
		    	            		coll.loadChildren(db);
		    	            		coll.add(item);
		    	            		coll.saveChildren(db);
		    	            	}
		    	            }
		        			Log.d(TAG, "Loading item data with key: "+item.getKey());
		    				// We create and issue a specified intent with the necessary data
		    		    	Intent i = new Intent(getBaseContext(), ItemDataActivity.class);
		    		    	i.putExtra("com.gimranov.zandy.app.itemKey", item.getKey());
		    		    	startActivity(i);
		    	        }
		    	    });
			AlertDialog dialog = builder.create();
			return dialog;
		case DIALOG_SORT:
			
			// We generate the sort name list for our current locale
			String[] sorts = new String[SORT_NAMES.length];
			for (int j = 0; j < SORT_NAMES.length; j++) {
				sorts[j] = getResources().getString(SORT_NAMES[j]);
			}
			
			AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
			builder2.setTitle(getResources().getString(R.string.set_sort_order))
		    	    .setItems(sorts, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int pos) {
							Cursor cursor;
							setSortBy(SORTS[pos]);
							if (collectionKey != null)
								cursor = getCursor(ItemCollection.load(collectionKey, db));
							else if (query != null)
								cursor = getCursor(query);
							else
								cursor = getCursor();
							ItemAdapter adapter = (ItemAdapter) getListAdapter();
							adapter.changeCursor(cursor);
		        			Log.d(TAG, "Re-sorting by: "+SORTS[pos]);
		    	        }
		    	    });
			AlertDialog dialog2 = builder2.create();
			return dialog2;
		case DIALOG_PROGRESS:
			Log.d(TAG, "_____________________dialog_progress");
			mProgressDialog = new ProgressDialog(this);
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			mProgressDialog.setIndeterminate(true);
			mProgressDialog.setMessage(getResources().getString(R.string.identifier_looking_up));
			return mProgressDialog;
		case DIALOG_IDENTIFIER:
			final EditText input = new EditText(this);
			input.setHint(getResources().getString(R.string.identifier_hint));
			
			final ItemActivity current = this;
			
			dialog = new AlertDialog.Builder(this)
	    	    .setTitle(getResources().getString(R.string.identifier_message))
	    	    .setView(input)
	    	    .setPositiveButton(getResources().getString(R.string.menu_search), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
	    	            Editable value = input.getText();
	    	            // run search
	    	            Bundle c = new Bundle();
	    	            c.putString("mode", "isbn");
	    	            c.putString("identifier", value.toString());
	    	            removeDialog(DIALOG_PROGRESS);
	    	            ItemActivity.this.b = c;
	    	            showDialog(DIALOG_PROGRESS);
	    	        }
	    	    }).setNeutralButton(getResources().getString(R.string.scan), new DialogInterface.OnClickListener() {
	    	        public void onClick(DialogInterface dialog, int whichButton) {
	    	        		IntentIntegrator integrator = new IntentIntegrator(current);
	    	        		integrator.initiateScan();
	    	        	}
	    	    }).setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
	    	        public void onClick(DialogInterface dialog, int whichButton) {
	    	        	// do nothing
	    	        }
	    	    }).create();
			return dialog;
		default:
			return null;
		}
	}
    
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch(id) {
		case DIALOG_PROGRESS:
			Log.d(TAG, "_____________________dialog_progress_prepare");
		}
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.zotero_menu, menu);

        // Turn on sort item
        MenuItem sort = menu.findItem(R.id.do_sort);
        sort.setEnabled(true);
        sort.setVisible(true);
        
        // Turn on search item
        MenuItem search = menu.findItem(R.id.do_search);
        search.setEnabled(true);
        search.setVisible(true);
        
        // Turn on identifier item
        MenuItem identifier = menu.findItem(R.id.do_identifier);
        identifier.setEnabled(true);
        identifier.setVisible(true);
        
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.do_sync:
        	if (!ServerCredentials.check(getBaseContext())) {
            	Toast.makeText(getBaseContext(), getResources().getString(R.string.sync_log_in_first), 
        				Toast.LENGTH_SHORT).show();
            	return true;
        	}
        	
			// Get credentials
			ServerCredentials cred = new ServerCredentials(getBaseContext());
			
        	// Make this a collection-specific sync, preceding by de-dirtying
        	Item.queue(db);
			ArrayList<APIRequest> list = new ArrayList<APIRequest>();
			APIRequest[] templ = {};
	    	for (Item i : Item.queue) {
        		Log.d(TAG, "Adding dirty item to sync: "+i.getTitle());
	    		list.add(cred.prep(APIRequest.update(i)));
	    	}
        	
        	if (collectionKey == null) {
            	Log.d(TAG, "Adding sync request for all items");
            	APIRequest req = APIRequest.fetchItems(false, cred);
    			req.setHandler(mEvent);
    			list.add(req);
        	} else {
            	Log.d(TAG, "Adding sync request for collection: " + collectionKey);
            	APIRequest req = APIRequest.fetchItems(collectionKey, true, cred);
            	req.setHandler(mEvent);
            	list.add(req);
        	}
        	APIRequest[] reqs = list.toArray(templ);
        	
			ZoteroAPITask task = new ZoteroAPITask(getBaseContext());
			task.setHandler(syncHandler);
			task.execute(reqs);
        	Toast.makeText(getApplicationContext(), getResources().getString(R.string.sync_started), 
    				Toast.LENGTH_SHORT).show();
            return true;
        case R.id.do_new:
        	removeDialog(DIALOG_NEW);
        	showDialog(DIALOG_NEW);
            return true;
        case R.id.do_identifier:
        	removeDialog(DIALOG_IDENTIFIER);
        	showDialog(DIALOG_IDENTIFIER);
            return true;
        case R.id.do_search:
        	onSearchRequested();
            return true;
        case R.id.do_prefs:
	    	Intent i = new Intent(getBaseContext(), SettingsActivity.class);
	    	Log.d(TAG, "Intent for class:  "+i.getClass().toString());
	    	startActivity(i);
	    	return true;
        case R.id.do_sort:
        	removeDialog(DIALOG_SORT);
        	showDialog(DIALOG_SORT);
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
	
    /* Sorting */
	public void setSortBy(String sort) {
		this.sortBy = sort;
	}
	
	/* Handling the ListView and keeping it up to date */
	public Cursor getCursor() {
		Cursor cursor = db.query("items", Database.ITEMCOLS, null, null, null, null, this.sortBy, null);
		if (cursor == null) {
			Log.e(TAG, "cursor is null");
		}
		return cursor;
	}

	public Cursor getCursor(ItemCollection parent) {
		String[] args = { parent.dbId };
		Cursor cursor = db.rawQuery("SELECT item_title, item_type, item_content, etag, dirty, " +
				"items._id, item_key, item_year, item_creator, timestamp, item_children " +
				" FROM items, itemtocollections WHERE items._id = item_id AND collection_id=? ORDER BY "+this.sortBy,
				args);
		if (cursor == null) {
			Log.e(TAG, "cursor is null");
		}
		return cursor;
	}

	public Cursor getCursor(String query) {
		String[] args = { "%"+query+"%", "%"+query+"%" };
		Cursor cursor = db.rawQuery("SELECT item_title, item_type, item_content, etag, dirty, " +
				"_id, item_key, item_year, item_creator, timestamp, item_children " +
				" FROM items WHERE item_title LIKE ? OR item_creator LIKE ?" +
				" ORDER BY "+this.sortBy,
				args);
		if (cursor == null) {
			Log.e(TAG, "cursor is null");
		}
		return cursor;
	}
	
	public Cursor getCursor(Query query) {
		return query.query(db);
	}

	/* Thread and helper to run lookups */
	
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		Log.d(TAG, "_____________________on_activity_result");
		
		IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
		if (scanResult != null) {
		    // handle scan result
			Bundle b = new Bundle();
			b.putString("mode", "isbn");
			b.putString("identifier", scanResult.getContents());
			if (scanResult != null
					&& scanResult.getContents() != null) {
				Log.d(TAG, b.getString("identifier"));
				progressThread = new ProgressThread(handler, b);
				progressThread.start();
				this.b = b;
				removeDialog(DIALOG_PROGRESS);			
				showDialog(DIALOG_PROGRESS);
			} else {
				Toast.makeText(getApplicationContext(),
						getResources().getString(R.string.identifier_scan_failed), 
	    				Toast.LENGTH_SHORT).show();
			}
		} else {
			Toast.makeText(getApplicationContext(),
					getResources().getString(R.string.identifier_scan_failed), 
    				Toast.LENGTH_SHORT).show();
		}
	}
	
	final Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			Log.d(TAG, "______________________handle_message");
			if (ProgressThread.STATE_DONE == msg.arg2) {
				Bundle data = msg.getData();
				String itemKey = data.getString("itemKey");
				if (itemKey != null) {
					if (collectionKey != null) {
						Item item = Item.load(itemKey, db);
						ItemCollection coll = ItemCollection.load(collectionKey, db);
						coll.add(item);
						coll.saveChildren(db);
					}
					
					mProgressDialog.dismiss();
					mProgressDialog = null;
					
					Log.d(TAG, "Loading new item data with key: "+itemKey);
    				// We create and issue a specified intent with the necessary data
    		    	Intent i = new Intent(getBaseContext(), ItemDataActivity.class);
    		    	i.putExtra("com.gimranov.zandy.app.itemKey", itemKey);
    		    	startActivity(i);
				}
				return;
			}
			
			if (ProgressThread.STATE_PARSING == msg.arg2) {
				mProgressDialog.setMessage(getResources().getString(R.string.identifier_processing));
				return;
			}
			
			if (ProgressThread.STATE_ERROR == msg.arg2) {
				dismissDialog(DIALOG_PROGRESS);
				Toast.makeText(getApplicationContext(),
						getResources().getString(R.string.identifier_lookup_failed), 
	    				Toast.LENGTH_SHORT).show();
				progressThread.setState(ProgressThread.STATE_DONE);
				return;
			}
		}
	};
	
	private class ProgressThread extends Thread {
		Handler mHandler;
		Bundle arguments;
		final static int STATE_DONE = 5;
		final static int STATE_FETCHING = 1;
		final static int STATE_PARSING = 6;
		final static int STATE_ERROR = 7;
		
		int mState;
		
		ProgressThread(Handler h, Bundle b) {
			mHandler = h;
			arguments = b;
			Log.d(TAG, "_____________________thread_constructor");
		}
		
		public void run() {
			Log.d(TAG, "_____________________thread_run");
			mState = STATE_FETCHING;
			
			// Setup
			String identifier = arguments.getString("identifier");
			String mode = arguments.getString("mode");
			URL url;
			String urlstring;
			
			String response = "";
			
			if ("isbn".equals(mode)) {
				urlstring = "http://xisbn.worldcat.org/webservices/xid/isbn/"
							+ identifier
							+ "?method=getMetadata&fl=*&format=json&count=1";
			} else {
				urlstring = "";
			}
			
			try {
				Log.d(TAG, "Fetching from: "+urlstring);
				url = new URL(urlstring);
                
                /* Open a connection to that URL. */
                URLConnection ucon = url.openConnection();                
                /*
                 * Define InputStreams to read from the URLConnection.
                 */
                InputStream is = ucon.getInputStream();
                BufferedInputStream bis = new BufferedInputStream(is, 16000);

                ByteArrayBuffer baf = new ByteArrayBuffer(50);
                int current = 0;
                
                /*
                 * Read bytes to the Buffer until there is nothing more to read(-1).
                 */
    			while (mState == STATE_FETCHING 
    					&& (current = bis.read()) != -1) {
                        baf.append((byte) current);
                }
                response = new String(baf.toByteArray());
    			Log.d(TAG, response);
    			
    			
	        } catch (IOException e) {
	                Log.e(TAG, "Error: ",e);
	        }

			Message msg = mHandler.obtainMessage();
        	msg.arg2 = STATE_PARSING;
        	mHandler.sendMessage(msg);
        	
        	/*
        	 * {
 "stat":"ok",
 "list":[{
	"url":["http://www.worldcat.org/oclc/177669176?referer=xid"],
	"publisher":"O'Reilly",
	"form":["BA"],
	"lccn":["2004273129"],
	"lang":"eng",
	"city":"Sebastopol, CA",
	"author":"by Mark Lutz and David Ascher.",
	"ed":"2nd ed.",
	"year":"2003",
	"isbn":["0596002815"],
	"title":"Learning Python",
	"oclcnum":["177669176",
..
	 "748093898"]}]}
        	 */
        	
        	// This is OCLC-specific logic
        	try {
				JSONObject result = new JSONObject(response);
				
				if (!result.getString("stat").equals("ok")) {
					Log.e(TAG, "Error response received");
					msg = mHandler.obtainMessage();
		        	msg.arg2 = STATE_ERROR;
		        	mHandler.sendMessage(msg);
		        	return;
				}
				
				result = result.getJSONArray("list").getJSONObject(0);
				String form = result.getJSONArray("form").getString(0);
				String type;
				
				if ("AA".equals(form)) type = "audioRecording";
				else if ("VA".equals(form)) type = "videoRecording";
				else if ("FA".equals(form)) type = "film";
				else type = "book";
				
				// TODO Fix this
				type = "book";
				
				Item item = new Item(getBaseContext(), type);
				
				JSONObject content = item.getContent();
				
				if (result.has("lccn")) {
					String lccn = "LCCN: " + result.getJSONArray("lccn").getString(0);
					content.put("extra", lccn);
				}
				
				if (result.has("isbn")) {
					content.put("ISBN", result.getJSONArray("isbn").getString(0));
				}
				
				content.put("title", result.optString("title", ""));
				content.put("place", result.optString("city", ""));
				content.put("edition", result.optString("ed", ""));
				content.put("language", result.optString("lang", ""));
				content.put("publisher", result.optString("publisher", ""));
				content.put("date", result.optString("year", ""));
				
				item.setTitle(result.optString("title", ""));
				item.setYear(result.optString("year", ""));
				
				String author = result.optString("author", "");
				
				item.setCreatorSummary(author);
				JSONArray array = new JSONArray();
				JSONObject member = new JSONObject();
				member.accumulate("creatorType", "author");
				member.accumulate("name", author);
				array.put(member);
				content.put("creators", array);
				
				item.setContent(content);
				item.save(db);
								
				msg = mHandler.obtainMessage();
				Bundle data = new Bundle();
				data.putString("itemKey", item.getKey());
				msg.setData(data);
				msg.arg2 = STATE_DONE;
	        	mHandler.sendMessage(msg);
				return;
			} catch (JSONException e) {
				Log.e(TAG, "exception parsing response", e);
				msg = mHandler.obtainMessage();
	        	msg.arg2 = STATE_ERROR;
	        	mHandler.sendMessage(msg);
	        	return;
			}
		}
		
		public void setState(int state) {
			mState = state;
		}
	}
}
