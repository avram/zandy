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

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import com.getbase.floatingactionbutton.FloatingActionButton;
import com.mattrobertson.zotable.app.data.Database;
import com.mattrobertson.zotable.app.data.Item;
import com.mattrobertson.zotable.app.data.ItemCollection;

/**
 * This Activity handles displaying and editing collection memberships for a
 * given item.
 * 
 * @author ajlyon
 *
 */
public class CollectionMembershipActivity extends Activity {

	private static final String TAG = "CollMembershipActivity";
	
	private String itemKey;
	private String itemTitle;
	private Item item;

	ListView lvCollections;
	ArrayList<ItemCollection> rows;
	CollectionMembershipAdapter adapter;
	
	private Database db;

	/**
	 * For API <= 7, where we can't pass Bundles to dialogs
	 */
	private Bundle b = new Bundle();

	ArrayList<String> arrCollKeys,arrCollNames;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		setContentView(R.layout.coll_mem_activity);

		getActionBar().setDisplayHomeAsUpEnabled(true);

        db = new Database(this);
                
        /* Get the incoming data from the calling activity */
        itemKey = getIntent().getStringExtra("com.mattrobertson.zotable.app.itemKey");
        item = Item.load(itemKey, db);
        if (item == null) {
        	Log.e(TAG, "Null item for key: "+itemKey);
        	finish();
        }
        itemTitle = item.getTitle();
        
        this.setTitle(getResources().getString(R.string.collections_for_item, itemTitle));

		lvCollections = (ListView)findViewById(R.id.lvCollections);
        
        rows = ItemCollection.getCollections(item, db);

		adapter = new CollectionMembershipAdapter(rows);

        lvCollections.setAdapter(adapter);

		// Fill Collection arrays - used to add to collection. Pre-pop for better performance.
		new Thread(new Runnable() {
			public void run() {
				fillCollsArr();
			}
		}).start();

		FloatingActionButton fab = (FloatingActionButton)findViewById(R.id.btnFab);

		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {

				AlertDialog.Builder builder = new AlertDialog.Builder(CollectionMembershipActivity.this);
				builder.setTitle("Add to collection");

				ListView lvColls = new ListView(CollectionMembershipActivity.this);

				String[] templ = {};
				String[] stringArray = arrCollNames.toArray(templ);

				ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(CollectionMembershipActivity.this, android.R.layout.simple_list_item_1, android.R.id.text1, stringArray);
				lvColls.setAdapter(modeAdapter);

				builder.setView(lvColls);
				final Dialog dialog = builder.create();

				lvColls.setOnItemClickListener(new OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
						//Toast.makeText(getBaseContext(),"Clicked "+arrCollNames.get(position).trim()+" ("+arrCollKeys.get(position)+")",Toast.LENGTH_LONG).show();

						ItemCollection coll = ItemCollection.load(arrCollKeys.get(position), db);
						coll.add(item, false, db);
						coll.saveChildren(db);

						refreshData();

						dialog.dismiss();
					}
				});

				dialog.show();
			}
		});
    }
    
    @Override
    public void onDestroy() {
    	if (db != null) db.close();
    	super.onDestroy();
    }
    
    @Override
    public void onResume() {
    	if (db == null) db = new Database(this);
    	super.onResume();
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

	private void refreshData() {
		rows = ItemCollection.getCollections(item, db);

		adapter.setData(rows);

		adapter.notifyDataSetChanged();
		lvCollections.invalidate();
	}

	public void fillCollsArr() {
		arrCollKeys = new ArrayList<>();
		arrCollNames = new ArrayList<>();

		String[] args = { "false" };
		Cursor rootCursor = db.query("collections", Database.COLLCOLS, "collection_parent=?", args, null, null, "collection_name", null);

		if (rootCursor == null)
			return;

		rootCursor.moveToPrevious();

		while (rootCursor.moveToNext()) {
			String collName = rootCursor.getString(rootCursor.getColumnIndex("collection_name"));
			String collKey = rootCursor.getString(rootCursor.getColumnIndex("collection_key"));

			arrCollKeys.add(collKey);
			arrCollNames.add(collName);

			getChildren(collKey, 1);
		}

		rootCursor.close();
	}

	private void getChildren(String parentKey, int level) {
		String[] args = { parentKey };
		Cursor childCursor = db.query("collections", Database.COLLCOLS, "collection_parent=?", args, null, null, "collection_name", null);

		if (childCursor == null)
			return;

		childCursor.moveToPrevious();

		while (childCursor.moveToNext()) {
			String collName = childCursor.getString(childCursor.getColumnIndex("collection_name"));
			String collKey = childCursor.getString(childCursor.getColumnIndex("collection_key"));

			arrCollKeys.add(collKey);

			// Period in front of name designates a level of depth in hierarchy
			for (int i=0; i<level; i++)
				collName = "    " + collName;

			arrCollNames.add(collName);

			getChildren(collKey, level+1);
		}

		childCursor.close();
	}

	class CollectionMembershipAdapter extends BaseAdapter {
		private ArrayList<ItemCollection> mList;

		public CollectionMembershipAdapter(ArrayList<ItemCollection> list) {
			mList = list;
		}

		public void setData(ArrayList<ItemCollection> newColl) {
			mList = newColl;
		}

		@Override
		public int getCount() {
			return mList.size();
		}
		@Override
		public Object getItem(int pos) {
			return mList.get(pos);
		}
		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row;

			// We are reusing views, but we need to initialize it if null
			if (null == convertView) {
				LayoutInflater inflater = getLayoutInflater();
				row = inflater.inflate(R.layout.list_coll_mem, null);
			} else {
				row = convertView;
			}

			final String collKey = ((ItemCollection)getItem(position)).getKey();
			final String title = ((ItemCollection)getItem(position)).getTitle();

			TextView tvName = (TextView) row.findViewById(R.id.tvCollName);
			tvName.setText(title);

			ImageView imgRemove = (ImageView) row.findViewById(R.id.imgRemove);
			imgRemove.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					CollectionMembershipActivity.this.runOnUiThread(new Runnable() {
						public void run() {
							ItemCollection col = ItemCollection.load(collKey,db);
							col.remove(item,false,db);

							refreshData();
						}
					});
				}
			});

			return row;
		}
	}
}