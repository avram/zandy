package org.zotero.client;

import java.util.ArrayList;

import org.zotero.client.data.Item;
import org.zotero.client.task.APIRequest;
import org.zotero.client.task.ZoteroAPITask;

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


public class ItemDataActivity extends ListActivity {

	private static final String TAG = "org.zotero.client.ItemDataActivity";
	
	static final int DIALOG_SINGLE_VALUE = 0;
	static final int DIALOG_ITEM_TYPE = 1;
//	static final int DIALOG_CREATORS = 2;
//	static final int DIALOG_TAGS = 3;
	static final int DIALOG_CONFIRM_NAVIGATE = 4;	
		
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
                
        /* Get the incoming data from the calling activity */
        // XXX Note that we don't know what to do when there is no key assigned
        String itemKey = getIntent().getStringExtra("org.zotero.client.itemKey");
        final Item item = Item.load(itemKey);
        
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
        		tvContent.setText(getItem(position).getString("content"));
         
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
    		    	i.putExtra("org.zotero.client.itemKey", item.getKey());
        	    	startActivity(i);
        	    	return;
        		} else if (row.getString("label").equals("tags")) {
        	    	Log.d(TAG, "Trying to start tag activity");
        	    	Intent i = new Intent(getBaseContext(), TagActivity.class);
        	    	i.putExtra("org.zotero.client.itemKey", item.getKey());
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
        			removeDialog(DIALOG_ITEM_TYPE);
        			showDialog(DIALOG_ITEM_TYPE, row);
        			return true;
        		} else if (row.getString("label").equals("creators")) {
        	    	Log.d(TAG, "Trying to start creators activity");
        	    	Intent i = new Intent(getBaseContext(), CreatorActivity.class);
    		    	i.putExtra("org.zotero.client.itemKey", item.getKey());
        	    	startActivity(i);
        	    	return true;
        		} else if (row.getString("label").equals("tags")) {
        	    	Log.d(TAG, "Trying to start tag activity");
        	    	Intent i = new Intent(getBaseContext(), TagActivity.class);
        	    	i.putExtra("org.zotero.client.itemKey", item.getKey());
        	    	startActivity(i);
        			return true;
        		}
    			removeDialog(DIALOG_SINGLE_VALUE);
        		showDialog(DIALOG_SINGLE_VALUE, row);
        		return true;
          }
        });

    }
    
    /*
     * Just one kind of dialog for now -- a value editor
     */
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
		/* Creators editor
		 * Since we've mangled the JSON, we'll need to load the item anew
		 */
//		case DIALOG_CREATORS:
//			return null;
		/* Tags editor
		 * We'll need to load the item anew from DB to get real tags
		 */
//		case DIALOG_TAGS:
//			return null;
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
        	Log.d(TAG, "Preparing sync requests");
        	Item.queue();
        	if (!Item.queue.isEmpty()) {
        		for (Item i : Item.queue) {
        			Log.d(TAG, "Syncing dirty item: "+i.getTitle());
        			// XXX We're not queueing here--
        			new ZoteroAPITask("NZrpJ7YDnz8U6NPbbonerxlt").execute(APIRequest.update(i));
        		}
        	}
        	
        	// We should handle collections here too, once they can be modified
        	
        	return true;
        case R.id.quit:
        	finish();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
}