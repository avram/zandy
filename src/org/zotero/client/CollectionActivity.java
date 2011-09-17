package org.zotero.client;

import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;

import org.zotero.client.data.CollectionAdapter;
import org.zotero.client.data.ItemAdapter;
import org.zotero.client.data.ItemCollection;
import org.zotero.client.task.APIRequest;
import org.zotero.client.task.ZoteroAPITask;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

/* Rework for collections only, then make another one for items */
public class CollectionActivity extends ListActivity {
	private CommonsHttpOAuthConsumer httpOAuthConsumer;
	private OAuthProvider httpOAuthProvider;

	private static final String TAG = "org.zotero.client.CollectionActivity";
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.collections);

        CollectionAdapter collectionAdapter = CollectionAdapter.create(getBaseContext());

        String collectionKey = getIntent().getStringExtra("org.zotero.client.collectionKey");
        if (collectionKey != null) {
	        ItemCollection coll = ItemCollection.load(collectionKey);
	        collectionAdapter.refresh(coll);
        }
        
        setListAdapter(collectionAdapter);
        
        ListView lv = getListView();
        lv.setOnItemClickListener(new OnItemClickListener() {
        	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        		CollectionAdapter adapter = (CollectionAdapter) parent.getAdapter();
        		Cursor cur = adapter.getCursor();
        		// Place the cursor at the selected item
        		if (cur.moveToPosition(position)) {
        			// and replace the cursor with one for the selected collection
        			ItemCollection coll = ItemCollection.load(cur);
        			if (coll != null && coll.getKey() != null) {
        				Log.d(TAG, "Loading child collection with key: "+coll.getKey());
        				// We create and issue a specified intent with the necessary data
        		    	Intent i = new Intent(getBaseContext(), CollectionActivity.class);
        		    	i.putExtra("org.zotero.client.collectionKey", coll.getKey());
        		    	startActivity(i);
        				//adapter.refresh(ItemCollection.load(cur));
        			} else {
        				// collection loaded was null. why?
        				Log.d(TAG, "Failed loading collection at position: "+position);
        			}
        		} else {
        			// failed to move cursor-- show a toast
            		TextView tvTitle = (TextView)view.findViewById(R.id.collection_title);
            		Toast.makeText(getApplicationContext(), "Can't open "+tvTitle.getText(), 
            				Toast.LENGTH_SHORT).show();
        		}
          }
        });
        
        lv.setOnItemLongClickListener(new OnItemLongClickListener() {
        	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        		
        		CollectionAdapter adapter = (CollectionAdapter) parent.getAdapter();
        		Cursor cur = adapter.getCursor();
        		// Place the cursor at the selected item
        		if (cur.moveToPosition(position)) {
        			// and replace the cursor with one for the selected collection
        			ItemCollection coll = ItemCollection.load(cur);
        			if (coll != null && coll.getKey() != null) {
        				if (coll.getSize() == 0) {
            				Log.d(TAG, "Collection with key: "+coll.getKey()+ " is empty.");
                    		Toast.makeText(getApplicationContext(), "Collection is empty, requesting update.", 
                    				Toast.LENGTH_SHORT).show();
                			Log.d(TAG, "Running a request to populate missing data for collection");
                           	APIRequest req = new APIRequest(ServerCredentials.APIBASE + "/users/5770/collections/"+coll.getKey()+"/items", "get", "NZrpJ7YDnz8U6NPbbonerxlt");
                    		req.disposition = "xml";
                    		// TODO Introduce a callback to update UI when ready
                    		new ZoteroAPITask("NZrpJ7YDnz8U6NPbbonerxlt", (CursorAdapter) getListAdapter()).execute(req);
            				return true;
        				}
        				Log.d(TAG, "Loading items for collection with key: "+coll.getKey());
        				// We create and issue a specified intent with the necessary data
        		    	Intent i = new Intent(getBaseContext(), ItemActivity.class);
        		    	i.putExtra("org.zotero.client.collectionKey", coll.getKey());
        		    	startActivity(i);
        			} else {
        				// collection loaded was null. why?
        				Log.d(TAG, "Failed loading items for collection at position: "+position);
        				return true;
        			}
        		} else {
        			// failed to move cursor-- show a toast
            		TextView tvTitle = (TextView)view.findViewById(R.id.collection_title);
            		Toast.makeText(getApplicationContext(), "Can't open items for "+tvTitle.getText(), 
            				Toast.LENGTH_SHORT).show();
            		return true;
        		}
        		return true;
          }
        });
       
        /* override back
        lv.setOnKeyListener(new OnKeyListener() {
        	/**
        	 * We handle the back key in two places: when viewing collections,
        	 * and when viewing items that are in a collection.
        	 
			@Override
			public boolean onKey(View view, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
		    			
			            // we need to go back a level and swap out the cursor...
						Log.d(TAG,"ID: "+view.getId());
						Class current = getListAdapter().getClass();
						if (current == CollectionAdapter.class) {
				    		CollectionAdapter adapter = (CollectionAdapter) getListAdapter();
							Log.d(TAG, "BACK as collection in LV");
				    		adapter.goUp();
						} else {
							Log.d(TAG, "BACK as item in LV");
			    			ItemAdapter adapter2 = (ItemAdapter) getListAdapter();
			    			ItemCollection parent = adapter2.getParent();
			    			if (parent != null) {
			    				CollectionAdapter replacement = CollectionAdapter.create(getApplicationContext(), parent);
			    				setListAdapter(replacement);
			    			}
						}
			    		return true;
		        }
				return true;
			}
        });
        */
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.zotero_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.do_sync:
        	Log.d(TAG, "Making sync request");
        	APIRequest req = new APIRequest(ServerCredentials.APIBASE + "/users/5770/collections", "get", "NZrpJ7YDnz8U6NPbbonerxlt");
			req.disposition = "xml";
			new ZoteroAPITask("NZrpJ7YDnz8U6NPbbonerxlt", (CursorAdapter) getListAdapter()).execute(req);	
            return true;
        case R.id.quit:
        	finish();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
 /*   
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
    		if (getListAdapter() instanceof CollectionAdapter) {
				Log.d(TAG, "BACK as coll in ACTIVITY");

	            // we need to go back a level and swap out the cursor...
	    		CollectionAdapter adapter = (CollectionAdapter) getListAdapter();
	    		if (adapter.getCursor() == null) {
	    			adapter.goUp();
	    			adapter.justSwapped = true;
	    			return true;
	    		} else {
	    			return false;
	    		}
    		}
        }
        return super.onKeyDown(keyCode, event);
    }*/
}