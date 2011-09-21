package com.gimranov.zandy.client;

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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import com.gimranov.zandy.client.data.CollectionAdapter;
import com.gimranov.zandy.client.data.ItemCollection;
import com.gimranov.zandy.client.task.APIRequest;
import com.gimranov.zandy.client.task.ZoteroAPITask;

/* Rework for collections only, then make another one for items */
public class CollectionActivity extends ListActivity {

	private static final String TAG = "com.gimranov.zandy.client.CollectionActivity";
		
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.collections);

        CollectionAdapter collectionAdapter = CollectionAdapter.create(getBaseContext());

        String collectionKey = getIntent().getStringExtra("com.gimranov.zandy.client.collectionKey");
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
        			if (coll != null && coll.getKey() != null && coll.getSubcollections().size() > 0) {
        				Log.d(TAG, "Loading child collection with key: "+coll.getKey());
        				// We create and issue a specified intent with the necessary data
        		    	Intent i = new Intent(getBaseContext(), CollectionActivity.class);
        		    	i.putExtra("com.gimranov.zandy.client.collectionKey", coll.getKey());
        		    	startActivity(i);
        			} else {
        				Log.d(TAG, "Failed loading child collections for collection");
        				Toast.makeText(getApplicationContext(), "No subcollections for collection", 
                				Toast.LENGTH_SHORT).show();
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
                           	APIRequest req = new APIRequest(ServerCredentials.APIBASE
                           			+ ServerCredentials.prep(getBaseContext(), ServerCredentials.COLLECTIONS)
                           			+"/"+coll.getKey()+"/items", "get", null);
                    		req.disposition = "xml";
                    		// TODO Introduce a callback to update UI when ready
                    		new ZoteroAPITask(getBaseContext(), (CursorAdapter) getListAdapter()).execute(req);
            				return true;
        				}
        				Log.d(TAG, "Loading items for collection with key: "+coll.getKey());
        				// We create and issue a specified intent with the necessary data
        		    	Intent i = new Intent(getBaseContext(), ItemActivity.class);
        		    	i.putExtra("com.gimranov.zandy.client.collectionKey", coll.getKey());
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
    }
    
    protected void onResume() {
		CollectionAdapter adapter = (CollectionAdapter) getListAdapter();
		// XXX This may be too agressive-- fix if causes issues
		Cursor cur = adapter.getCursor();
		if (cur != null) cur.requery();
		adapter.notifyDataSetChanged();
    	super.onResume();
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
        	if (!ServerCredentials.check(getApplicationContext())) {
            	Toast.makeText(getApplicationContext(), "Log in to sync", 
        				Toast.LENGTH_SHORT).show();
            	return true;
        	}
        	Log.d(TAG, "Making sync request for all collections");
        	APIRequest req = new APIRequest(ServerCredentials.APIBASE 
        			+ ServerCredentials.prep(getBaseContext(), ServerCredentials.COLLECTIONS),
        			"get", null);
			req.disposition = "xml";
			new ZoteroAPITask(getBaseContext(), (CursorAdapter) getListAdapter()).execute(req);	
        	Toast.makeText(getApplicationContext(), "Started syncing; refreshing collection list", 
    				Toast.LENGTH_SHORT).show();
            return true;
        case R.id.do_new:
        	Log.d(TAG, "Can't yet make new collections");
        	Toast.makeText(getApplicationContext(), "Sorry, new collection creation is not yet possible. Soon!", 
    				Toast.LENGTH_SHORT).show();
            return true;
        case R.id.do_prefs:
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
}