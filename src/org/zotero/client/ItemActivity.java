package org.zotero.client;

import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;

import org.zotero.client.data.Item;
import org.zotero.client.data.ItemAdapter;
import org.zotero.client.data.ItemCollection;
import org.zotero.client.task.APIRequest;
import org.zotero.client.task.ZoteroAPITask;

import android.app.ListActivity;
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
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.items);
        
        String collectionKey = getIntent().getStringExtra("org.zotero.client.collectionKey");
        // TODO Figure out how we'll address other views that aren't collections
        ItemCollection coll = ItemCollection.load(collectionKey);
        
        ItemAdapter itemAdapter = ItemAdapter.create(getBaseContext(), coll);
        
        setListAdapter(itemAdapter);
        
        ListView lv = getListView();
        lv.setOnItemClickListener(new OnItemClickListener() {
        	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
     			// If we have a click on an item, do something...
        		ItemAdapter adapter = (ItemAdapter) parent.getAdapter();
        		Cursor cur = adapter.getCursor();
        		// Place the cursor at the selected item
        		if (cur.moveToPosition(position)) {
        			// and replace the cursor with one for the selected collection
        			Item coll = Item.load(cur);
        			if (coll != null && coll.getKey() != null) {
        				Log.d(TAG, "Loading item with key: "+coll.getKey());
        				// TODO do something
        				Toast.makeText(getApplicationContext(), "We'd have shown this to you", 
	            				Toast.LENGTH_SHORT).show();
        			} else {
        				// collection loaded was null. why?
        				Log.d(TAG, "Failed loading item at position: "+position);
        			}
        		} else {
        			// failed to move cursor-- show a toast
            		TextView tvTitle = (TextView)view.findViewById(R.id.item_title);
            		Toast.makeText(getApplicationContext(), "Can't open "+tvTitle.getText(), 
            				Toast.LENGTH_SHORT).show();
        		}
        	}
        });
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