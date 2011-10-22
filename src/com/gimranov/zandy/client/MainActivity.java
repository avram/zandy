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

import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import oauth.signpost.http.HttpParameters;
import android.app.Activity;
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

import com.gimranov.zandy.client.data.Attachment;
import com.gimranov.zandy.client.data.Database;
import com.gimranov.zandy.client.data.Item;
import com.gimranov.zandy.client.data.ItemCollection;
import com.gimranov.zandy.client.task.APIRequest;
import com.gimranov.zandy.client.task.ZoteroAPITask;

public class MainActivity extends Activity implements OnClickListener {
	private CommonsHttpOAuthConsumer httpOAuthConsumer;
	private OAuthProvider httpOAuthProvider;

	private static final String TAG = "com.gimranov.zandy.client.MainActivity";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
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
			
			// Now we're dealing with share link, it seems.
			// For now, just add it to the main library-- we'd like to let the person choose a library,
			// but not yet.
			
			Item item = new Item(this, "webpage");
			item.save();
			Item.set(item.getKey(), "url", extras.getString("android.intent.extra.TEXT"));
			Item.set(item.getKey(), "title", extras.getString("android.intent.extra.SUBJECT"));
			Item.setTag(item.getKey(), null, "#added-by-zandy", 0);
			Log.d(TAG, "Loading item data with key: "+item.getKey());
			// We create and issue a specified intent with the necessary data
	    	Intent i = new Intent(this, ItemDataActivity.class);
	    	i.putExtra("com.gimranov.zandy.client.itemKey", item.getKey());
	    	i.putExtra("com.gimranov.zandy.client.itemDbId", item.dbId);
	    	startActivity(i);
		}

		setContentView(R.layout.main);

		Button collectionButton = (Button) findViewById(R.id.collectionButton);
		collectionButton.setOnClickListener(this);
		Button itemButton = (Button) findViewById(R.id.itemButton);
		itemButton.setOnClickListener(this);
		Button loginButton = (Button) findViewById(R.id.loginButton);
		loginButton.setOnClickListener(this);

		// Let items in on the fun
		Item.db = new Database(getBaseContext());
		XMLResponseParser.db = Item.db;
		ItemCollection.db = Item.db;
		Attachment.db = Item.db;

		if (ServerCredentials.check(getBaseContext())) {
			loginButton.setText("Logged in");
			loginButton.setClickable(false);
		}
	}

	public void onResume() {
		Button loginButton = (Button) findViewById(R.id.loginButton);

		if (!ServerCredentials.check(getBaseContext())) {
			loginButton.setText("Log in");
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
		if (intent != null)
			uri = intent.getData();
		else
			return;
		
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
				loginButton.setText("Logged in");
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
				Toast.makeText(getApplicationContext(), "Log in to sync",
						Toast.LENGTH_SHORT).show();
				return true;
			}
        	Log.d(TAG, "Making sync request for all collections");
        	APIRequest req = new APIRequest(ServerCredentials.APIBASE 
        			+ ServerCredentials.prep(getBaseContext(), ServerCredentials.COLLECTIONS),
        			"get", null);
			req.disposition = "xml";
			new ZoteroAPITask(getBaseContext()).execute(req);
			Toast.makeText(getApplicationContext(), "Started syncing...",
					Toast.LENGTH_SHORT).show();
			return true;
		case R.id.do_prefs:
			startActivity(new Intent(this, SettingsActivity.class));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}
