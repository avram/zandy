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

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.gimranov.zandy.app.data.Database;
import com.gimranov.zandy.app.data.Item;
import com.gimranov.zandy.app.data.ItemCollection;
import com.gimranov.zandy.app.task.APIRequest;
import com.gimranov.zandy.app.task.ZoteroAPITask;

/**
 * This Activity handles displaying and editing collection memberships for a
 * given item.
 * 
 * @author ajlyon
 *
 */
public class CollectionMembershipActivity extends ListActivity {

	private static final String TAG = "com.gimranov.zandy.app.CollectionMembershipActivity";
	
	static final int DIALOG_CONFIRM_NAVIGATE = 4;
	static final int DIALOG_COLLECTION_LIST = 1;
	
	private Item item;
	
	private Database db;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        db = new Database(this);
                
        /* Get the incoming data from the calling activity */
        String itemKey = getIntent().getStringExtra("com.gimranov.zandy.app.itemKey");
        final Item item = Item.load(itemKey, db);
        this.item = item;
        
        this.setTitle(getResources().getString(R.string.collections_for_item, item.getTitle()));
        
        ArrayList<ItemCollection> rows = ItemCollection.getCollections(item, db);
        
        setListAdapter(new ArrayAdapter<ItemCollection>(this, R.layout.list_data, rows) {
        	@Override
        	public View getView(int position, View convertView, ViewGroup parent) {
        		View row;
        		
                // We are reusing views, but we need to initialize it if null
        		if (null == convertView) {
                    LayoutInflater inflater = getLayoutInflater();
        			row = inflater.inflate(R.layout.list_data, null);
        		} else {
        			row = convertView;
        		}
         
        		/* Our layout has just two fields */
        		TextView tvLabel = (TextView) row.findViewById(R.id.data_label);
        		tvLabel.setText("");
        		TextView tvContent = (TextView) row.findViewById(R.id.data_content);

        		tvContent.setText(getItem(position).getTitle());
         
        		return row;
        	}
        });
        
        ListView lv = getListView();
        lv.setTextFilterEnabled(true);
        lv.setOnItemClickListener(new OnItemClickListener() {
        	// Warning here because Eclipse can't tell whether my ArrayAdapter is
        	// being used with the correct parametrization.
        	@SuppressWarnings("unchecked")
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        		// If we have a click on an entry, prompt to view that tag's items.
        		ArrayAdapter<ItemCollection> adapter = (ArrayAdapter<ItemCollection>) parent.getAdapter();
        		ItemCollection row = adapter.getItem(position);
        		Bundle b = new Bundle();
        		b.putString("itemKey", item.getKey());
        		b.putString("collectionKey", row.getKey());
      			removeDialog(DIALOG_CONFIRM_NAVIGATE);
       			showDialog(DIALOG_CONFIRM_NAVIGATE, b);
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
    
	protected Dialog onCreateDialog(int id, Bundle b) {
		final String collectionKey = b.getString("collectionKey");
		final String itemKey = b.getString("itemKey");
		AlertDialog dialog;
		
		switch (id) {
		case DIALOG_COLLECTION_LIST:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			final ArrayList<ItemCollection> collections = ItemCollection.getCollections(db);
			int size = collections.size();
			String[] collectionNames = new String[size];
			for (int i = 0; i < size; i++) {
				collectionNames[i] = collections.get(i).getTitle();
			}
			builder.setTitle(getResources().getString(R.string.choose_parent_collection))
		    	    .setItems(collectionNames, new DialogInterface.OnClickListener() {
						@SuppressWarnings("unchecked")
						public void onClick(DialogInterface dialog, int pos) {
		    	            Item item = Item.load(itemKey, db);
							collections.get(pos).add(item);
							collections.get(pos).saveChildren(db);
		    	        	// XXX temporary, no i18n
		    	        	Toast.makeText(getApplicationContext(), "Sync soon to make this stick.", 
		    	    				Toast.LENGTH_SHORT).show();
							ArrayAdapter<ItemCollection> la = (ArrayAdapter<ItemCollection>) getListAdapter();
		    	            la.clear();
		    	            for (ItemCollection b : ItemCollection.getCollections(item,db)) {
		    	            	la.add(b);
		    	            }
		    	            la.notifyDataSetChanged();
		    	        }
		    	    });
			dialog = builder.create();
			return dialog;
		case DIALOG_CONFIRM_NAVIGATE:
			dialog = new AlertDialog.Builder(this)
		    	    .setTitle(getResources().getString(R.string.collection_membership_detail))
		    	    .setPositiveButton(getResources().getString(R.string.tag_view), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							Intent i = new Intent(getBaseContext(), ItemActivity.class);
		    		    	i.putExtra("com.gimranov.zandy.app.collectionKey", collectionKey);
		        	    	startActivity(i);
		    	        }
		    	    }).setNeutralButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
		    	        public void onClick(DialogInterface dialog, int whichButton) {
		    	        	// do nothing
		    	        }
		    	    }).setNegativeButton(getResources().getString(R.string.collection_remove_item), new DialogInterface.OnClickListener() {
	    	        	@SuppressWarnings("unchecked")
		    	    	public void onClick(DialogInterface dialog, int whichButton) {
		    	        	Item item = Item.load(itemKey, db);
		    	        	ItemCollection coll = ItemCollection.load(collectionKey, db);
		    	        	coll.remove(item, db);
		    	        	// XXX temporary, no i18n
		    	        	Toast.makeText(getApplicationContext(), "Sync soon to make this stick.", 
		    	    				Toast.LENGTH_SHORT).show();
							ArrayAdapter<ItemCollection> la = (ArrayAdapter<ItemCollection>) getListAdapter();
		    	            la.clear();
		    	            for (ItemCollection b : ItemCollection.getCollections(item,db)) {
		    	            	la.add(b);
		    	            }
		    	            la.notifyDataSetChanged();
		    	        }
		    	    }).create();
			return dialog;
		default:
			Log.e(TAG, "Invalid dialog requested");
			return null;
		}
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
            	Toast.makeText(getApplicationContext(), getResources().getString(R.string.sync_log_in_first), 
        				Toast.LENGTH_SHORT).show();
            	return true;
        	}
        	Log.d(TAG, "Preparing sync requests");
        	new ZoteroAPITask(getBaseContext()).execute(APIRequest.update(this.item));
        	Toast.makeText(getApplicationContext(), getResources().getString(R.string.sync_started), 
    				Toast.LENGTH_SHORT).show();
        	return true;
        case R.id.do_new:
    		Bundle b = new Bundle();
    		b.putString("itemKey", this.item.getKey());
    		removeDialog(DIALOG_COLLECTION_LIST);
    		showDialog(DIALOG_COLLECTION_LIST, b);
            return true;
        case R.id.do_prefs:
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
}
