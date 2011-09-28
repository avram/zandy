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
package com.gimranov.zandy.client;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.TextView.BufferType;

import com.gimranov.zandy.client.data.Item;
import com.gimranov.zandy.client.task.APIRequest;
import com.gimranov.zandy.client.task.ZoteroAPITask;


public class ItemDataActivity extends ListActivity {

	private static final String TAG = "com.gimranov.zandy.client.ItemDataActivity";
	
	static final int DIALOG_SINGLE_VALUE = 0;
	static final int DIALOG_ITEM_TYPE = 1;
	static final int DIALOG_CONFIRM_NAVIGATE = 4;
	static final int DIALOG_CONFIRM_DELETE = 5;
	
	public Item item;
		
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
                
        /* Get the incoming data from the calling activity */
        String itemKey = getIntent().getStringExtra("com.gimranov.zandy.client.itemKey");
        item = Item.load(itemKey);
        
        // When an item in the view has been updated via a sync, the temporary key may have
        // been swapped out, so we fall back on the DB ID
        if (item == null) {
            String itemDbId = getIntent().getStringExtra("com.gimranov.zandy.client.itemDbId");
        	item = Item.loadDbId(itemDbId);
        }
        	
        // Set the activity title to the current item's title, if the title works
        if (item.getTitle() != null && !item.getTitle().equals(""))
        	this.setTitle(item.getTitle());
        else
        	this.setTitle("Item Data");
        
        ArrayList<Bundle> rows = item.toBundleArray();
        
        /* 
         * We use the standard ArrayAdapter, passing in our data as a Bundle.
         * Since it's no longer a simple TextView, we need to override getView, but
         * we can do that anonymously.
         */
        setListAdapter(new ArrayAdapter<Bundle>(this, R.layout.list_data, rows) {
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
        		TextView tvContent = (TextView) row.findViewById(R.id.data_content);
        		
	        	/* Since the field names are the API / internal form, we
	        	 * attempt to get a localized, human-readable version. */
        		tvLabel.setText(Item.localizedStringForString(
        					getItem(position).getString("label")));
        		
        		String content = getItem(position).getString("content");
        		
        		tvContent.setText(content);
         
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
     			// If we have a click on an entry, do something...
        		ArrayAdapter<Bundle> adapter = (ArrayAdapter<Bundle>) parent.getAdapter();
        		Bundle row = adapter.getItem(position);
        		if (row.getString("label").equals("url")) {
        			row.putString("url", row.getString("content"));
        			removeDialog(DIALOG_CONFIRM_NAVIGATE);
        			showDialog(DIALOG_CONFIRM_NAVIGATE, row);
        			return;
        		} else if (row.getString("label").equals("DOI")) {
        			String url = "http://dx.doi.org/"+Uri.encode(row.getString("content"));
        			row.putString("url", url);
        			removeDialog(DIALOG_CONFIRM_NAVIGATE);
        			showDialog(DIALOG_CONFIRM_NAVIGATE, row);
        			return;
        		}  else if (row.getString("label").equals("creators")) {
        	    	Log.d(TAG, "Trying to start creators activity");
        	    	Intent i = new Intent(getBaseContext(), CreatorActivity.class);
    		    	i.putExtra("com.gimranov.zandy.client.itemKey", item.getKey());
        	    	startActivity(i);
        	    	return;
        		} else if (row.getString("label").equals("tags")) {
        	    	Log.d(TAG, "Trying to start tag activity");
        	    	Intent i = new Intent(getBaseContext(), TagActivity.class);
        	    	i.putExtra("com.gimranov.zandy.client.itemKey", item.getKey());
        	    	startActivity(i);
        			return;
        		}
        		
				Toast.makeText(getApplicationContext(), row.getString("content"), 
        				Toast.LENGTH_SHORT).show();
        	}
        });
        
        /*
         * On long click, we bring up an edit dialog.
         */
        lv.setOnItemLongClickListener(new OnItemLongClickListener() {
        	/*
        	 * Same annotation as in onItemClick(..), above.
        	 */
        	@SuppressWarnings("unchecked")
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
     			// If we have a long click on an entry, we'll provide a way of editing it
        		ArrayAdapter<Bundle> adapter = (ArrayAdapter<Bundle>) parent.getAdapter();
        		Bundle row = adapter.getItem(position);
        		// Show the right type of dialog for the row in question
        		if (row.getString("label").equals("itemType")) {
        			// XXX 
                	Toast.makeText(getApplicationContext(), "Item type cannot be changed.", 
            				Toast.LENGTH_SHORT).show();
        			//removeDialog(DIALOG_ITEM_TYPE);
        			//showDialog(DIALOG_ITEM_TYPE, row);
        			return true;
        		} else if (row.getString("label").equals("creators")) {
        	    	Log.d(TAG, "Trying to start creators activity");
        	    	Intent i = new Intent(getBaseContext(), CreatorActivity.class);
    		    	i.putExtra("com.gimranov.zandy.client.itemKey", item.getKey());
        	    	startActivity(i);
        	    	return true;
        		} else if (row.getString("label").equals("tags")) {
        	    	Log.d(TAG, "Trying to start tag activity");
        	    	Intent i = new Intent(getBaseContext(), TagActivity.class);
        	    	i.putExtra("com.gimranov.zandy.client.itemKey", item.getKey());
        	    	startActivity(i);
        			return true;
        		}
    			removeDialog(DIALOG_SINGLE_VALUE);
        		showDialog(DIALOG_SINGLE_VALUE, row);
        		return true;
          }
        });

    }
    
	protected Dialog onCreateDialog(int id, Bundle b) {
		final String label = b.getString("label");
		final String itemKey = b.getString("itemKey");
		final String content = b.getString("content");
		AlertDialog dialog;
		
		switch (id) {
		/* Simple editor for a single value */
		case DIALOG_SINGLE_VALUE:			
			final EditText input = new EditText(this);
			input.setText(content, BufferType.EDITABLE);
			
			dialog = new AlertDialog.Builder(this)
	    	    .setTitle("Edit " + Item.localizedStringForString(label))
	    	    .setView(input)
	    	    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
	    	        @SuppressWarnings("unchecked")
					public void onClick(DialogInterface dialog, int whichButton) {
	    	            Editable value = input.getText();
	    	            Item.set(itemKey, label, value.toString());
	    	            Item item = Item.load(itemKey);
	    	            ArrayAdapter<Bundle> la = (ArrayAdapter<Bundle>) getListAdapter();
	    	            la.clear();
	    	            for (Bundle b : item.toBundleArray()) {
	    	            	la.add(b);
	    	            }
	    	            la.notifyDataSetChanged();
	    	        }
	    	    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	    	        public void onClick(DialogInterface dialog, int whichButton) {
	    	        	// do nothing
	    	        }
	    	    }).create();
			return dialog;
		/* Item type selector */
		case DIALOG_ITEM_TYPE:
			dialog = new AlertDialog.Builder(this)
	    	    .setTitle("Change Item Type")
	    	    .setItems(Item.ITEM_TYPES_EN, new DialogInterface.OnClickListener() {
	    	        @SuppressWarnings("unchecked")
					public void onClick(DialogInterface dialog, int pos) {
	    	            Item.set(itemKey, label, Item.ITEM_TYPES[pos]);
	    	            Item item = Item.load(itemKey);
	    	            ArrayAdapter<Bundle> la = (ArrayAdapter<Bundle>) getListAdapter();
	    	            la.clear();
	    	            for (Bundle b : item.toBundleArray()) {
	    	            	la.add(b);
	    	            }
	    	            la.notifyDataSetChanged();
	    	        }
	    	    }).create();
			return dialog;
		case DIALOG_CONFIRM_NAVIGATE:
			dialog = new AlertDialog.Builder(this)
		    	    .setTitle("View this online?")
		    	    .setPositiveButton("View", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
		        			// The behavior for invalid URIs might be nasty, but
		        			// we'll cross that bridge if we come to it.
		        			Uri uri = Uri.parse(content);
		        			startActivity(new Intent(Intent.ACTION_VIEW)
		        							.setData(uri));
		    	        }
		    	    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		    	        public void onClick(DialogInterface dialog, int whichButton) {
		    	        	// do nothing
		    	        }
		    	    }).create();
			return dialog;
		case DIALOG_CONFIRM_DELETE:
			dialog = new AlertDialog.Builder(this)
		    	    .setTitle("Delete this item")
		    	    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							Item i = Item.load(itemKey);
							i.delete();
		    	        }
		    	    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		    	        public void onClick(DialogInterface dialog, int whichButton) {
		    	        	// do nothing
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
        // Remove new item-- should be created from context of an item list
        menu.removeItem(R.id.do_new);
        
        // Turn on delete item
        MenuItem del = menu.findItem(R.id.do_delete);
        del.setEnabled(true);
        del.setVisible(true);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem i) {
        // Handle item selection
        switch (i.getItemId()) {
        case R.id.do_sync:
        	if (!ServerCredentials.check(getApplicationContext())) {
            	Toast.makeText(getApplicationContext(), "Log in to sync", 
        				Toast.LENGTH_SHORT).show();
            	return true;
        	}
        	Log.d(TAG, "Preparing sync requests, starting with present item");
        	new ZoteroAPITask(getBaseContext()).execute(APIRequest.update(item));
        	Toast.makeText(getApplicationContext(), "Started syncing...", 
    				Toast.LENGTH_SHORT).show();
        	return true;
        case R.id.do_prefs:
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        case R.id.do_delete:
        	Bundle b = new Bundle();
        	b.putString("itemKey",item.getKey());
			removeDialog(DIALOG_CONFIRM_DELETE);
    		showDialog(DIALOG_CONFIRM_DELETE, b);
        	return true;
        default:
            return super.onOptionsItemSelected(i);
        }
    }
}
