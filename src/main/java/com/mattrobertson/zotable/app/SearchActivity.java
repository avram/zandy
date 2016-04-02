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

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.mattrobertson.zotable.app.data.Database;
import com.mattrobertson.zotable.app.data.Item;
import com.mattrobertson.zotable.app.data.ItemAdapter;

public class SearchActivity extends ListActivity {

    private static final String TAG = "SearchActivity";

    private Database db;

     @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

         getActionBar().setDisplayHomeAsUpEnabled(true);

        db = new Database(this);

        prepareAdapter();

        final ListView lv = getListView();
        lv.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ItemAdapter adapter = (ItemAdapter) parent.getAdapter();
                Cursor cur = adapter.getCursor();

                if (cur.moveToPosition(position)) {

                    Item item = Item.load(cur);

                    // We create and issue a specified intent with the necessary data
                    Intent i = new Intent(getBaseContext(), ItemDataActivity.class);

                    if (item != null) {
                        Log.d(TAG, "Loading item data with key: " + item.getKey());
                        i.putExtra("com.mattrobertson.zotable.app.itemKey", item.getKey());
                        i.putExtra("com.mattrobertson.zotable.app.itemDbId", item.dbId);
                    }

                    startActivity(i);
                } else {
                    // failed to move cursor-- show a toast
                    TextView tvTitle = (TextView) view.findViewById(R.id.item_title);
                    Toast.makeText(getApplicationContext(),getResources().getString(R.string.cant_open_item, tvTitle.getText()),Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Application.getInstance().getBus().register(this);
    }

    @Override
    public void onDestroy() {
        ItemAdapter adapter = (ItemAdapter) getListAdapter();
        Cursor cur = adapter.getCursor();
        if (cur != null) cur.close();
        if (db != null) db.close();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Application.getInstance().getBus().unregister(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            onBackPressed();

        return super.onOptionsItemSelected(item);
    }

    private void prepareAdapter() {
        ItemAdapter adapter = new ItemAdapter(this, prepareCursor());
        setListAdapter(adapter);
    }

    private Cursor prepareCursor() {
        String query = getIntent().getStringExtra("query");

        setTitle(getResources().getString(R.string.search_results, query));

        return getCursor(query);
    }

    public Cursor getCursor(String query) {
        String qLike = "%" + query + "%";

        String[] args = {qLike,qLike,qLike};
        Cursor cursor = db.rawQuery("SELECT item_title, item_type, item_content, etag, dirty, " +
                "_id, item_key, item_year, item_creator, timestamp, item_children " +
                " FROM items WHERE item_title LIKE ? OR item_creator LIKE ? OR item_content LIKE ?" +
                " ORDER BY item_title",
                args);

        if (cursor == null) {
            Log.e(TAG, "cursor is null");
        }

        return cursor;
    }
}