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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import org.apache.http.util.ByteArrayBuffer;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.gimranov.zandy.client.data.Attachment;
import com.gimranov.zandy.client.data.Item;
import com.gimranov.zandy.client.task.APIRequest;
import com.gimranov.zandy.client.task.ZoteroAPITask;

/**
 * This Activity handles displaying and editing attachments. It works almost the same as
 * ItemDataActivity and TagActivity, using a simple ArrayAdapter on Bundles with the creator info.
 * 
 * This currently operates by showing the attachments for a given item
 * 
 * @author ajlyon
 *
 */
public class AttachmentActivity extends ListActivity {

	private static final String TAG = "com.gimranov.zandy.client.AttachmentActivity";
	
	static final int DIALOG_CONFIRM_NAVIGATE = 4;	
	static final int DIALOG_FILE_PROGRESS = 6;	
	static final int DIALOG_CONFIRM_DELETE = 5;	
	
	public Item item;
	private ProgressDialog mProgressDialog;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
                
        /* Get the incoming data from the calling activity */
        // XXX Note that we don't know what to do when there is no key assigned
        String itemKey = getIntent().getStringExtra("com.gimranov.zandy.client.itemKey");
        Item item = Item.load(itemKey);
        this.item = item;
        
        this.setTitle("Attachments for "+item.getTitle());
        
        ArrayList<Attachment> rows = Attachment.forItem(item);
        
        /* 
         * We use the standard ArrayAdapter, passing in our data as a Attachment.
         * Since it's no longer a simple TextView, we need to override getView, but
         * we can do that anonymously.
         */
        setListAdapter(new ArrayAdapter<Attachment>(this, R.layout.list_attach, rows) {
        	@Override
        	public View getView(int position, View convertView, ViewGroup parent) {
        		View row;
        		
                // We are reusing views, but we need to initialize it if null
        		if (null == convertView) {
                    LayoutInflater inflater = getLayoutInflater();
        			row = inflater.inflate(R.layout.list_attach, null);
        		} else {
        			row = convertView;
        		}

        		//TextView tvTitle = (TextView)row.findViewById(R.id.attachment_title);
        		ImageView tvType = (ImageView)row.findViewById(R.id.attachment_type);
        		TextView tvSummary = (TextView)row.findViewById(R.id.attachment_summary);
        		
        		Attachment att = getItem(position);
        		Log.d(TAG, "Have an attachment: "+att.title);
        		
        		tvType.setImageResource(Item.resourceForType(att.getType()));
        		
        		//tvSummary.setText(Item.localizedStringForString(
        		//		att.status));
        		tvSummary.setText(att.title + " Status: " + att.status);
         
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
        		ArrayAdapter<Attachment> adapter = (ArrayAdapter<Attachment>) parent.getAdapter();
        		Attachment row = adapter.getItem(position);
        						
				if (row.url != null && row.url.length() > 0) {
        			Bundle b = new Bundle();
        			b.putString("title", row.title);
        			b.putString("key", row.key);
        			showDialog(DIALOG_FILE_PROGRESS, b);
				}
        	}
        });
    }
    
	protected Dialog onCreateDialog(int id, Bundle b) {
		switch (id) {			
		case DIALOG_CONFIRM_NAVIGATE:
			return null;
		case DIALOG_FILE_PROGRESS:
			Attachment att = Attachment.load(b.getString("key"));
			
			if (!ServerCredentials.sBaseStorageDir.exists())
				ServerCredentials.sBaseStorageDir.mkdir();
			if (!ServerCredentials.sDocumentStorageDir.exists())
				ServerCredentials.sDocumentStorageDir.mkdir();
			//File keyDir = new File(ServerCredentials.sBaseStorageDir,att.key);
			//if (!keyDir.exists())
			//	keyDir.mkdir();
			
			Log.d(TAG, att.status);
			if (att.status.equals(Attachment.ZFS_AVAILABLE)) {
				String sanitized = att.title.replace(' ', '_');
				File file = new File(ServerCredentials.sDocumentStorageDir,sanitized);
				
				SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
				
				Log.d(TAG,"Starting to try and download ZFS-available attachment");
				
				mProgressDialog = new ProgressDialog(this);
				mProgressDialog.setMessage("Downloading file for "+b.getString("title"));
				mProgressDialog.setIndeterminate(true);
				mProgressDialog.setMax(100);
				mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				mProgressDialog.show();
				try {
					if (!ServerCredentials.sBaseStorageDir.exists())
						ServerCredentials.sBaseStorageDir.mkdir();
					if (!ServerCredentials.sDocumentStorageDir.exists())
						ServerCredentials.sDocumentStorageDir.mkdir();
					
					download(new URL(att.url+"?key="+settings.getString("user_key","")),
							file);
					att.filename = file.getPath();
					att.status = Attachment.ZFS_LOCAL;
					att.save();
					Log.d(TAG,"File downloaded, I think");
					mProgressDialog.dismiss();
				} catch (IOException e) {
					Log.e(TAG,"DownloadManager exception on: "+att.key,e);
				}
			}
			if (att.status.equals(Attachment.ZFS_LOCAL)) {
				Log.d(TAG,"Starting to display local attachment");

				Uri uri = Uri.fromFile(new File(att.filename));
				try {
					String mimeType = att.content.optString("mimeType",null);
					startActivity(new Intent(Intent.ACTION_VIEW)
								.setDataAndType(uri,mimeType));
				} catch (ActivityNotFoundException e) {
					Log.e(TAG, "No activity for intent", e);
					Toast.makeText(getApplicationContext(), "No application for files of this type", 
	        				Toast.LENGTH_SHORT).show();
				}
			}
			return null;
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
        	if (!ServerCredentials.check(getApplicationContext())) {
            	Toast.makeText(getApplicationContext(), "Log in to sync", 
        				Toast.LENGTH_SHORT).show();
            	return true;
        	}
        	Log.d(TAG, "Preparing sync requests, starting with present item");
        	new ZoteroAPITask(getBaseContext()).execute(APIRequest.update(this.item));
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
    

    public void download(URL url, File destination) {  
    	//this is the downloader method
        try {
                long startTime = System.currentTimeMillis();
                Log.d(TAG, "download begining");
                Log.d(TAG, "download url:" + url.toString());
                Log.d(TAG, "downloaded file name:" + destination.getPath());
                /* Open a connection to that URL. */
                URLConnection ucon = url.openConnection();

                /*
                 * Define InputStreams to read from the URLConnection.
                 */
                InputStream is = ucon.getInputStream();
                BufferedInputStream bis = new BufferedInputStream(is);

                /*
                 * Read bytes to the Buffer until there is nothing more to read(-1).
                 */
                ByteArrayBuffer baf = new ByteArrayBuffer(50);
                int current = 0;
                while ((current = bis.read()) != -1) {
                        baf.append((byte) current);
                }

                /* Convert the Bytes read to a String. */
                FileOutputStream fos = new FileOutputStream(destination);
                fos.write(baf.toByteArray());
                fos.close();
                Log.d(TAG, "download ready in"
                                + ((System.currentTimeMillis() - startTime) / 1000)
                                + " sec");

        } catch (IOException e) {
                Log.e(TAG, "Error: ",e);
        }

}
    
}
