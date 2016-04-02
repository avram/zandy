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
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
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
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.BufferType;


import com.getbase.floatingactionbutton.FloatingActionButton;
import com.mattrobertson.zotable.app.data.Database;
import com.mattrobertson.zotable.app.data.Item;
import com.mattrobertson.zotable.app.data.ItemCollection;

/**
 * This Activity handles displaying and editing tags. It works almost the same as
 * ItemDataActivity, using a simple ArrayAdapter on Bundles with the tag info.
 * 
 * @author ajlyon
 *
 */
public class TagActivity extends Activity {

	private static final String TAG = "TagActivity";
	
	static final int DIALOG_TAG = 3;
	static final int DIALOG_CONFIRM_NAVIGATE = 4;	
	
	private Item item;

    ListView lvTags;
    TagAdapter adapter;
    ArrayList<Bundle> rows;

    String itemKey = "";

	private Database db;

	protected Bundle b = new Bundle();
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.tag_activity);

        db = new Database(this);
                
        /* Get the incoming data from the calling activity */
        itemKey = getIntent().getStringExtra("com.mattrobertson.zotable.app.itemKey");
        Item item = Item.load(itemKey, db);
        this.item = item;
        
        this.setTitle(getResources().getString(R.string.tags_for_item, item.getTitle()));

        lvTags = (ListView)findViewById(R.id.lvTags);

        rows = item.tagsToBundleArray();

        adapter = new TagAdapter(rows);
        lvTags.setAdapter(adapter);

        lvTags.setTextFilterEnabled(true);
        lvTags.setOnItemClickListener(new OnItemClickListener() {
            @SuppressWarnings("unchecked")
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            }
        });

        FloatingActionButton fab = (FloatingActionButton)findViewById(R.id.btnFab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                AlertDialog.Builder builder = new AlertDialog.Builder(TagActivity.this);
                builder.setTitle("New tag");

                final EditText etTagName = new EditText(TagActivity.this);
                etTagName.setTextColor(getResources().getColor(R.color.white));

                builder.setView(etTagName);
                builder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @SuppressWarnings("unchecked")
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Item item = Item.load(itemKey, db);
                        item.addTag(etTagName.getText().toString());
                        item.save(db);
                        refreshData();
                    }
                });

                builder.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @SuppressWarnings("unchecked")
                    public void onClick(DialogInterface dialog, int whichButton) {

                    }
                });


                final Dialog dialog = builder.create();

                dialog.show();
            }
        });
    }
    
    @Override
    public void onDestroy() {
    	if (db != null)
            db.close();

    	super.onDestroy();
    }
    
    @Override
    public void onResume() {
    	if (db == null)
            db = new Database(this);

    	super.onResume();
    }

    public void refreshData() {
        item = Item.load(itemKey, db);
        rows = item.tagsToBundleArray();

        adapter.setData(rows);
        adapter.notifyDataSetChanged();

        lvTags.invalidate();
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

    class TagAdapter extends BaseAdapter {
        private ArrayList<Bundle> mList;

        public TagAdapter(ArrayList<Bundle> list) {
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

        		/* Our layout has just two fields */
            TextView tvLabel = (TextView) row.findViewById(R.id.data_label);
            final TextView tvContent = (TextView) row.findViewById(R.id.data_content);

            if (((Bundle)getItem(position)).getInt("type") == 1)
                tvLabel.setText(getResources().getString(R.string.tag_auto));
            else
                tvLabel.setText(getResources().getString(R.string.tag_user));
            tvContent.setText(((Bundle) getItem(position)).getString("tag"));

            ImageView imgRemove = (ImageView) row.findViewById(R.id.imgAction);
            imgRemove.setImageResource(R.drawable.ic_delete);
            imgRemove.setVisibility(View.VISIBLE);

            imgRemove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    TagActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            item.removeTag(tvContent.getText().toString());
                            item.save(db);
                            refreshData();
                        }
                    });
                }
            });

            return row;
        }
    }
}
