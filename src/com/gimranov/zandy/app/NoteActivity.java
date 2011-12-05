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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView.BufferType;

import com.gimranov.zandy.app.data.Attachment;
import com.gimranov.zandy.app.data.Database;
import com.gimranov.zandy.app.data.Item;
import com.gimranov.zandy.app.task.APIRequest;

/**
 * This Activity handles displaying and editing of notes.
 * 
 * @author mlt
 *
 */
public class NoteActivity extends Activity {

	private static final String TAG = "com.gimranov.zandy.app.NoteActivity";
	
	static final int DIALOG_CONFIRM_NAVIGATE = 4;	
	static final int DIALOG_FILE_PROGRESS = 6;	
	static final int DIALOG_CONFIRM_DELETE = 5;	
	static final int DIALOG_NOTE = 3;
	static final int DIALOG_NEW = 1;
	
	public Attachment att;
	private Database db;
    private ZWebView mWebView;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setDefaultKeyMode(DEFAULT_KEYS_DISABLE);
        setContentView(R.layout.note);

        db = new Database(this);
        
        /* Get the incoming data from the calling activity */
        final String attKey = getIntent().getStringExtra("com.gimranov.zandy.app.attKey");
        final Attachment att = Attachment.load(attKey, db);
        
        if (att == null) {
        	Log.e(TAG, "NoteActivity started without attKey; finishing.");
        	finish();
        	return;
        }
        
        Item item = Item.load(att.parentKey, db);
        this.att = att;
        
        // Set up buttons
        Button cancelButton = (Button) findViewById(R.id.noteCancel);
		cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				finish();
			}
		});
        Button editButton = (Button) findViewById(R.id.noteEdit);
		editButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Bundle b = new Bundle();
				b.putString("attachmentKey", att.key);
				b.putString("itemKey", att.parentKey);
				b.putString("content", att.content.optString("note", ""));
				removeDialog(DIALOG_NOTE);
				showDialog(DIALOG_NOTE, b);
			}
		});
        
        setTitle(getResources().getString(R.string.note_for_item, item.getTitle()));
        //file:///android_assets/tinymce/
        mWebView = (ZWebView) findViewById(R.id.webview);
        mWebView.getSettings().setJavaScriptEnabled(false);
		mWebView.loadUrl(dataUrlForNote(att.content.optString("note", "")));
    }
    
    /**
     * Returns urlencoded  data: URL for Unicode note string
     * @param note
     * @return
     */
    private static String dataUrlForNote(String note) {
    	String data =
    	        "<html xmlns=\"http://www.w3.org/1999/xhtml\">" +
    	"<meta http-equiv='Content-Type' content='text/html; charset=utf-8'>"+
    	        "<head>" +
    	        "</script>" +
    	        "</head>" +
    	    	"<body>" +
    	        note +
    	        "</body>" +
    	        "</html>";
    	String b64 = new String(Base64.encode(data.getBytes(), Base64.DEFAULT));
		return "data:text/html;base64,"+b64;
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
        	finish();
        	return true;
        }
       return super.onKeyDown(keyCode, event);
    }
    
    protected Dialog onCreateDialog(int id, Bundle b) {
		final String attachmentKey = b.getString("attachmentKey");
		final String itemKey = b.getString("itemKey");
		final String content = b.getString("content");
		final String mode = b.getString("mode");
		AlertDialog dialog;
		switch (id) {			
		case DIALOG_NOTE:
			final EditText input = new EditText(this);
			input.setText(content, BufferType.EDITABLE);
			
			AlertDialog.Builder builder = new AlertDialog.Builder(this)
	    	    .setTitle(getResources().getString(R.string.note))
	    	    .setView(input)
	    	    .setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
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
						mWebView.loadUrl(dataUrlForNote(att.content.optString("note", "")));
					}
	    	    }).setNeutralButton(getResources().getString(R.string.cancel),
	    	    		new DialogInterface.OnClickListener() {
	    	        public void onClick(DialogInterface dialog, int whichButton) {
	    	        	// do nothing
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
