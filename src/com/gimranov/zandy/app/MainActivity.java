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
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.gimranov.zandy.app.data.Database;
import com.gimranov.zandy.app.data.Item;
import com.gimranov.zandy.app.data.ItemCollection;
import com.gimranov.zandy.app.task.APIRequest;
import com.gimranov.zandy.app.task.ZoteroAPITask;

public class MainActivity extends Activity implements OnClickListener {
	private CommonsHttpOAuthConsumer httpOAuthConsumer;
	private OAuthProvider httpOAuthProvider;

	private static final String TAG = "com.gimranov.zandy.app.MainActivity";

	static final int DIALOG_CHOOSE_COLLECTION = 1;
	
	private Database db;
	
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
			
			showDialog(DIALOG_CHOOSE_COLLECTION, b);
		}

		setContentView(R.layout.main);

		Button collectionButton = (Button) findViewById(R.id.collectionButton);
		collectionButton.setOnClickListener(this);
		Button itemButton = (Button) findViewById(R.id.itemButton);
		itemButton.setOnClickListener(this);
		Button loginButton = (Button) findViewById(R.id.loginButton);
		loginButton.setOnClickListener(this);

		if (ServerCredentials.check(getBaseContext())) {
			loginButton.setText(getResources().getString(R.string.logged_in));
			loginButton.setClickable(false);
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
			startOAuth();
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
				
				showDialog(DIALOG_CHOOSE_COLLECTION, b);
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
			String verifier = uri
					.getQueryParameter(oauth.signpost.OAuth.OAUTH_VERIFIER);
			try {
				/*
				 * Here, we're handling the callback from the completed OAuth.
				 * We don't need to do anything highly visible, although it
				 * would be nice to show a Toast or something.
				 */
				this.httpOAuthProvider.retrieveAccessToken(
						this.httpOAuthConsumer, verifier);
				HttpParameters params = this.httpOAuthProvider
						.getResponseParameters();
				String userID = params.getFirst("userID");
				Log.d(TAG, "uid: " + userID);
				String userKey = this.httpOAuthConsumer.getToken();
				Log.d(TAG, "ukey: " + userKey);
				String userSecret = this.httpOAuthConsumer.getTokenSecret();
				Log.d(TAG, "usecret: " + userSecret);

				/*
				 * These settings live in the Zotero preferences tree.
				 */
				SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
				SharedPreferences.Editor editor = settings.edit();
				// For Zotero, the key and secret are identical, it seems
				editor.putString("user_key", userKey);
				editor.putString("user_secret", userSecret);
				editor.putString("user_id", userID);

				editor.commit();

				Button loginButton = (Button) findViewById(R.id.loginButton);
				loginButton.setText(getResources().getString(R.string.logged_in));
				loginButton.setClickable(false);

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
        	APIRequest req = new APIRequest(ServerCredentials.APIBASE 
        			+ ServerCredentials.prep(getBaseContext(), ServerCredentials.COLLECTIONS),
        			"get", null);
			req.disposition = "xml";
			new ZoteroAPITask(getBaseContext()).execute(req);
			Toast.makeText(getApplicationContext(), getResources().getString(R.string.sync_started),
					Toast.LENGTH_SHORT).show();
			return true;
		case R.id.do_prefs:
			//startActivity(new Intent(this, SettingsActivity.class));
			startActivity(new Intent(this, LookupActivity.class));
			return true;
        case R.id.do_search:
        	onSearchRequested();
            return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	protected Dialog onCreateDialog(int id, Bundle b) {
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
