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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.gimranov.zandy.app.data.Database;

public class SettingsActivity extends PreferenceActivity implements OnClickListener {
	
	private static final String TAG = "com.gimranov.zandy.app.SettingsActivity";

	static final int DIALOG_CONFIRM_DELETE = 5;

     @Override
     public void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);  

         addPreferencesFromResource(R.xml.settings);
         setContentView(R.layout.preferences);
         
         Button requestButton = (Button) findViewById(R.id.requestQueue);
 		 requestButton.setOnClickListener(this);

         Button resetButton = (Button) findViewById(R.id.resetDatabase);
 		 resetButton.setOnClickListener(this);
     }
     
	public void onClick(View v) {
		if (v.getId() == R.id.requestQueue) {
			Intent i = new Intent(getApplicationContext(), RequestActivity.class);
			startActivity(i);
		} else if (v.getId() == R.id.resetDatabase) {
			showDialog(DIALOG_CONFIRM_DELETE);
		}
	}
	
	protected Dialog onCreateDialog(int id) {
		AlertDialog dialog;
		
		switch (id) {
		case DIALOG_CONFIRM_DELETE:
			dialog = new AlertDialog.Builder(this)
			// TODO i18n
		    	    .setTitle(getResources().getString(R.string.settings_reset_database_warning))
		    	    .setPositiveButton(getResources().getString(R.string.menu_delete), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							Database db = new Database(getBaseContext());
							db.resetAllData();
							finish();
		    	        }
		    	    }).setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
		    	        public void onClick(DialogInterface dialog, int whichButton) {
		    	        	// do nothing
		    	        }
		    	    }).create();
			return dialog;
		default:
			Log.e(TAG, "Invalid dialog requested");
			return null;
		}
	}
}
