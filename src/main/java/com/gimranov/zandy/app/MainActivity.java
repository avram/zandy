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

import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import oauth.signpost.http.HttpParameters;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.gimranov.zandy.app.data.Database;
import com.gimranov.zandy.app.data.Item;
import com.gimranov.zandy.app.data.ItemAdapter;
import com.gimranov.zandy.app.data.ItemCollection;
import com.gimranov.zandy.app.task.APIRequest;
import com.gimranov.zandy.app.task.ZoteroAPITask;

public class MainActivity extends Activity implements OnClickListener {
	private CommonsHttpOAuthConsumer httpOAuthConsumer;
	private OAuthProvider httpOAuthProvider;

	private static final String TAG = "com.gimranov.zandy.app.MainActivity";

	static final int DIALOG_CHOOSE_COLLECTION = 1;
	
	private Database db;
	private Bundle b = new Bundle();
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Let items in on the fun
		db = new Database(getBaseContext());
		
		Intent intent = getIntent();
		String action = intent.getAction();
		if (action != null
				&& action.equals("android.intent.action.SEND")
				&& intent.getExtras() != null) {
			// Browser sends us no data, just extras
			Bundle extras = intent.getExtras();
			for (String s : extras.keySet()) {
				try {
					Log.d("TAG","Got extra: "+s +" => "+extras.getString(s));
				} catch (ClassCastException e) {
					Log.e(TAG, "Not a string, it seems", e);
				}
			}
			
			Bundle b = new Bundle();
			b.putString("url", extras.getString("android.intent.extra.TEXT"));
			b.putString("title", extras.getString("android.intent.extra.SUBJECT"));
			this.b = b;
			showDialog(DIALOG_CHOOSE_COLLECTION);
		}

		setContentView(R.layout.main);

		Button collectionButton = (Button) findViewById(R.id.collectionButton);
		collectionButton.setOnClickListener(this);
		Button itemButton = (Button) findViewById(R.id.itemButton);
		itemButton.setOnClickListener(this);
		Button loginButton = (Button) findViewById(R.id.loginButton);
		loginButton.setOnClickListener(this);

		if (ServerCredentials.check(getBaseContext())) {
			loginButton.setVisibility(View.GONE);

            ItemAdapter adapter = new ItemAdapter(this, getCursor("timestamp ASC, item_title COLLATE NOCASE"));
            ListView lv = ((ListView) findViewById(android.R.id.list));
            lv.setAdapter(adapter);

            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
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
	}

	public void onResume() {
		Button loginButton = (Button) findViewById(R.id.loginButton);

		if (!ServerCredentials.check(getBaseContext())) {
			loginButton.setText(getResources().getString(R.string.log_in));
			loginButton.setClickable(true);
		}
		
		super.onResume();
	}
	
	/**
	 * Implementation of the OnClickListener interface, to handle button events.
	 * 
	 * Note: When adding a button, it needs to be added here, but the
	 * ClickListener needs to be set in the main onCreate(..) as well.
	 */
	public void onClick(View v) {
		Log.d(TAG, "Click on: " + v.getId());
		if (v.getId() == R.id.collectionButton) {
			Log.d(TAG, "Trying to start collection activity");
			Intent i = new Intent(this, CollectionActivity.class);
			startActivity(i);
		} else if (v.getId() == R.id.itemButton) {
			Log.d(TAG, "Trying to start all-item activity");
			Intent i = new Intent(this, ItemActivity.class);
			startActivity(i);
		} else if (v.getId() == R.id.loginButton) {
			Log.d(TAG, "Starting OAuth");
			  new Thread(new Runnable() {
				    public void run() {
						startOAuth();
				    }
				  }).start();

		} else {
			Log.w(TAG, "Uncaught click on: " + v.getId());
		}
	}

	/**
	 * Makes the OAuth call. The response on the callback is handled by the
	 * onNewIntent(..) method below.
	 * 
	 * This will send the user to the OAuth server to get set up.
	 */
	protected void startOAuth() {
		try {
			this.httpOAuthConsumer = new CommonsHttpOAuthConsumer(
					ServerCredentials.CONSUMERKEY,
					ServerCredentials.CONSUMERSECRET);
			this.httpOAuthProvider = new DefaultOAuthProvider(
					ServerCredentials.OAUTHREQUEST,
					ServerCredentials.OAUTHACCESS,
					ServerCredentials.OAUTHAUTHORIZE);

			String authUrl;
			authUrl = httpOAuthProvider.retrieveRequestToken(httpOAuthConsumer,
					ServerCredentials.CALLBACKURL);
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)));
		} catch (OAuthMessageSignerException e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
		} catch (OAuthNotAuthorizedException e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
		} catch (OAuthExpectationFailedException e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
		} catch (OAuthCommunicationException e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * Receives intents that the app knows how to interpret. These will probably
	 * all be URIs with the protocol "zotero://".
	 * 
	 * This is currently only used to receive OAuth responses, but it could be
	 * used with things like zotero://select and zotero://attachment in the
	 * future.
	 */
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Log.d(TAG, "Got new intent");
		
		if (intent == null) return;
		
		// Here's what we do if we get a share request from the browser
			String action = intent.getAction();
			if (action != null
					&& action.equals("android.intent.action.SEND")
					&& intent.getExtras() != null) {
				// Browser sends us no data, just extras
				Bundle extras = intent.getExtras();
				for (String s : extras.keySet()) {
					try {
						Log.d("TAG","Got extra: "+s +" => "+extras.getString(s));
					} catch (ClassCastException e) {
						Log.e(TAG, "Not a string, it seems", e);
					}
				}
				
				Bundle b = new Bundle();
				b.putString("url", extras.getString("android.intent.extra.TEXT"));
				b.putString("title", extras.getString("android.intent.extra.SUBJECT"));
				this.b=b;
				showDialog(DIALOG_CHOOSE_COLLECTION);
				return;
			}
		
		/*
		 * It's possible we've lost these to garbage collection, so we
		 * reinstantiate them if they turn out to be null at this point.
		 */
		if (this.httpOAuthConsumer == null)
			this.httpOAuthConsumer = new CommonsHttpOAuthConsumer(
					ServerCredentials.CONSUMERKEY,
					ServerCredentials.CONSUMERSECRET);
		if (this.httpOAuthProvider == null)
			this.httpOAuthProvider = new DefaultOAuthProvider(
					ServerCredentials.OAUTHREQUEST,
					ServerCredentials.OAUTHACCESS,
					ServerCredentials.OAUTHAUTHORIZE);

		/*
		 * Also double-check that intent isn't null, because something here
		 * caused a NullPointerException for a user.
		 */
		Uri uri;
		uri = intent.getData();
		
		if (uri != null) {
			/*
			 * TODO The logic should have cases for the various things coming in
			 * on this protocol.
			 */
			final String verifier = uri
					.getQueryParameter(oauth.signpost.OAuth.OAUTH_VERIFIER);

			new Thread(new Runnable() {
				public void run() {
				    	try {
				    		/*
				    		 * Here, we're handling the callback from the completed OAuth.
				    		 * We don't need to do anything highly visible, although it
				    		 * would be nice to show a Toast or something.
				    		 */
				    		httpOAuthProvider.retrieveAccessToken(
				    				httpOAuthConsumer, verifier);
				    		HttpParameters params = httpOAuthProvider
				    				.getResponseParameters();
				    		final String userID = params.getFirst("userID");
				    		Log.d(TAG, "uid: " + userID);
				    		final String userKey = httpOAuthConsumer.getToken();
				    		Log.d(TAG, "ukey: " + userKey);
				    		final String userSecret = httpOAuthConsumer.getTokenSecret();
				    		Log.d(TAG, "usecret: " + userSecret);
				    		
					    	runOnUiThread(new Runnable(){
					    		public void run(){
					    			/*
					    			 * These settings live in the Zotero preferences tree.
					    			 */
					    			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
					    			SharedPreferences.Editor editor = settings.edit();
					    			// For Zotero, the key and secret are identical, it seems
					    			editor.putString("user_key", userKey);
					    			editor.putString("user_secret", userSecret);
					    			editor.putString("user_id", userID);

					    			editor.commit();

					    			Button loginButton = (Button) findViewById(R.id.loginButton);
					    			loginButton.setText(getResources().getString(R.string.logged_in));
					    			loginButton.setClickable(false);
					    		}
					    	});
				    	} catch (OAuthMessageSignerException e) {
				    		Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
				    	} catch (OAuthNotAuthorizedException e) {
				    		Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
				    	} catch (OAuthExpectationFailedException e) {
				    		Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
				    	} catch (OAuthCommunicationException e) {
				    		Toast.makeText(MainActivity.this, "Error communicating with server. Check your time settings, network connectivity, and try again. OAuth error: "+e.getMessage(), Toast.LENGTH_LONG).show();
				    	}
				    }
			  }).start();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.zotero_menu, menu);
		
		// button doesn't make sense here.
		menu.removeItem(R.id.do_new);
		
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
        	Log.d(TAG, "Making sync request for all collections");
        	ServerCredentials cred = new ServerCredentials(getBaseContext());
        	APIRequest req = APIRequest.fetchCollections(cred);
			new ZoteroAPITask(getBaseContext()).execute(req);
			Toast.makeText(getApplicationContext(), getResources().getString(R.string.sync_started),
					Toast.LENGTH_SHORT).show();
			return true;
		case R.id.do_prefs:
			startActivity(new Intent(this, SettingsActivity.class));
			return true;
        case R.id.do_search:
        	onSearchRequested();
            return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

    public Cursor getCursor(String sortBy) {
        Cursor cursor = db.query("items", Database.ITEMCOLS, null, null, null, null, sortBy, null);
        if (cursor == null) {
            Log.e(TAG, "cursor is null");
        }
        return cursor;
    }

	protected Dialog onCreateDialog(int id) {
		final String url = b.getString("url");
		final String title = b.getString("title");
		AlertDialog dialog;
		switch (id) {			
		case DIALOG_CHOOSE_COLLECTION:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			// Now we're dealing with share link, it seems.
			// For now, just add it to the main library-- we'd like to let the person choose a library,
			// but not yet.
			final ArrayList<ItemCollection> collections = ItemCollection.getCollections(db);
			int size = collections.size();
			String[] collectionNames = new String[size];
			for (int i = 0; i < size; i++) {
				collectionNames[i] = collections.get(i).getTitle();
			}
			builder.setTitle(getResources().getString(R.string.choose_parent_collection))
		    	    .setItems(collectionNames, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int pos) {
		    	            Item item = new Item(getBaseContext(), "webpage");
		    	            item.save(db);
		    	            Log.d(TAG,"New item has key: "+item.getKey() + ", dbId: "+item.dbId);
							Item.set(item.getKey(), "url", url, db);
							Item.set(item.getKey(), "title", title, db);
							Item.setTag(item.getKey(), null, "#added-by-zandy", 1, db);
							collections.get(pos).add(item);
							collections.get(pos).saveChildren(db);
							Log.d(TAG, "Loading item data with key: "+item.getKey());
							// We create and issue a specified intent with the necessary data
					    	Intent i = new Intent(getBaseContext(), ItemDataActivity.class);
					    	i.putExtra("com.gimranov.zandy.app.itemKey", item.getKey());
					    	i.putExtra("com.gimranov.zandy.app.itemDbId", item.dbId);
					    	startActivity(i);
		    	        }
		    	    });
			dialog = builder.create();
			return dialog;
		default:
			Log.e(TAG, "Invalid dialog requested");
			return null;
		}
	}
}
