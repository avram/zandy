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
import android.content.DialogInterface.OnClickListener;
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
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.gimranov.zandy.app.data.Creator;
import com.gimranov.zandy.app.data.Database;
import com.gimranov.zandy.app.data.Item;
import com.gimranov.zandy.app.task.APIRequest;
import com.gimranov.zandy.app.task.ZoteroAPITask;

/**
 * This Activity handles displaying and editing creators. It works almost the same as
 * ItemDataActivity and TagActivity, using a simple ArrayAdapter on Bundles with the creator info.
 * 
 * This currently operates by showing the creators for a given item; it could be
 * modified some day to show all creators in the database (when they come to be saved
 * that way).
 * 
 * @author ajlyon
 *
 */
public class CreatorActivity extends ListActivity {

	private static final String TAG = "com.gimranov.zandy.app.CreatorActivity";
	
	static final int DIALOG_CREATOR = 3;
	static final int DIALOG_CONFIRM_NAVIGATE = 4;	
	static final int DIALOG_CONFIRM_DELETE = 5;	
	
	public Item item;
	
	private Database db;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
                
        db = new Database(this);
        
        /* Get the incoming data from the calling activity */
        String itemKey = getIntent().getStringExtra("com.gimranov.zandy.app.itemKey");
        Item item = Item.load(itemKey, db);
        this.item = item;
        
        this.setTitle("Creators for "+item.getTitle());
        
        ArrayList<Bundle> rows = item.creatorsToBundleArray();
        
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
        		
        		tvLabel.setText(Item.localizedStringForString(
        				getItem(position).getString("creatorType")));
        		tvContent.setText(getItem(position).getString("name"));
         
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
        		
/* TODO Rework this logic to open an ItemActivity showing items with this creator
        		if (row.getString("label").equals("url")) {
        			row.putString("url", row.getString("content"));
        			removeDialog(DIALOG_CONFIRM_NAVIGATE);
        			showDialog(DIALOG_CONFIRM_NAVIGATE, row);
        			return;
        		}
        		
        		if (row.getString("label").equals("DOI")) {
        			String url = "http://dx.doi.org/"+Uri.encode(row.getString("content"));
        			row.putString("url", url);
        			removeDialog(DIALOG_CONFIRM_NAVIGATE);
        			showDialog(DIALOG_CONFIRM_NAVIGATE, row);
        			return;
        		}
 */       		
				Toast.makeText(getApplicationContext(), row.getString("name"), 
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
     			// If we have a long click on an entry, show an editor
        		ArrayAdapter<Bundle> adapter = (ArrayAdapter<Bundle>) parent.getAdapter();
        		Bundle row = adapter.getItem(position);
        		
    			removeDialog(DIALOG_CREATOR);
        		showDialog(DIALOG_CREATOR, row);
        		return true;
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
		final String creatorType = b.getString("creatorType");
		final int creatorPosition = b.getInt("position");
		
		String name = b.getString("name");
		String firstName = b.getString("firstName");
		String lastName = b.getString("lastName");
		
		switch (id) {
		/* Editor for a creator
		 */
		case DIALOG_CREATOR:
			AlertDialog.Builder builder;
			AlertDialog dialog;

			LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
			final View layout = inflater.inflate(R.layout.creator_dialog,
			                              (ViewGroup) findViewById(R.id.layout_root));

			TextView textName = (TextView) layout.findViewById(R.id.creator_name);
			textName.setText(name);
			TextView textFN = (TextView) layout.findViewById(R.id.creator_firstName);
			textFN.setText(firstName);
			TextView textLN = (TextView) layout.findViewById(R.id.creator_lastName);
			textLN.setText(lastName);

			CheckBox mode = (CheckBox) layout.findViewById(R.id.creator_mode);
			mode.setChecked((firstName == null || firstName.equals("")) 
							&& (lastName == null || lastName.equals(""))
							&& (lastName != null && !name.equals("")));
			
			// Set up the adapter to get creator types
			String[] types = Item.localizedCreatorTypesForItemType(item.getType());
						
			// what position are we?
			int arrPosition = 0;
			String localType = "";
			if (creatorType != null) {
				localType = Item.localizedStringForString(creatorType);
			} else {
				// We default to the first possibility when none specified
				localType = Item.localizedStringForString(
										Item.creatorTypesForItemType(item.getType())[0]);
			}
			for (int i = 0; i < types.length; i++) {
				if (types[i].equals(localType)) {
					arrPosition = i;
					break;
				}
			}

			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
							android.R.layout.simple_spinner_item, types);
		    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			
			Spinner spinner = (Spinner) layout.findViewById(R.id.creator_type);
			spinner.setAdapter(adapter);

			spinner.setSelection(arrPosition);
			builder = new AlertDialog.Builder(this);
			builder.setView(layout);
			builder.setPositiveButton(getResources().getString(R.string.ok), new OnClickListener(){
    	        @SuppressWarnings("unchecked")
				public void onClick(DialogInterface dialog, int whichButton) {
    	        	Creator c;
    				TextView textName = (TextView) layout.findViewById(R.id.creator_name);
    				TextView textFN = (TextView) layout.findViewById(R.id.creator_firstName);
    				TextView textLN = (TextView) layout.findViewById(R.id.creator_lastName);
    				Spinner spinner = (Spinner) layout.findViewById(R.id.creator_type);
    				CheckBox mode = (CheckBox) layout.findViewById(R.id.creator_mode);
    				
    				String selected = (String) spinner.getSelectedItem();
    				// Set up the adapter to get creator types
    				String[] types = Item.localizedCreatorTypesForItemType(item.getType());
    				
    				// what position are we?
    				int typePos = 0;
    				for (int i = 0; i < types.length; i++) {
    					if (types[i].equals(selected)) {
    						typePos = i;
    						break;
    					}
    				}
    				
    				String realType = Item.creatorTypesForItemType(item.getType())[typePos];

    				if (mode.isChecked())
    					c = new Creator(realType, textName.getText().toString(), true);
    				else
    					c = new Creator(realType, textFN.getText().toString(), textLN.getText().toString());
    	            
    	            Item.setCreator(item.getKey(), c, creatorPosition, db);
    	            item = Item.load(item.getKey(), db);
    	            ArrayAdapter<Bundle> la = (ArrayAdapter<Bundle>) getListAdapter();
    	            la.clear();
    	            for (Bundle b : item.creatorsToBundleArray()) {
    	            	la.add(b);
    	            }
    	            la.notifyDataSetChanged();
    	        }
			});
			
			builder.setNeutralButton(getResources().getString(R.string.cancel), new OnClickListener(){
				public void onClick(DialogInterface dialog, int whichButton) {
    	        	// do nothing
    	        }
			});

			builder.setNegativeButton(getResources().getString(R.string.menu_delete), new OnClickListener(){
				@SuppressWarnings("unchecked")
				public void onClick(DialogInterface dialog, int whichButton) {
    	            Item.setCreator(item.getKey(), null, creatorPosition, db);
    	            item = Item.load(item.getKey(), db);
    	            ArrayAdapter<Bundle> la = (ArrayAdapter<Bundle>) getListAdapter();
    	            la.clear();
    	            for (Bundle b : item.creatorsToBundleArray()) {
    	            	la.add(b);
    	            }
    	            la.notifyDataSetChanged();
    	        }
			});
			
			dialog = builder.create();
			return dialog;
			
		case DIALOG_CONFIRM_NAVIGATE:
/*			dialog = new AlertDialog.Builder(this)
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
			return dialog;*/
			return null;
		default:
			Log.e(TAG, "Invalid dialog requested");
			return null;
		}
	}
               
    /*
     * I've been just copying-and-pasting the options menu code from activity to activity.
     * It needs to be reworked for some of these activities.
     */
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
        	Log.d(TAG, "Preparing sync requests, starting with present item");
        	new ZoteroAPITask(getBaseContext()).execute(APIRequest.update(this.item));
        	Toast.makeText(getApplicationContext(), getResources().getString(R.string.sync_started), 
    				Toast.LENGTH_SHORT).show();
        	
        	return true;
        case R.id.do_new:
    		Bundle row = new Bundle();
    		row.putInt("position", -1);
    		row.putString("itemKey", this.item.getKey());
			removeDialog(DIALOG_CREATOR);
    		showDialog(DIALOG_CREATOR, row);
            return true;
        case R.id.do_prefs:
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
}
