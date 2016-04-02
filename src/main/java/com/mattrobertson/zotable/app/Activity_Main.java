/*******************************************************************************
 * This file is part of Zotable.
 *
 * Zotable is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Zotable is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Zotable.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.mattrobertson.zotable.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;

import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import oauth.signpost.http.HttpParameters;

/**
 * Created by Matt on 7/22/2015.
 */
public class Activity_Main extends FragmentActivity {

    private static final String TAG = "Activity_Main";

    private CommonsHttpOAuthConsumer httpOAuthConsumer;
    private OAuthProvider httpOAuthProvider;

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;

    private Button btnLogin;
    private LinearLayout conItems, conCollections, conTags, conFavorites;
    private TextView tvItemsLabel, tvCollectionsLabel, tvTagsLabel, tvFavsLabel, tvSettingsLabel;
    private ImageView imgItems, imgCollections, imgTags, imgFavs, imgSettings;

    private Fragment fragment;

    private Bundle b = new Bundle();

    private String curTitle = "All Items";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);

        drawerToggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                R.string.drawer_open,
                R.string.drawer_close
        ) {
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);

                if (getActionBar() != null)
                    getActionBar().setTitle(curTitle);

                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);

                if (getActionBar() != null)
                    getActionBar().setTitle(getResources().getString(R.string.app_name));

                invalidateOptionsMenu();
            }
        };

        LinearLayout leftDrawerFrame = (LinearLayout)findViewById(R.id.left_drawer);
        leftDrawerFrame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Do nothing -- intercepts touches to background listview
            }
        });

        btnLogin = (Button)findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    public void run() {
                        startOAuth();
                    }
                }).start();
            }
        });

        // If logged in, hide login button -- if not, open drawer for user to log in
        if (ServerCredentials.check(getBaseContext())) {
            btnLogin.setVisibility(View.GONE);

            RelativeLayout conSearch = (RelativeLayout)findViewById(R.id.conSearch);
            conSearch.setVisibility(View.VISIBLE);
            final EditText etSearch = (EditText)findViewById(R.id.etSearch);

            Button btnDoSearch = (Button)findViewById(R.id.btnDoSearch);
            btnDoSearch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent iSearch = new Intent(Activity_Main.this, SearchActivity.class);
                    iSearch.putExtra("query",etSearch.getText().toString());
                    startActivity(iSearch);
                }
            });
        }
        else {
            drawerLayout.openDrawer(Gravity.START);
        }

        conItems = (LinearLayout)findViewById(R.id.conItems);
        conCollections = (LinearLayout)findViewById(R.id.conCollections);
        conTags = (LinearLayout)findViewById(R.id.conTags);
        conFavorites = (LinearLayout)findViewById(R.id.conFavorites);

        tvItemsLabel = (TextView)findViewById(R.id.tvItemsLabel);
        tvCollectionsLabel = (TextView)findViewById(R.id.tvCollectionsLabel);
        tvTagsLabel = (TextView)findViewById(R.id.tvTagsLabel);
        tvFavsLabel = (TextView)findViewById(R.id.tvFavoritesLabel);
        tvSettingsLabel = (TextView)findViewById(R.id.tvSettingsLabel);

        DrawerNavListener navListener = new DrawerNavListener();

        conItems.setOnClickListener(navListener);
        conCollections.setOnClickListener(navListener);
        conTags.setOnClickListener(navListener);
        conFavorites.setOnClickListener(navListener);
        tvSettingsLabel.setOnClickListener(navListener);

        imgItems = (ImageView)findViewById(R.id.imgItemsIcon);
        imgCollections = (ImageView)findViewById(R.id.imgCollectionsIcon);
        imgTags = (ImageView)findViewById(R.id.imgTagsIcon);
        imgFavs = (ImageView)findViewById(R.id.imgFavoritesIcon);

        fragment = new Fragment_Items();

        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.content_frame, fragment)
                .commit();

        if (getActionBar() != null) {
            getActionBar().setTitle(curTitle);

            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setHomeButtonEnabled(true);
        }

        // Set the drawer toggle as the DrawerListener
        drawerLayout.setDrawerListener(drawerToggle);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle your other action bar items...

        return super.onOptionsItemSelected(item);
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
            toastError(e.getMessage());
        } catch (OAuthNotAuthorizedException e) {
            toastError(e.getMessage());
        } catch (OAuthExpectationFailedException e) {
            toastError(e.getMessage());
        } catch (OAuthCommunicationException e) {
            toastError(e.getMessage());
        }
    }

    private void toastError(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(Activity_Main.this, message, Toast.LENGTH_LONG);
            }
        });
    }

    /**
     * Receives intents that the app knows how to interpret. These will probably
     * all be URIs with the protocol "zotable://".
     *
     * This is currently only used to receive OAuth responses, but it could be
     * used with things like zotable://select and zotable://attachment in the
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

            // showDialog(DIALOG_CHOOSE_COLLECTION); -- no longer needed(?)
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
					    			 * These settings live in the Zotero preferences_old tree.
					    			 */
                                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(Activity_Main.this);
                                SharedPreferences.Editor editor = settings.edit();
                                // For Zotero, the key and secret are identical, it seems
                                editor.putString("user_key", userKey);
                                editor.putString("user_secret", userSecret);
                                editor.putString("user_id", userID);

                                editor.commit();

                                // setUpLoggedInUser(); -- no longer needed(?)

                                // If logged in & Items fragment has been initialized, sync items through
                                // fragment's sync method
                                if (fragment != null) {
                                    if (fragment.getClass() == Fragment_Items.class) {
                                        ((Fragment_Items)fragment).sync();
                                    }
                                }

                                // Close the nav drawer & hide the login button
                                drawerLayout.closeDrawers();
                                btnLogin.setVisibility(View.GONE);
                            }
                        });
                    } catch (OAuthMessageSignerException e) {
                        toastError(e.getMessage());
                    } catch (OAuthNotAuthorizedException e) {
                        toastError(e.getMessage());
                    } catch (OAuthExpectationFailedException e) {
                        toastError(e.getMessage());
                    } catch (OAuthCommunicationException e) {
                        toastError("Error communicating with server. Check your time settings, network connectivity, and try again. OAuth error: " + e.getMessage());
                    }
                }
            }).start();
        }
    }

    private class DrawerNavListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Fragment fragment = new Fragment_Items();

            if (v.getId() == R.id.conItems) {
                fragment = new Fragment_Items();
                curTitle = "All Items";

                // Update Drawer appearance
                tvItemsLabel.setTypeface(null, Typeface.BOLD);
                tvItemsLabel.setTextColor(getResources().getColor(R.color.primary));
                tvCollectionsLabel.setTypeface(null, Typeface.NORMAL);
                tvCollectionsLabel.setTextColor(getResources().getColor(R.color.text_color));
                tvTagsLabel.setTypeface(null, Typeface.NORMAL);
                tvTagsLabel.setTextColor(getResources().getColor(R.color.text_color));
                tvFavsLabel.setTypeface(null, Typeface.NORMAL);
                tvFavsLabel.setTextColor(getResources().getColor(R.color.text_color));

                imgItems.setImageResource(R.drawable.ic_item_blue);
                imgCollections.setImageResource(R.drawable.ic_collection_black);
                imgTags.setImageResource(R.drawable.ic_tag_black);
                imgFavs.setImageResource(R.drawable.ic_favorites_black);

                FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.beginTransaction()
                        .replace(R.id.content_frame, fragment)
                        .commit();
            }
            else if (v.getId() == R.id.conCollections) {
                fragment = new Fragment_Collections();
                curTitle = "Collections";

                // Update Drawer appearance
                tvItemsLabel.setTypeface(null, Typeface.NORMAL);
                tvItemsLabel.setTextColor(getResources().getColor(R.color.text_color));
                tvCollectionsLabel.setTypeface(null, Typeface.BOLD);
                tvCollectionsLabel.setTextColor(getResources().getColor(R.color.primary));
                tvTagsLabel.setTypeface(null, Typeface.NORMAL);
                tvTagsLabel.setTextColor(getResources().getColor(R.color.text_color));
                tvFavsLabel.setTypeface(null, Typeface.NORMAL);
                tvFavsLabel.setTextColor(getResources().getColor(R.color.text_color));

                imgItems.setImageResource(R.drawable.ic_item_black);
                imgCollections.setImageResource(R.drawable.ic_collection_blue);
                imgTags.setImageResource(R.drawable.ic_tag_black);
                imgFavs.setImageResource(R.drawable.ic_favorites_black);

                FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.beginTransaction()
                        .replace(R.id.content_frame, fragment)
                        .commit();
            }
            else if (v.getId() == R.id.conTags) {
                fragment = new Fragment_Tags();
                curTitle = "Tags";

                // Update Drawer appearance
                tvItemsLabel.setTypeface(null, Typeface.NORMAL);
                tvItemsLabel.setTextColor(getResources().getColor(R.color.text_color));
                tvCollectionsLabel.setTypeface(null, Typeface.NORMAL);
                tvCollectionsLabel.setTextColor(getResources().getColor(R.color.text_color));
                tvTagsLabel.setTypeface(null, Typeface.BOLD);
                tvTagsLabel.setTextColor(getResources().getColor(R.color.primary));
                tvFavsLabel.setTypeface(null, Typeface.NORMAL);
                tvFavsLabel.setTextColor(getResources().getColor(R.color.text_color));

                imgItems.setImageResource(R.drawable.ic_item_black);
                imgCollections.setImageResource(R.drawable.ic_collection_black);
                imgTags.setImageResource(R.drawable.ic_tag_blue);
                imgFavs.setImageResource(R.drawable.ic_favorites_black);

                FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.beginTransaction()
                        .replace(R.id.content_frame, fragment)
                        .commit();
            }
            else if (v.getId() == R.id.conFavorites) {
                fragment = new Fragment_Favorites();
                curTitle = "Favorites";

                // Update Drawer appearance
                tvItemsLabel.setTypeface(null, Typeface.NORMAL);
                tvItemsLabel.setTextColor(getResources().getColor(R.color.text_color));
                tvCollectionsLabel.setTypeface(null, Typeface.NORMAL);
                tvCollectionsLabel.setTextColor(getResources().getColor(R.color.text_color));
                tvTagsLabel.setTypeface(null, Typeface.NORMAL);
                tvTagsLabel.setTextColor(getResources().getColor(R.color.text_color));
                tvFavsLabel.setTypeface(null, Typeface.BOLD);
                tvFavsLabel.setTextColor(getResources().getColor(R.color.primary));

                imgItems.setImageResource(R.drawable.ic_item_black);
                imgCollections.setImageResource(R.drawable.ic_collection_black);
                imgTags.setImageResource(R.drawable.ic_tag_black);
                imgFavs.setImageResource(R.drawable.ic_favorites_blue);

                FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.beginTransaction()
                        .replace(R.id.content_frame, fragment)
                        .commit();
            }
            else if (v.getId() == R.id.tvSettingsLabel) {
                Intent i = new Intent(getBaseContext(), Activity_Preference.class);
                startActivity(i);
            }

            drawerLayout.closeDrawers();
        }
    }
}
