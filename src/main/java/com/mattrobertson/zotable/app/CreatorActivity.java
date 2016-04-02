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

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import com.getbase.floatingactionbutton.FloatingActionButton;
import com.mattrobertson.zotable.app.data.Creator;
import com.mattrobertson.zotable.app.data.Database;
import com.mattrobertson.zotable.app.data.Item;
import com.mattrobertson.zotable.app.data.ItemCollection;

public class CreatorActivity extends Activity {

	private static final String TAG = "CreatorActivity";

	static final int DIALOG_CREATOR = 3;

	ListView lvCreators;
	CreatorAdapter adapter;

	public Item item;

	private Database db;

	/**
	 * For API <= 7, to pass bundles to activities
	 */
	private Bundle b = new Bundle();

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.creator_activity);

		db = new Database(this);

		lvCreators = (ListView) findViewById(R.id.lvCreators);
        
        /* Get the incoming data from the calling activity */
		String itemKey = getIntent().getStringExtra("com.mattrobertson.zotable.app.itemKey");
		Item item = Item.load(itemKey, db);
		this.item = item;

		this.setTitle("Creators for " + item.getTitle());

		ArrayList<Bundle> rows = item.creatorsToBundleArray();
		adapter = new CreatorAdapter(rows);
        lvCreators.setAdapter(adapter);

		lvCreators.setTextFilterEnabled(true);
		lvCreators.setOnItemClickListener(new OnItemClickListener() {
			// Warning here because Eclipse can't tell whether my ArrayAdapter is
			// being used with the correct parametrization.
			@SuppressWarnings("unchecked")
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// If we have a click on an entry, do something...
				CreatorAdapter adapter = (CreatorAdapter) parent.getAdapter();
				Bundle row = (Bundle) (adapter.getItem(position));

				CreatorActivity.this.b = row;
				removeDialog(DIALOG_CREATOR);
				showDialog(DIALOG_CREATOR);
			}
		});

		FloatingActionButton fab = (FloatingActionButton)findViewById(R.id.btnFab);

		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				//Toast.makeText(CreatorActivity.this,"Adding",Toast.LENGTH_SHORT).show();

				// Set local Bundle b to null to show we are adding a new Creator
				CreatorActivity.this.b = null;
				removeDialog(DIALOG_CREATOR);
				showDialog(DIALOG_CREATOR);
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

	public void refreshData() {
		ArrayList<Bundle> rows = item.creatorsToBundleArray();
		adapter = new CreatorAdapter(rows);
		lvCreators.setAdapter(adapter);

		adapter.notifyDataSetChanged();
		lvCreators.invalidate();
	}

	protected Dialog onCreateDialog(int id) {

		final String creatorType;
		final int creatorPosition;

		String name, firstName, lastName;

		if (b != null) {
			creatorType = b.getString("creatorType");
			creatorPosition = b.getInt("position");
			name = b.getString("name");
			firstName = b.getString("firstName");
			lastName = b.getString("lastName");
		}
		else {
			creatorType = "";
			creatorPosition = -1;
			firstName = "First";
			lastName = "Last";
			name = firstName + " " + lastName;
		}

		switch (id) {
		/* Editor for a creator
		 */
			case DIALOG_CREATOR:
				AlertDialog.Builder builder;
				AlertDialog dialog;

				LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
				final View layout = inflater.inflate(R.layout.creator_dialog, (ViewGroup) findViewById(R.id.layout_root));

				TextView textName = (TextView) layout.findViewById(R.id.creator_name);
				TextView textFN = (TextView) layout.findViewById(R.id.creator_firstName);
				TextView textLN = (TextView) layout.findViewById(R.id.creator_lastName);

				textName.setText(name);
				textFN.setText(firstName);
				textLN.setText(lastName);

				CheckBox mode = (CheckBox) layout.findViewById(R.id.creator_mode);
				mode.setChecked((firstName == null || firstName.equals(""))
						&& (lastName == null || lastName.equals(""))
						&& (lastName != null && !name.equals("")));

				// Set up the adapter to get creator types
				String[] types = Item.localizedCreatorTypesForItemType(item.getType());

				// what position are we?
				int arrPosition = 0;
				String localType = "";

				if (creatorType != null) {
					localType = Item.localizedStringForString(creatorType);
				}
				else { // We default to the first possibility when none specified
					localType = Item.localizedStringForString(Item.creatorTypesForItemType(item.getType())[0]);
				}

				// Set spinner to display item type
				for (int i = 0; i < types.length; i++) {
					if (types[i].equals(localType)) {
						arrPosition = i;
						break;
					}
				}

				ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, types);
				adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

				Spinner spinner = (Spinner) layout.findViewById(R.id.creator_type);
				spinner.setAdapter(adapter);

				spinner.setSelection(arrPosition);
				builder = new AlertDialog.Builder(this);
				builder.setView(layout);
				builder.setPositiveButton(getResources().getString(R.string.ok), new OnClickListener() {
					@SuppressWarnings("unchecked")
					public void onClick(DialogInterface dialog, int whichButton) {
						Creator c;
						TextView textName = (TextView) layout.findViewById(R.id.creator_name);
						TextView textFN = (TextView) layout.findViewById(R.id.creator_firstName);
						TextView textLN = (TextView) layout.findViewById(R.id.creator_lastName);
						Spinner spinner = (Spinner) layout.findViewById(R.id.creator_type);
						CheckBox mode = (CheckBox) layout.findViewById(R.id.creator_mode);

						String selected = (String) spinner.getSelectedItem();
						// Set up the adapter to get creator types
						String[] types = Item.localizedCreatorTypesForItemType(item.getType());

						// what position are we?
						int typePos = 0;
						for (int i = 0; i < types.length; i++) {
							if (types[i].equals(selected)) {
								typePos = i;
								break;
							}
						}

						String realType = Item.creatorTypesForItemType(item.getType())[typePos];

						if (mode.isChecked())
							c = new Creator(realType, textName.getText().toString(), true);
						else
							c = new Creator(realType, textFN.getText().toString(), textLN.getText().toString());

						Item.setCreator(item.getKey(), c, creatorPosition, db);
						item = Item.load(item.getKey(), db);

						refreshData();
					}
				});

				builder.setNeutralButton(getResources().getString(R.string.cancel), new OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// do nothing
					}
				});

				builder.setNegativeButton(getResources().getString(R.string.menu_delete), new OnClickListener() {
					@SuppressWarnings("unchecked")
					public void onClick(DialogInterface dialog, int whichButton) {
						Item.setCreator(item.getKey(), null, creatorPosition, db);
						item = Item.load(item.getKey(), db);

						refreshData();
					}
				});

				dialog = builder.create();

				WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
				lp.copyFrom(dialog.getWindow().getAttributes());
				lp.width = WindowManager.LayoutParams.MATCH_PARENT;
				lp.height = WindowManager.LayoutParams.MATCH_PARENT;

				dialog.getWindow().setAttributes(lp);

				return dialog;

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
			case R.id.do_prefs:
				startActivity(new Intent(this, SettingsActivity.class));
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	class CreatorAdapter extends BaseAdapter {
		private ArrayList<Bundle> mList;

		public CreatorAdapter(ArrayList<Bundle> list) {
			mList = list;
		}

		public void setData(ArrayList<Bundle> newList) {
			mList = newList;
		}

		@Override
		public int getCount() {
			return mList.size();
		}

		@Override
		public Object getItem(int pos) {
			return mList.get(pos);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row;

			// We are reusing views, but we need to initialize it if null
			if (null == convertView) {
				LayoutInflater inflater = getLayoutInflater();
				row = inflater.inflate(R.layout.list_data, null);
			} else {
				row = convertView;
			}

        	TextView tvLabel = (TextView) row.findViewById(R.id.data_label);
			TextView tvContent = (TextView) row.findViewById(R.id.data_content);
            ImageView imgAction = (ImageView)row.findViewById(R.id.imgAction);

			tvLabel.setText(Item.localizedStringForString(((Bundle)getItem(position)).getString("creatorType")));
			tvContent.setText(((Bundle)getItem(position)).getString("name"));
            imgAction.setImageResource(R.drawable.ic_edit);
            imgAction.setVisibility(View.VISIBLE);

			return row;
		}
	}
}