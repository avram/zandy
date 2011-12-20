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
import android.app.ExpandableListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import android.widget.Toast;

import com.gimranov.zandy.app.data.Database;
import com.gimranov.zandy.app.data.Item;
import com.gimranov.zandy.app.task.APIRequest;
import com.gimranov.zandy.app.task.ZoteroAPITask;


public class ItemDataActivity extends ExpandableListActivity {

	private static final String TAG = "com.gimranov.zandy.app.ItemDataActivity";
	
	static final int DIALOG_SINGLE_VALUE = 0;
	static final int DIALOG_ITEM_TYPE = 1;
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
        item = Item.load(itemKey, db);
        
        // When an item in the view has been updated via a sync, the temporary key may have
        // been swapped out, so we fall back on the DB ID
        if (item == null) {
            String itemDbId = getIntent().getStringExtra("com.gimranov.zandy.app.itemDbId");
            if (itemDbId == null) {
            	Log.d(TAG, "Failed to load item using itemKey and no dbId specified. Give up and finish activity.");
            	finish();
            	return;
            }
        	item = Item.loadDbId(itemDbId, db);
        }
        	
        // Set the activity title to the current item's title, if the title works
        if (item.getTitle() != null && !item.getTitle().equals(""))
        	this.setTitle(item.getTitle());
        else
        	this.setTitle(getResources().getString(R.string.item_details));
        
        ArrayList<Bundle> rows = item.toBundleArray(db);

        BundleListAdapter mBundleListAdapter = new BundleListAdapter();
        mBundleListAdapter.bundles = rows;
        setListAdapter(mBundleListAdapter);
        registerForContextMenu(getExpandableListView());
        
        ExpandableListView lv = getExpandableListView();
        lv.setTextFilterEnabled(true);
        lv.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
			@Override
			public boolean onChildClick(ExpandableListView parent, View v,
					int groupPosition, int childPosition, long id) {
				Log.d(TAG, "child click");
				return false;
			}        	
        });
        lv.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
        	// Warning here because Eclipse can't tell whether my ArrayAdapter is
        	// being used with the correct parametrization.
			public boolean onGroupClick(ExpandableListView parent, View view, int position, long id) {
     			Log.d(TAG, "group clicked");
				// If we have a click on an entry, do something...
        		BundleListAdapter adapter = (BundleListAdapter) parent.getExpandableListAdapter();
        		Bundle row = adapter.getGroup(position);
        		if (row.getString("label").equals("url")) {
        			row.putString("url", row.getString("content"));
        			removeDialog(DIALOG_CONFIRM_NAVIGATE);
        			showDialog(DIALOG_CONFIRM_NAVIGATE, row);
        			return true;
        		} else if (row.getString("label").equals("DOI")) {
        			String url = "http://dx.doi.org/"+Uri.encode(row.getString("content"));
        			row.putString("url", url);
        			removeDialog(DIALOG_CONFIRM_NAVIGATE);
        			showDialog(DIALOG_CONFIRM_NAVIGATE, row);
        			return true;
        		}  else if (row.getString("label").equals("creators")) {
        	    	Log.d(TAG, "Trying to start creators activity");
        	    	Intent i = new Intent(getBaseContext(), CreatorActivity.class);
    		    	i.putExtra("com.gimranov.zandy.app.itemKey", item.getKey());
        	    	startActivity(i);
        	    	return true;
        		} else if (row.getString("label").equals("tags")) {
        	    	Log.d(TAG, "Trying to start tag activity");
        	    	Intent i = new Intent(getBaseContext(), TagActivity.class);
        	    	i.putExtra("com.gimranov.zandy.app.itemKey", item.getKey());
        	    	startActivity(i);
        			return true;
	    		} else if (row.getString("label").equals("children")) {
	    	    	Log.d(TAG, "Trying to start attachment activity");
	    	    	Intent i = new Intent(getBaseContext(), AttachmentActivity.class);
	    	    	i.putExtra("com.gimranov.zandy.app.itemKey", item.getKey());
	    	    	startActivity(i);
	    			return true;
	    		} else if (row.getString("label").equals("collections")) {
	    	    	Log.d(TAG, "Trying to start collection membership activity");
	    	    	Intent i = new Intent(getBaseContext(), CollectionMembershipActivity.class);
	    	    	i.putExtra("com.gimranov.zandy.app.itemKey", item.getKey());
	    	    	startActivity(i);
	    			return true;
	    		}
				Toast.makeText(getApplicationContext(), row.getString("content"), 
        				Toast.LENGTH_SHORT).show();
        		return false;
        	}
        });
        
        /*
         * On long click, we bring up an edit dialog.
         */
        lv.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
        	@Override
			public void onCreateContextMenu(ContextMenu menu, View view,
					ContextMenuInfo menuInfo) {
     			Log.d(TAG, "contxt menu");
        		ExpandableListView.ExpandableListContextMenuInfo info =
        				(ExpandableListView.ExpandableListContextMenuInfo) menuInfo;
        		int type = ExpandableListView.getPackedPositionType(info.packedPosition);
        		int group =	ExpandableListView.getPackedPositionGroup(info.packedPosition);
        		int child = ExpandableListView.getPackedPositionChild(info.packedPosition);
        		
        		if (type == 0) {
        			
        			// If we have a long click on an entry, we'll provide a way of editing it
            		BundleListAdapter adapter = (BundleListAdapter) ((ExpandableListView) info.targetView.getParent()).getExpandableListAdapter();;
            		Bundle row = adapter.getGroup(group);
            		// Show the right type of dialog for the row in question
            		if (row.getString("label").equals("itemType")) {
            			// XXX don't need i18n, since this should be overcome
                    	Toast.makeText(getApplicationContext(), "Item type cannot be changed.", 
                				Toast.LENGTH_SHORT).show();
            			//removeDialog(DIALOG_ITEM_TYPE);
            			//showDialog(DIALOG_ITEM_TYPE, row);
            			return;
            		} else if (row.getString("label").equals("children")) {
            	    	Log.d(TAG, "Not starting children activity on click-and-hold");
            	    	return;
            		} else if (row.getString("label").equals("creators")) {
            	    	Log.d(TAG, "Trying to start creators activity");
            	    	Intent i = new Intent(getBaseContext(), CreatorActivity.class);
        		    	i.putExtra("com.gimranov.zandy.app.itemKey", item.getKey());
            	    	startActivity(i);
            	    	return;
            		} else if (row.getString("label").equals("tags")) {
            	    	Log.d(TAG, "Trying to start tag activity");
            	    	Intent i = new Intent(getBaseContext(), TagActivity.class);
            	    	i.putExtra("com.gimranov.zandy.app.itemKey", item.getKey());
            	    	startActivity(i);
            			return;
            		}
        			removeDialog(DIALOG_SINGLE_VALUE);
            		showDialog(DIALOG_SINGLE_VALUE, row);
            		return;
        		}
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
	    	    .setTitle(getResources().getString(R.string.edit_item_field, Item.localizedStringForString(label)))
	    	    .setView(input)
	    	    .setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
	    	            Editable value = input.getText();
	    	            String fixed;
	    	            if ("note".equals(label)) {
		    	            fixed = value.toString().replaceAll("\n\n", "\n<br>");
	    	            } else {
	    	            	fixed = value.toString();
	    	            }
	    	            Item.set(itemKey, label, fixed, db);
	    	            Item item = Item.load(itemKey, db);
	    	            BundleListAdapter la = (BundleListAdapter) getExpandableListAdapter();
	    	            la.bundles = item.toBundleArray(db);
	    	            la.notifyDataSetChanged();
	    	        }
	    	    }).setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
	    	        public void onClick(DialogInterface dialog, int whichButton) {
	    	        	// do nothing
	    	        }
	    	    }).create();
			return dialog;
		/* Item type selector */
		case DIALOG_ITEM_TYPE:
			dialog = new AlertDialog.Builder(this)
	    	    .setTitle(getResources().getString(R.string.item_type_change))
	    	    // XXX i18n
	    	    .setItems(Item.ITEM_TYPES_EN, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int pos) {
	    	            Item.set(itemKey, label, Item.ITEM_TYPES[pos], db);
	    	            Item item = Item.load(itemKey, db);
	    	            BundleListAdapter la = (BundleListAdapter) getExpandableListAdapter();
	    	            la.bundles = item.toBundleArray(db);
	    	            la.notifyDataSetChanged();
	    	        }
	    	    }).create();
			return dialog;
		case DIALOG_CONFIRM_NAVIGATE:
			dialog = new AlertDialog.Builder(this)
		    	    .setTitle(getResources().getString(R.string.view_online_warning))
		    	    .setPositiveButton(getResources().getString(R.string.view),
		    	    		new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
		        			// The behavior for invalid URIs might be nasty, but
		        			// we'll cross that bridge if we come to it.
		        			Uri uri = Uri.parse(content);
		        			startActivity(new Intent(Intent.ACTION_VIEW)
		        							.setData(uri));
		    	        }
		    	    }).setNegativeButton(getResources().getString(R.string.cancel),
		    	    		new DialogInterface.OnClickListener() {
		    	        public void onClick(DialogInterface dialog, int whichButton) {
		    	        	// do nothing
		    	        }
		    	    }).create();
			return dialog;
		case DIALOG_CONFIRM_DELETE:
			dialog = new AlertDialog.Builder(this)
		    	    .setTitle(getResources().getString(R.string.item_delete_confirm))
		    	    .setPositiveButton(getResources().getString(R.string.menu_delete), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							Item i = Item.load(itemKey, db);
							i.delete(db);
							finish();
		    	        }
		    	    }).setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
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
            	Toast.makeText(getApplicationContext(), getResources().getString(R.string.sync_log_in_first), 
        				Toast.LENGTH_SHORT).show();
            	return true;
        	}
        	Log.d(TAG, "Preparing sync requests, starting with present item");
        	APIRequest req;
        	if (APIRequest.API_CLEAN.equals(item.dirty)) {
        		ArrayList<Item> items = new ArrayList<Item>();
        		items.add(item);
        		req = APIRequest.add(items);
        	} else {
        		req = APIRequest.update(item);
        	}
        	new ZoteroAPITask(getBaseContext()).execute(req);
        	Toast.makeText(getApplicationContext(), getResources().getString(R.string.sync_started), 
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
    
    /**
     * List adapter that provides our bundles in the appropriate way
     */
    public class BundleListAdapter extends BaseExpandableListAdapter {
        
        public ArrayList<Bundle> bundles;

        public Bundle getChild(int groupPosition, int childPosition) {
        	return bundles.get(groupPosition);
        }

        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        public int getChildrenCount(int groupPosition) {
            return bundles.get(groupPosition).size();
        }

        public TextView getGenericView() {
            // Layout parameters for the ExpandableListView
            AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 64);

            TextView textView = new TextView(ItemDataActivity.this);
            textView.setLayoutParams(lp);
            // Center the text vertically
            textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
            // Set the text starting position
            textView.setPadding(36, 0, 0, 0);
            return textView;
        }

        public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                View convertView, ViewGroup parent) {
        	Log.d(TAG, "childview: "+childPosition);
            TextView textView = getGenericView();
            textView.setText(getChild(groupPosition, childPosition).getString("content"));
            return textView;
        }

        public Bundle getGroup(int groupPosition) {
            return bundles.get(groupPosition);
        }

        public int getGroupCount() {
            return bundles.size();
        }

        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                ViewGroup parent) {
        	
        	Log.d(TAG, "groupview: "+groupPosition);
        	
    		View row;
    		
    		Bundle b = getGroup(groupPosition);
    		String label = b.getString("label");
    		String content = "";

    		if ("children".equals(label)) {
    			int notes = b.getInt("noteCount", 0);
    			int atts = b.getInt("attachmentCount", 0);
        		if (notes == 0 && atts == 0) getResources().getString(R.string.item_attachment_info_none);
    			else content = getResources().getString(R.string.item_attachment_info_template, notes, atts);
    		} else if ("collections".equals((getGroup(groupPosition).getString("label")))) {
    			int count = b.getInt("collectionCount", 0);
        		content = getResources().getString(R.string.item_collection_count, count);
    		} else {
    			content = b.getString("content");
    		}
    	     		
            // We are reusing views, but we need to initialize it if null
    		if (null == convertView) {
                LayoutInflater inflater = getLayoutInflater();
    			row = inflater.inflate(R.layout.list_data, null);
    		} else {
    			row = convertView;
    		}

    		/* Our layout has just two fields */
    		TextView tvLabel = (TextView) row.findViewById(R.id.data_label);
            tvLabel.setPadding(36, 0, 0, 0);
    		TextView tvContent = (TextView) row.findViewById(R.id.data_content);
            tvContent.setPadding(36, 0, 0, 0);

    		/* Since the field names are the API / internal form, we
        	 * attempt to get a localized, human-readable version. */
    		tvLabel.setText(Item.localizedStringForString(label));
    		if ("title".equals(label) || "note".equals(label)) {
    			tvContent.setText(Html.fromHtml(content));
    		} else {
        		tvContent.setText(content);
    		}
     
    		return row;
        }

        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        public boolean hasStableIds() {
            return true;
        }

    }
}
