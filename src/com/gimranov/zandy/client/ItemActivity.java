package com.gimranov.zandy.client;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
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

import com.gimranov.zandy.client.data.Item;
import com.gimranov.zandy.client.data.ItemAdapter;
import com.gimranov.zandy.client.data.ItemCollection;
import com.gimranov.zandy.client.task.APIRequest;
import com.gimranov.zandy.client.task.ZoteroAPITask;


public class ItemActivity extends ListActivity {

	private static final String TAG = "com.gimranov.zandy.client.ItemActivity";
	
	static final int DIALOG_VIEW = 0;
	static final int DIALOG_NEW = 1;
	
	private String collectionKey;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        ItemAdapter itemAdapter;
        
        setContentView(R.layout.items);
        
        collectionKey = getIntent().getStringExtra("com.gimranov.zandy.client.collectionKey");
        // TODO Figure out how we'll address other views that aren't collections
        if (collectionKey != null) {
        	ItemCollection coll = ItemCollection.load(collectionKey);
        	itemAdapter = ItemAdapter.create(getBaseContext(), coll);
        	this.setTitle(coll.getTitle());
        } else {
        	itemAdapter = ItemAdapter.create(getBaseContext());
        	// XXX i18n
        	this.setTitle("All items");
        }
        
        setListAdapter(itemAdapter);
        
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
    		    	i.putExtra("com.gimranov.zandy.client.itemKey", item.getKey());
    		    	startActivity(i);
        		} else {
        			// failed to move cursor-- show a toast
            		TextView tvTitle = (TextView)view.findViewById(R.id.item_title);
            		// XXX i18n
            		Toast.makeText(getApplicationContext(), "Can't open "+tvTitle.getText(), 
            				Toast.LENGTH_SHORT).show();
        		}
        	}
        });
    }
    
    protected void onResume() {
		ItemAdapter adapter = (ItemAdapter) getListAdapter();
		// XXX This may be too agressive-- fix if causes issues
		Cursor cur = adapter.getCursor();
		if (cur != null) cur.requery();
		adapter.notifyDataSetChanged();
    	super.onResume();
    }
    
	protected Dialog onCreateDialog(int id, Bundle b) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		// XXX i18n
		builder.setTitle("Item Type")
	    	    .setItems(Item.ITEM_TYPES_EN, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int pos) {
	    	            Item item = new Item(getBaseContext(), Item.ITEM_TYPES[pos]);
	    	            item.dirty = APIRequest.API_DIRTY;
	    	            item.save();
	    	            if (collectionKey != null) {
	    	            	ItemCollection coll = ItemCollection.load(collectionKey);
	    	            	if (coll != null) {
	    	            		coll.loadChildren();
	    	            		coll.add(item);
	    	            		coll.saveChildren();
	    	            	}
	    	            }
	        			Log.d(TAG, "Loading item data with key: "+item.getKey());
	    				// We create and issue a specified intent with the necessary data
	    		    	Intent i = new Intent(getBaseContext(), ItemDataActivity.class);
	    		    	i.putExtra("com.gimranov.zandy.client.itemKey", item.getKey());
	    		    	startActivity(i);
	    	        }
	    	    });
		AlertDialog dialog = builder.create();
		return dialog;
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
        	if (!ServerCredentials.check(getBaseContext())) {
        		// XXX i18n
            	Toast.makeText(getBaseContext(), "Log in to sync", 
        				Toast.LENGTH_SHORT).show();
            	return true;
        	}
        	// Make this a collection-specific sync, preceding by de-dirtying
        	// De-dirtying
        	Item.queue();
        	APIRequest[] reqs = new APIRequest[Item.queue.size() + 1];
        	for (int j = 0; j < Item.queue.size(); j++) {
        		Log.d(TAG, "Adding dirty item to sync: "+Item.queue.get(j).getTitle());
        		reqs[j] = ServerCredentials.prep(getBaseContext(), APIRequest.update(Item.queue.get(j)));
        	}
        	if (collectionKey == null) {
            	Log.d(TAG, "Adding sync request for all items");
            	APIRequest req = new APIRequest(ServerCredentials.APIBASE 
            			+ ServerCredentials.prep(getBaseContext(), ServerCredentials.ITEMS +"/top"),
            			"get", null);
    			req.disposition = "xml";
    			reqs[Item.queue.size()] = req;
        	} else {
            	Log.d(TAG, "Adding sync request for collection: " + collectionKey);
            	APIRequest req = new APIRequest(ServerCredentials.APIBASE
							+ ServerCredentials.prep(getBaseContext(), ServerCredentials.COLLECTIONS)
							+"/"
							+ collectionKey + "/items",
						"get",
						null);
    			req.disposition = "xml";
    			reqs[Item.queue.size()] = req;
        	}
        	// This then provides a full queue, with the locally dirty items first, followed
        	// by a scoped sync. Cool!
			new ZoteroAPITask(getBaseContext(), (CursorAdapter) getListAdapter()).execute(reqs);
			// XXX i18n
        	Toast.makeText(getApplicationContext(), "Started syncing...", 
    				Toast.LENGTH_SHORT).show();
            return true;
        case R.id.do_new:
        	removeDialog(DIALOG_NEW);
        	showDialog(DIALOG_NEW);
            return true;
        case R.id.do_prefs:
	    	Intent i = new Intent(getBaseContext(), SettingsActivity.class);
	    	Log.d(TAG, "Intent for class:  "+i.getClass().toString());
	    	startActivity(i);
	    	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
}