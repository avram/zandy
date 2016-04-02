/*******************************************************************************
 * This file is part of Zotable.
 * 
 * Zotable is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Zotable is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with Zotable.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.mattrobertson.zotable.app;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.mattrobertson.zotable.app.data.CollectionAdapter;
import com.mattrobertson.zotable.app.data.Database;
import com.mattrobertson.zotable.app.data.ItemCollection;
import com.mattrobertson.zotable.app.task.APIRequest;

public class CollectionActivity extends Activity {

	private static final String TAG = "CollectionActivity";
	private ItemCollection collection;
	private Database db;

    ListView lvCollections;

	final Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			Log.d(TAG,"received message: "+msg.arg1);
			refreshView();
			
			if (msg.arg1 == APIRequest.UPDATED_DATA) {
				//refreshView();
				return;
			}
			
			if (msg.arg1 == APIRequest.QUEUED_MORE) {
				Toast.makeText(getApplicationContext(),
						getResources().getString(R.string.sync_queued_more, msg.arg2),
        				Toast.LENGTH_SHORT).show();
				return;
			}

			if (msg.arg1 == APIRequest.BATCH_DONE) {
                Application.getInstance().getBus().post(SyncEvent.COMPLETE);
				//Toast.makeText(getApplicationContext(),getResources().getString(R.string.sync_complete),Toast.LENGTH_SHORT).show();
				return;
			}
			
			if (msg.arg1 == APIRequest.ERROR_UNKNOWN) {
				String desc = (msg.arg2 == 0) ? "" : " ("+msg.arg2+")";
				Toast.makeText(getApplicationContext(),
						getResources().getString(R.string.sync_error)+desc, 
        				Toast.LENGTH_SHORT).show();
				return;
			}
		}
	};
	
	/**
	 * Refreshes the current list adapter
	 */
	private void refreshView() {
		CollectionAdapter adapter = (CollectionAdapter) (lvCollections.getAdapter());
		Cursor newCursor = (collection == null) ? create() : create(collection);
		adapter.changeCursor(newCursor);
		adapter.notifyDataSetChanged();
		Log.d(TAG, "refreshing view on request");
	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		getActionBar().setDisplayHomeAsUpEnabled(true);

        db = new Database(this);

		setContentView(R.layout.collections);

        lvCollections = (ListView)findViewById(R.id.lvCollections);

        CollectionAdapter collectionAdapter;
        
        String collectionKey = getIntent().getStringExtra("com.mattrobertson.zotable.app.collectionKey");
        if (collectionKey != null) {
	        ItemCollection coll = ItemCollection.load(collectionKey, db);
	        // We set the title to the current collection
	        this.collection = coll;
	        this.setTitle(coll.getTitle());
	        collectionAdapter = new CollectionAdapter(this, create(coll));
        } else {
        	this.setTitle(getResources().getString(R.string.collections));
	        collectionAdapter = new CollectionAdapter(this, create());
        }
        
        lvCollections.setAdapter(collectionAdapter);

        lvCollections.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                CollectionAdapter adapter = (CollectionAdapter) parent.getAdapter();
                Cursor cur = adapter.getCursor();
                // Place the cursor at the selected item
                if (cur.moveToPosition(position)) {
                    // and replace the cursor with one for the selected collection
                    ItemCollection coll = ItemCollection.load(cur);
                    if (coll != null && coll.getKey() != null) {
                        Intent i = new Intent(getBaseContext(), ItemActivity.class);
                        if (coll.getSize() == 0) {
                            // Send a message that we need to refresh the collection
                            i.putExtra("com.mattrobertson.zotable.app.rerequest", true);
                        }
                        i.putExtra("com.mattrobertson.zotable.app.collectionKey", coll.getKey());
                        startActivity(i);
                    } else {
                        // collection loaded was null. why?
                        Log.d(TAG, "Failed loading items for collection at position: " + position);
                        return;
                    }
                } else {
                    // failed to move cursor-- show a toast
                    TextView tvTitle = (TextView) view.findViewById(R.id.collection_title);
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.collection_cant_open, tvTitle.getText()),
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                return;
            }
        });
    }
    
    protected void onResume() {
		CollectionAdapter adapter = (CollectionAdapter)(lvCollections.getAdapter());
		// XXX This may be too agressive-- fix if causes issues
		Cursor newCursor = (collection == null) ? create() : create(collection);
		adapter.changeCursor(newCursor);
		adapter.notifyDataSetChanged();
		if (db == null) db = new Database(this);
    	super.onResume();
    }
    
    public void onDestroy() {
        CollectionAdapter adapter = (CollectionAdapter)(lvCollections.getAdapter());
		Cursor cur = adapter.getCursor();
		if(cur != null) cur.close();
		if (db != null) db.close();
		super.onDestroy();
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
		case android.R.id.home:
			onBackPressed();
			return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
	/**
	 * Gives a cursor for top-level collections
	 * @return
	 */
	public Cursor create() {
		String[] args = { "false" };
		Cursor cursor = db.query("collections", Database.COLLCOLS, "collection_parent=?", args, null, null, "collection_name", null);
		if (cursor == null) {
			Log.e(TAG, "cursor is null");
		}

		return cursor;
	}

	/**
	 * Gives a cursor for child collections of a given parent
	 * @param parent
	 * @return
	 */
	public Cursor create(ItemCollection parent) {
		String[] args = { parent.getKey() };
		Cursor cursor = db.query("collections", Database.COLLCOLS, "collection_parent=?", args, null, null, "collection_name", null);
		if (cursor == null) {
			Log.e(TAG, "cursor is null");
		}

		return cursor;
	}
    
}
