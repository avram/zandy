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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.SearchManager;
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
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.gimranov.zandy.app.data.Database;
import com.gimranov.zandy.app.data.Item;
import com.gimranov.zandy.app.data.ItemAdapter;
import com.gimranov.zandy.app.data.ItemCollection;
import com.gimranov.zandy.app.task.APIRequest;
import com.gimranov.zandy.app.task.ZoteroAPITask;


public class ItemActivity extends ListActivity {

	private static final String TAG = "com.gimranov.zandy.app.ItemActivity";
	
	static final int DIALOG_VIEW = 0;
	static final int DIALOG_NEW = 1;
	static final int DIALOG_SORT = 2;
	
	static final String[] SORTS = {
		"item_year, item_title",
		"item_creator, item_year",
		"item_title, item_year",
		"timestamp ASC, item_title"
	};

	// XXX i8n
	static final String[] SORTS_EN = {
		"Year, then title",
		"Creator, then year",
		"Title, then year",
		"Date modified, then title"
	};	
	
	private String collectionKey;
	private String query;
	private Database db;
	
	public String sortBy = "item_year, item_title";
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        
        db = new Database(this);
		
        setContentView(R.layout.items);

        prepareAdapter();
        
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
        } else {        
	        collectionKey = intent.getStringExtra("com.gimranov.zandy.app.collectionKey");
	        if (collectionKey != null) {
	        	ItemCollection coll = ItemCollection.load(collectionKey, db);
	        	cursor = getCursor(coll);
	        	this.setTitle(coll.getTitle());
	        } else {
	        	cursor = getCursor();
	        	this.setTitle(getResources().getString(R.string.all_items));
	        }
        }
        return cursor;
    }
    
	protected Dialog onCreateDialog(int id, Bundle b) {
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
			AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
			builder2.setTitle(getResources().getString(R.string.set_sort_order))
					// XXX i18n
		    	    .setItems(SORTS_EN, new DialogInterface.OnClickListener() {
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
		default:
			return null;
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
        	// Make this a collection-specific sync, preceding by de-dirtying
        	// De-dirtying
        	Item.queue(db);
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
        	Toast.makeText(getApplicationContext(), getResources().getString(R.string.sync_started), 
    				Toast.LENGTH_SHORT).show();
            return true;
        case R.id.do_new:
        	removeDialog(DIALOG_NEW);
        	showDialog(DIALOG_NEW);
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
	
	public void setSortBy(String sort) {
		this.sortBy = sort;
	}
	
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

}