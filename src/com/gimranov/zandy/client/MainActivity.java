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
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.gimranov.zandy.client.data.Database;
import com.gimranov.zandy.client.data.Item;
import com.gimranov.zandy.client.data.ItemCollection;

public class MainActivity extends Activity implements OnClickListener {
	private CommonsHttpOAuthConsumer httpOAuthConsumer;
	private OAuthProvider httpOAuthProvider;
	
	private static final String TAG = "com.gimranov.zandy.client.MainActivity";
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);
        
        Button collectionButton = (Button)findViewById(R.id.collectionButton);
        collectionButton.setOnClickListener(this);
        Button itemButton = (Button)findViewById(R.id.itemButton);
        itemButton.setOnClickListener(this);
        Button loginButton = (Button)findViewById(R.id.loginButton);
        loginButton.setOnClickListener(this);
        
        // Let items in on the fun
        Item.db = new Database(getBaseContext());
        XMLResponseParser.db = Item.db;
        ItemCollection.db = Item.db;
        
		SharedPreferences settings = getBaseContext().getSharedPreferences("zotero_prefs", 0);
		String userID = settings.getString("user_id", null);
		String userKey = settings.getString("user_key", null);
		
		if (userID != null && userKey != null) {
			loginButton.setText("Logged in");
			loginButton.setClickable(false);
		}
    }
    
    /**
     * Implementation of the OnClickListener interface, to handle button events.
     * 
     * Note: When adding a button, it needs to be added here, but the ClickListener
     * needs to be set in the main onCreate(..) as well.
     */
    public void onClick(View v) {
    	Log.d(TAG,"Click on: "+v.getId());
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
    		Log.w(TAG,"Uncaught click on: "+v.getId());
    	}
    }
    
    /** 
     * Makes the OAuth call. The response on the callback is handled by the onNewIntent(..)
     * method below.
     * 
     * This will send the user to the OAuth server to get set up.
     */
    protected void startOAuth() {
    	try {
    		this.httpOAuthConsumer = new CommonsHttpOAuthConsumer(ServerCredentials.CONSUMERKEY,
    													ServerCredentials.CONSUMERSECRET);
    		this.httpOAuthProvider = new DefaultOAuthProvider(ServerCredentials.OAUTHREQUEST,
    			ServerCredentials.OAUTHACCESS, ServerCredentials.OAUTHAUTHORIZE);
    	
    		String authUrl;
    		authUrl = httpOAuthProvider.retrieveRequestToken(httpOAuthConsumer, ServerCredentials.CALLBACKURL);
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
     * Receives intents that the app knows how to interpret. These will probably all be
     * URIs with the protocol "zotero://".
     * 
     * This is currently only used to receive OAuth responses, but it could be used with
     * things like zotero://select and zotero://attachment in the future.
     */
    @Override
    protected void onNewIntent(Intent intent) {
    	super.onNewIntent(intent);
    	
    	Uri uri = intent.getData();
    	
    	if (uri != null) {
    		/* TODO The logic should have cases for the various things coming in
    		 * on this protocol.
    		 */ 
    		String verifier = uri.getQueryParameter(oauth.signpost.OAuth.OAUTH_VERIFIER);
    		try {
    			/*
    			 * Here, we're handling the callback from the completed OAuth. We don't need to do
    			 * anything highly visible, although it would be nice to show a Toast or something. 
    			 */
				this.httpOAuthProvider.retrieveAccessToken(this.httpOAuthConsumer, verifier);
				HttpParameters params = this.httpOAuthProvider.getResponseParameters();
				String userID = params.getFirst("userID");
				Log.d(TAG, "uid: " +userID);
				String userKey = this.httpOAuthConsumer.getToken();
				Log.d(TAG, "ukey: " +userKey);
				String userSecret = this.httpOAuthConsumer.getTokenSecret();
				Log.d(TAG, "usecret: " +userSecret);
				
				/*
				 * These settings live in the Zotero preferences tree.
				 */
				SharedPreferences settings = getBaseContext().getSharedPreferences("zotero_prefs", 0);
				SharedPreferences.Editor editor = settings.edit();
				// For Zotero, the key and secret are identical, it seems
				editor.putString("user_key", userKey);
				editor.putString("user_secret", userSecret);
				editor.putString("user_id", userID);
				
				editor.commit();
				
		        Button loginButton = (Button)findViewById(R.id.loginButton);
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
}
