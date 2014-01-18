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
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import android.widget.Toast;

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
	
	static final int DIALOG_NOTE = 3;
	
	public Attachment att;
	private Database db;
		
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        
        setTitle(getResources().getString(R.string.note_for_item, item.getTitle()));

        TextView text = (TextView) findViewById(R.id.noteText);
        TextView title = (TextView) findViewById(R.id.noteTitle);
        title.setText(att.title);
        text.setText(Html.fromHtml(att.content.optString("note", "")));
        
        Button editButton = (Button) findViewById(R.id.editNote);
		editButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				showDialog(DIALOG_NOTE);
			}
		});
		
        /* Warn that this won't propagate for attachment notes */
        if (!"note".equals(att.getType())) {
			Toast.makeText(this, R.string.attachment_note_warning, Toast.LENGTH_LONG).show();
        }
    } 
    
    protected Dialog onCreateDialog(int id) {
		AlertDialog dialog;
		switch (id) {
		case DIALOG_NOTE:
			final EditText input = new EditText(this);
			input.setText(att.content.optString("note", ""), BufferType.EDITABLE);
			
			AlertDialog.Builder builder = new AlertDialog.Builder(this)
	    	    .setTitle(getResources().getString(R.string.note))
	    	    .setView(input)
	    	    .setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
	    	            Editable value = input.getText();
	    	            String fixed = value.toString().replaceAll("\n\n", "\n<br>");
	    	            att.setNoteText(fixed);
	    	            att.dirty = APIRequest.API_DIRTY;
	    	            att.save(db);
	    	            
				        TextView text = (TextView) findViewById(R.id.noteText);
				        TextView title = (TextView) findViewById(R.id.noteTitle);
				        title.setText(att.title);
				        text.setText(Html.fromHtml(att.content.optString("note", "")));
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
