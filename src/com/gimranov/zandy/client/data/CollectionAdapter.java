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
package com.gimranov.zandy.client.data;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.gimranov.zandy.client.R;
import com.gimranov.zandy.client.task.APIRequest;

/**
 * Exposes collection to be displayed by a ListView
 * @author ajlyon
 *
 */
public class CollectionAdapter extends ResourceCursorAdapter {
	private static final String TAG = "com.gimranov.zandy.client.data.CollectionAdapter";

	private Database db;
	public Context context;
	private ItemCollection parent;
		
	public CollectionAdapter(Context context, Cursor cursor) {
		super(context, R.layout.list_collection, cursor, false);
		this.context = context;
	}
	
    public View newView(Context context, Cursor cur, ViewGroup parent) {
        LayoutInflater li = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return li.inflate(R.layout.list_collection, parent, false);
    }

    /**
     * Call this when the data has been updated-- it refreshes the cursor and notifies of the change
     */
    public void notifyDataSetChanged() {
    	super.notifyDataSetChanged();
    }
    
	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		//Log.d(TAG, "bindView view is: " + view.getId());
		TextView tvTitle = (TextView)view.findViewById(R.id.collection_title);
		TextView tvInfo = (TextView)view.findViewById(R.id.collection_info);
	
		if (cursor == null) {
			Log.e(TAG, "cursor is null in bindView");
		}
		ItemCollection collection = ItemCollection.load(cursor);
		if (collection == null) {
			Log.e(TAG, "collection is null in bindView");
		}
		if (tvTitle == null) {
			Log.e(TAG, "tvTitle is null in bindView");
		}
		tvTitle.setText(collection.getTitle());
		StringBuilder sb = new StringBuilder();
		sb.append(collection.getSize() + " items");
		sb.append("; " + collection.getSubcollections().size() + " subcollections");
		if(!collection.dirty.equals(APIRequest.API_CLEAN))
			sb.append("; "+collection.dirty);
		tvInfo.setText(sb.toString());
	}

	/**
	 * Requeries the database for top-level collections
	 */
	public void refresh() {
		if (this.getCursor() != null)
			this.getCursor().close();
		String[] args = { "false" };			
		Cursor cursor = db.query("collections", Database.COLLCOLS, "collection_parent=?", args, null, null, "collection_name", null);
		if (cursor == null) {
			Log.e(TAG, "cursor is null");
		}
		this.changeCursor(cursor);
	}
	
	/**
	 * Requeries the database for the specified collection
	 */
	public void refresh(ItemCollection parent) {
		this.parent = parent;
		if (this.getCursor() != null)
			this.getCursor().close();
		String[] args = { parent.getKey() };
		Cursor cursor = db.query("collections", Database.COLLCOLS, "collection_parent=?", args, null, null, "collection_name", null);
		if (cursor == null) {
			Log.e(TAG, "cursor is null");
		}
		this.changeCursor(cursor);
	}
	
	/**
	 * Requery/refresh one level up
	 */
	public void goUp() {
		if (this.parent == null) {
			// do nothing
		} else {
			ItemCollection grandparent = this.parent.getParent();
			if (grandparent == null) {
				this.parent = null;
				refresh();
			} else {
				refresh(grandparent);
			}
		}
	}
	
	/**
	 * Gives an adapter for top-level collections
	 * @param context
	 * @return
	 */
	public static CollectionAdapter create(Context context) {
		Database db = new Database(context);
		String[] args = { "false" };
		Cursor cursor = db.query("collections", Database.COLLCOLS, "collection_parent=?", args, null, null, "collection_name", null);
		if (cursor == null) {
			Log.e(TAG, "cursor is null");
		}
		Log.e(TAG, "created collectionadapter");
		CollectionAdapter adapter = new CollectionAdapter(context, cursor);
		adapter.db = db;
		return adapter;
	}

	/**
	 * Gives an adapter for child collections of a given parent
	 * @param context
	 * @param parent
	 * @return
	 */
	public static CollectionAdapter create(Context context, ItemCollection parent) {
		Database db = new Database(context);
		String[] args = { parent.getKey() };
		Cursor cursor = db.query("collections", Database.COLLCOLS, "collection_parent=?", args, null, null, "collection_name", null);
		if (cursor == null) {
			Log.e(TAG, "cursor is null");
		}
		Log.e(TAG, "created collectionadapter for child");
		CollectionAdapter adapter = new CollectionAdapter(context, cursor);
		adapter.parent = parent;
		adapter.db = db;
		return adapter;
	}

}
