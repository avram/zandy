package org.zotero.client;

import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import oauth.signpost.http.HttpParameters;

import org.zotero.client.data.Database;
import org.zotero.client.data.Item;
import org.zotero.client.data.ItemCollection;

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

public class MainActivity extends Activity implements OnClickListener {
	private CommonsHttpOAuthConsumer httpOAuthConsumer;
	private OAuthProvider httpOAuthProvider;
	
	private static final String TAG = "org.zotero.client.MainActivity";
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);
        
        Button collectionButton = (Button)findViewById(R.id.collectionButton);
        collectionButton.setOnClickListener(this);
        Button itemButton = (Button)findViewById(R.id.itemButton);
        itemButton.setOnClickListener(this);
        
        // Let items in on the fun
        Item.db = new Database(getBaseContext());
        XMLResponseParser.db = Item.db;
        ItemCollection.db = Item.db;  
    }
    
    /**
     * Implementation of the OnClickListener interface, to handle button events.
     */
    public void onClick(View v) {
    	if (v.getId() == R.id.collectionButton) {
	    	Log.d(TAG, "Trying to start collection activity");
	    	Intent i = new Intent(this, CollectionActivity.class);
	    	startActivity(i);
    	} else if (v.getId() == R.id.itemButton) {
	    	Log.d(TAG, "Trying to start all-item activity");
	    	Intent i = new Intent(this, ItemActivity.class);
	    	startActivity(i);
    	}
    }
    
    /** 
     * Makes the OAuth call
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
     * This is currently only used to receive OAuth responses.
     */
    @Override
    protected void onNewIntent(Intent intent) {
    	super.onNewIntent(intent);
    	
    	Uri uri = intent.getData();
    	
    	if (uri != null) {
    		String verifier = uri.getQueryParameter(oauth.signpost.OAuth.OAUTH_VERIFIER);
    		try {
				this.httpOAuthProvider.retrieveAccessToken(this.httpOAuthConsumer, verifier);
				HttpParameters params = this.httpOAuthProvider.getResponseParameters();
				String userID = params.getFirst("userID");
				String userKey = this.httpOAuthConsumer.getToken();
				String userSecret = this.httpOAuthConsumer.getTokenSecret();
				
				SharedPreferences settings = getBaseContext().getSharedPreferences("zotero_prefs", 0);
				SharedPreferences.Editor editor = settings.edit();
				// For Zotero, the key and secret are identical, it seems
				editor.putString("user_key", userKey);
				editor.putString("user_secret", userSecret);
				editor.putString("user_id", userID);			
				
				editor.commit();
							
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
