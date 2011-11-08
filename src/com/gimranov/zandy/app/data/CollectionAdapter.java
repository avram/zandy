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
package com.gimranov.zandy.app.data;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.gimranov.zandy.app.R;
import com.gimranov.zandy.app.task.APIRequest;

/**
 * Exposes collection to be displayed by a ListView
 * @author ajlyon
 *
 */
public class CollectionAdapter extends ResourceCursorAdapter {
	public static final String TAG = "com.gimranov.zandy.app.data.CollectionAdapter";

	public Context context;
		
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
		TextView tvTitle = (TextView)view.findViewById(R.id.collection_title);
		TextView tvInfo = (TextView)view.findViewById(R.id.collection_info);
		
		Database db = new Database(context);
	
		ItemCollection collection = ItemCollection.load(cursor);
		tvTitle.setText(collection.getTitle());
		StringBuilder sb = new StringBuilder();
		sb.append(collection.getSize() + " items");
		sb.append("; " + collection.getSubcollections(db).size() + " subcollections");
		if(!collection.dirty.equals(APIRequest.API_CLEAN))
			sb.append("; "+collection.dirty);
		tvInfo.setText(sb.toString());
		db.close();
	}

}
