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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONException;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import android.widget.Toast;

import com.gimranov.zandy.app.data.Attachment;
import com.gimranov.zandy.app.data.Database;
import com.gimranov.zandy.app.data.Item;
import com.gimranov.zandy.app.task.APIRequest;
import com.gimranov.zandy.app.task.ZoteroAPITask;

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

	private static final String TAG = "com.gimranov.zandy.app.AttachmentActivity";
	
	static final int DIALOG_CONFIRM_NAVIGATE = 4;	
	static final int DIALOG_FILE_PROGRESS = 6;	
	static final int DIALOG_CONFIRM_DELETE = 5;	
	static final int DIALOG_NOTE = 3;
	static final int DIALOG_NEW = 1;
	
	public Item item;
	private ProgressDialog mProgressDialog;
	private Database db;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        db = new Database(this);
        
        /* Get the incoming data from the calling activity */
        final String itemKey = getIntent().getStringExtra("com.gimranov.zandy.app.itemKey");
        Item item = Item.load(itemKey, db);
        this.item = item;
        
        this.setTitle("Attachments for "+item.getTitle());
        
        ArrayList<Attachment> rows = Attachment.forItem(item, db);
        
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

        		ImageView tvType = (ImageView)row.findViewById(R.id.attachment_type);
        		TextView tvSummary = (TextView)row.findViewById(R.id.attachment_summary);
        		
        		Attachment att = getItem(position);
        		Log.d(TAG, "Have an attachment: "+att.title);
        		
        		tvType.setImageResource(Item.resourceForType(att.getType()));
        		
        		try {
					Log.d(TAG, att.content.toString(4));
				} catch (JSONException e) {
					Log.e(TAG, "JSON parse exception when reading attachment content", e);
				}
        		
        		if (att.getType().equals("note")) {
        			String note = att.content.optString("note","");
        			if (note.length() > 40) {
        				note = note.substring(0,40);
        			}
        			tvSummary.setText(note);
        		} else {
        			StringBuffer status = new StringBuffer(getResources().getString(R.string.status));
        			if (att.status == Attachment.ZFS_AVAILABLE)
        				status.append(getResources().getString(R.string.attachment_zfs_available));
        			else if (att.status == Attachment.ZFS_LOCAL)
        				status.append(getResources().getString(R.string.attachment_zfs_local));
        			else
        				status.append(getResources().getString(R.string.attachment_unknown));
        			tvSummary.setText(att.title + " " + status.toString());
        		}
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
        		String url = (row.url != null && !row.url.equals("")) ?
        				row.url : row.content.optString("url");
				if (!row.getType().equals("note")) {
					Bundle b = new Bundle();
        			b.putString("title", row.title);
        			b.putString("key", row.key);
        			b.putString("content", url);
        			// 0 means download from ZFS. 1 is everything else (?)
        			String linkMode = row.content.optString("linkMode","0");
        			if (linkMode.equals("0"))
        				showDialog(DIALOG_FILE_PROGRESS, b);
        			else
        				showDialog(DIALOG_CONFIRM_NAVIGATE, b);
				}
								
				if (row.getType().equals("note")) {
					Bundle b = new Bundle();
					b.putString("attachmentKey", row.key);
					b.putString("itemKey", itemKey);
					b.putString("content", row.content.optString("note", ""));
					showDialog(DIALOG_NOTE, b);
				}
        	}
        });
    }
    
    @Override
    public void onDestroy() {
    	if (db != null) db.close();
    	super.onDestroy();
    }
    
    @Override
    public void onResume() {
    	if (db == null) db = new Database(this);
    	super.onResume();
    }
    
	protected Dialog onCreateDialog(int id, Bundle b) {
		final String attachmentKey = b.getString("attachmentKey");
		final String itemKey = b.getString("itemKey");
		final String content = b.getString("content");
		final String mode = b.getString("mode");
		AlertDialog dialog;
		switch (id) {			
		case DIALOG_CONFIRM_NAVIGATE:
			dialog = new AlertDialog.Builder(this)
		    	    .setTitle(getResources().getString(R.string.view_online_warning))
		    	    .setPositiveButton(getResources().getString(R.string.view), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
		        			// The behavior for invalid URIs might be nasty, but
		        			// we'll cross that bridge if we come to it.
		        			Uri uri = Uri.parse(content);
		        			startActivity(new Intent(Intent.ACTION_VIEW)
		        							.setData(uri));
		    	        }
		    	    }).setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
		    	        public void onClick(DialogInterface dialog, int whichButton) {
		    	        	// do nothing
		    	        }
		    	    }).create();
			return dialog;
		case DIALOG_NOTE:
			final EditText input = new EditText(this);
			input.setText(content, BufferType.EDITABLE);
			
			dialog = new AlertDialog.Builder(this)
	    	    .setTitle(getResources().getString(R.string.note))
	    	    .setView(input)
	    	    .setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
					@SuppressWarnings("unchecked")
					public void onClick(DialogInterface dialog, int whichButton) {
	    	            Editable value = input.getText();
						if (mode != null && mode.equals("new")) {
							Log.d(TAG, "Attachment created with parent key: "+itemKey);
							Attachment att = new Attachment(getBaseContext(), "note", itemKey);
		    	            att.setNoteText(value.toString());
		    	            att.save(db);
						} else {
							Attachment att = Attachment.load(attachmentKey, db);
		    	            att.setNoteText(value.toString());
		    	            att.dirty = APIRequest.API_DIRTY;
		    	            att.save(db);
						}
	    	            ArrayAdapter<Attachment> la = (ArrayAdapter<Attachment>) getListAdapter();
	    	            la.clear();
	    	            for (Attachment a : Attachment.forItem(Item.load(itemKey, db), db)) {
	    	            	la.add(a);
	    	            }
	    	            la.notifyDataSetChanged();
	    	        }
	    	    }).setNegativeButton(getResources().getString(R.string.cancel),
	    	    		new DialogInterface.OnClickListener() {
	    	        public void onClick(DialogInterface dialog, int whichButton) {
	    	        	// do nothing
	    	        }
	    	    }).create();
			return dialog;
		case DIALOG_FILE_PROGRESS:
			Attachment att = Attachment.load(b.getString("key"), db);
			
			if (!ServerCredentials.sBaseStorageDir.exists())
				ServerCredentials.sBaseStorageDir.mkdir();
			if (!ServerCredentials.sDocumentStorageDir.exists())
				ServerCredentials.sDocumentStorageDir.mkdir();

			/* Prepare target filename and file */
			String sanitized = att.title.replace(' ', '_')
					.replaceFirst("^(.*?)(\\.?[^.]*)$", "$1"+"_"+att.key+"$2");
			File file = new File(ServerCredentials.sDocumentStorageDir,sanitized);
			
			File attFile = new File(att.filename);
			
			if (att.status == Attachment.ZFS_AVAILABLE
					// Zero-length or nonexistent gives length == 0
					|| (attFile != null && attFile.length() == 0)) {
				
				SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
				
				Log.d(TAG,"Starting to try and download ZFS-available attachment");
				
				mProgressDialog = new ProgressDialog(this);

				Toast.makeText(getApplicationContext(),
						getResources().getString(R.string.attachment_downloading, b.getString("title")), 
        				Toast.LENGTH_SHORT).show();	
				mProgressDialog.setMessage(getResources().getString(R.string.attachment_downloading, b.getString("title")));
				mProgressDialog.setIndeterminate(true);
				mProgressDialog.setMax(100);
				mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				mProgressDialog.show();
				Toast.makeText(getApplicationContext(), getResources().getString(R.string.attachment_download_initiated), 
        				Toast.LENGTH_SHORT).show();	
				try {
					if (!ServerCredentials.sBaseStorageDir.exists())
						ServerCredentials.sBaseStorageDir.mkdir();
					if (!ServerCredentials.sDocumentStorageDir.exists())
						ServerCredentials.sDocumentStorageDir.mkdir();
					
					download(new URL(att.url+"?key="+settings.getString("user_key","")),
							file);
					att.filename = file.getPath();
					if (file.exists() && file.length() > 0) {
						att.status = Attachment.ZFS_LOCAL;
						Log.d(TAG,"File downloaded");
					} else {
						att.status = Attachment.ZFS_AVAILABLE;
						Toast.makeText(getApplicationContext(), getResources().getString(R.string.attachment_download_failed), 
		        				Toast.LENGTH_SHORT).show();						
					}
					att.save(db);
					mProgressDialog.dismiss();
				} catch (IOException e) {
					Log.e(TAG,"DownloadManager exception on: "+att.key,e);
				}
			}
			if (att.status == Attachment.ZFS_LOCAL) {
				Log.d(TAG,"Starting to display local attachment");
				Uri uri = Uri.fromFile(new File(att.filename));
				String mimeType = att.content.optString("mimeType",null);
				try {
					startActivity(new Intent(Intent.ACTION_VIEW)
								.setDataAndType(uri,mimeType));
				} catch (ActivityNotFoundException e) {
					Log.e(TAG, "No activity for intent", e);
					Toast.makeText(getApplicationContext(),
							getResources().getString(R.string.attachment_intent_failed, mimeType), 
	        				Toast.LENGTH_SHORT).show();
				}
			}
			return null;
		default:
			Log.e(TAG, "Invalid dialog requested");
			return null;
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
        case R.id.do_sync:
        	if (!ServerCredentials.check(getApplicationContext())) {
            	Toast.makeText(getApplicationContext(), getResources().getString(R.string.sync_log_in_first), 
        				Toast.LENGTH_SHORT).show();
            	return true;
        	}
        	Log.d(TAG, "Preparing sync requests, starting with present item");
        	new ZoteroAPITask(getBaseContext()).execute(APIRequest.update(this.item));
        	Toast.makeText(getApplicationContext(), getResources().getString(R.string.sync_started), 
    				Toast.LENGTH_SHORT).show();
        	
        	return true;
        case R.id.do_new:
			Bundle b = new Bundle();
			b.putString("itemKey", this.item.getKey());
			b.putString("mode", "new");
        	removeDialog(DIALOG_NOTE);
        	showDialog(DIALOG_NOTE, b);
            return true;
        case R.id.do_prefs:
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    

    /**
     * Saves a file from specified URL to specified destination.
     * TODO This should be made asynchronous, and spun out into its own routine
     * @param url
     * @param destination
     */
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
                BufferedInputStream bis = new BufferedInputStream(is, 16000);

                /*
                 * Read bytes to the Buffer until there is nothing more to read(-1).
                 */
                ByteArrayBuffer baf = new ByteArrayBuffer(50);
                int current = 0;
                while ((current = bis.read()) != -1) {
                	//Log.d(TAG, "Some more input...");
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
