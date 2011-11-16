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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.http.util.ByteArrayBuffer;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.gimranov.zandy.app.data.Database;

/**
 * Runs lookup routines to create new items
 * @author ajlyon
 *
 */
public class LookupActivity extends Activity implements OnClickListener {

	private static final String TAG = "com.gimranov.zandy.app.LookupActivity";
	
	static final int DIALOG_PROGRESS = 6;
	
	private ProgressDialog mProgressDialog;
	private ProgressThread progressThread;
	private Database db;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        db = new Database(this);
        
        /* Get the incoming data from the calling activity */
        final String identifier = getIntent().getStringExtra("com.gimranov.zandy.app.identifier");
        final String mode = getIntent().getStringExtra("com.gimranov.zandy.app.mode");
        
		setContentView(R.layout.lookup);
        
		Button lookupButton = (Button) findViewById(R.id.lookupButton);
		lookupButton.setOnClickListener(this);
                   
    }
    
	/**
	 * Implementation of the OnClickListener interface, to handle button events.
	 * 
	 * Note: When adding a button, it needs to be added here, but the
	 * ClickListener needs to be set in the main onCreate(..) as well.
	 */
	public void onClick(View v) {
		Log.d(TAG, "Click on: " + v.getId());
		if (v.getId() == R.id.lookupButton) {
			Log.d(TAG, "Trying to start search activity");
			TextView field = (TextView) findViewById(R.id.identifier);
			Editable fieldContents = (Editable) field.getText();
			Bundle b = new Bundle();
			b.putString("mode", "isbn");
			b.putString("identifier", fieldContents.toString());
			showDialog(DIALOG_PROGRESS, b);
		} else {
			Log.w(TAG, "Uncaught click on: " + v.getId());
		}
	}

    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    }
    
    @Override
    public void onResume() {
    	if (db == null) db = new Database(this);
    	super.onResume();
    }
    
	protected Dialog onCreateDialog(int id, Bundle b) {
		switch (id) {			
		case DIALOG_PROGRESS:
			mProgressDialog = new ProgressDialog(this);
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mProgressDialog.setIndeterminate(true);
			mProgressDialog.setMax(100);
			return mProgressDialog;
		default:
			Log.e(TAG, "Invalid dialog requested");
			return null;
		}
	}
	
	protected void onPrepareDialog(int id, Dialog dialog, Bundle b) {
		switch(id) {
		case DIALOG_PROGRESS:
			mProgressDialog.setProgress(0);
			mProgressDialog.setMessage("Looking up item...");
			progressThread = new ProgressThread(handler, b);
			progressThread.start();
		}
	}
	
	
	final Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			if (ProgressThread.STATE_DONE == msg.arg2) {
				if(mProgressDialog.isShowing())
					dismissDialog(DIALOG_PROGRESS);
				// do something-- we're done.
				return;
			}
			
			if (ProgressThread.STATE_PARSING == msg.arg2) {
				mProgressDialog.setMessage("Parsing item data...");
				return;
			}
			
			int total = msg.arg1;
			mProgressDialog.setProgress(total);
			if (total >= 100) {
				dismissDialog(DIALOG_PROGRESS);
				progressThread.setState(ProgressThread.STATE_DONE);
			}
		}
	};
	
	private class ProgressThread extends Thread {
		Handler mHandler;
		Bundle arguments;
		final static int STATE_DONE = 5;
		final static int STATE_FETCHING = 1;
		final static int STATE_PARSING = 6;
		int mState;
		
		ProgressThread(Handler h, Bundle b) {
			mHandler = h;
			arguments = b;
		}
		
		public void run() {
			mState = STATE_FETCHING;
			
			// Setup
			String identifier = arguments.getString("identifier");
			String mode = arguments.getString("mode");
			URL url;
			String urlstring;
			
			if ("isbn".equals(mode)) {
				if (identifier == null || identifier.equals(""))
					identifier = "0674081250";
				urlstring = "http://xisbn.worldcat.org/webservices/xid/isbn/"
							+ identifier
							+ "?method=getMetadata&fl=*&format=json&count=1";
			} else {
				urlstring = "";
			}
			
			try {
				Log.d(TAG, "Fetching from: "+urlstring);
				url = new URL(urlstring);
                
                /* Open a connection to that URL. */
                URLConnection ucon = url.openConnection();                
                /*
                 * Define InputStreams to read from the URLConnection.
                 */
                InputStream is = ucon.getInputStream();
                BufferedInputStream bis = new BufferedInputStream(is, 16000);

                ByteArrayBuffer baf = new ByteArrayBuffer(50);
                int current = 0;
                
                /*
                 * Read bytes to the Buffer until there is nothing more to read(-1).
                 */
    			while (mState == STATE_FETCHING 
    					&& (current = bis.read()) != -1) {
                        baf.append((byte) current);
                        
                        if (baf.length() % 2048 == 0) {
                        	Message msg = mHandler.obtainMessage();
                        	// XXX do real length later
                        	Log.d(TAG, baf.length() + " downloaded so far");
                        	msg.arg1 = baf.length() % 100;
                        	mHandler.sendMessage(msg);
                        }
                }
                String content = new String(baf.toByteArray());
    			Log.d(TAG, content);
    			
    			
	        } catch (IOException e) {
	                Log.e(TAG, "Error: ",e);
	        }
        	Message msg = mHandler.obtainMessage();
        	msg.arg2 = STATE_DONE;
        	mHandler.sendMessage(msg);
		}
		
		public void setState(int state) {
			mState = state;
		}
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
        case R.id.do_prefs:
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
}
