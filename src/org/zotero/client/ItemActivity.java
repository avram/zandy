package org.zotero.client;

import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;

import org.zotero.client.data.Item;
import org.zotero.client.data.ItemAdapter;
import org.zotero.client.data.ItemCollection;
import org.zotero.client.task.APIRequest;
import org.zotero.client.task.ZoteroAPITask;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.AlertDialog.Builder;
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


public class ItemActivity extends ListActivity {
	private CommonsHttpOAuthConsumer httpOAuthConsumer;
	private OAuthProvider httpOAuthProvider;

	private static final String TAG = "org.zotero.client.ItemActivity";
	
	static final int DIALOG_VIEW = 0;

	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        ItemAdapter itemAdapter;
        
        setContentView(R.layout.items);
        
        String collectionKey = getIntent().getStringExtra("org.zotero.client.collectionKey");
        // TODO Figure out how we'll address other views that aren't collections
        if (collectionKey != null) {
        	ItemCollection coll = ItemCollection.load(collectionKey);
        	itemAdapter = ItemAdapter.create(getBaseContext(), coll);
        } else {
        	itemAdapter = ItemAdapter.create(getBaseContext());
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
    		    	i.putExtra("org.zotero.client.itemKey", item.getKey());
    		    	startActivity(i);
        		} else {
        			// failed to move cursor-- show a toast
            		TextView tvTitle = (TextView)view.findViewById(R.id.item_title);
            		Toast.makeText(getApplicationContext(), "Can't open "+tvTitle.getText(), 
            				Toast.LENGTH_SHORT).show();
        		}
        	}
        });
    }
    
    protected void onResume() {
		ItemAdapter adapter = (ItemAdapter) getListAdapter();
		// XXX This may be too agressive-- fix if causes issues
		adapter.getCursor().requery();
		adapter.notifyDataSetChanged();
    	super.onResume();
    }
    
	protected Dialog onCreateDialog(int id, Bundle b) {
		CharSequence value = b.getCharSequence("itemType");
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Type: " + value)
		       .setCancelable(true)
		       .setPositiveButton("OK", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                dialog.cancel();
		                removeDialog(0);
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
        	// TODO Make this a collection-specific sync, preceding by de-dirtying
        	Log.d(TAG, "Making sync request");
        	APIRequest req = new APIRequest(ServerCredentials.APIBASE + "/users/5770/collections", "get", "NZrpJ7YDnz8U6NPbbonerxlt");
			req.disposition = "xml";
			new ZoteroAPITask("NZrpJ7YDnz8U6NPbbonerxlt", (CursorAdapter) getListAdapter()).execute(req);	
            return true;
        case R.id.quit:
        	finish();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
}