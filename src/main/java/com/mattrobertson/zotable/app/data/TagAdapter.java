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
package com.mattrobertson.zotable.app.data;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.mattrobertson.zotable.app.R;
import com.mattrobertson.zotable.app.task.APIRequest;

/**
 * Exposes collection to be displayed by a ListView
 * @author ajlyon
 *
 */
public class TagAdapter extends ResourceCursorAdapter {
	public static final String TAG = "TagAdapter";

	public Context context;

	public TagAdapter(Context context, Cursor cursor) {
		super(context, R.layout.list_tag, cursor, false);
		this.context = context;
	}
	
    public View newView(Context context, Cursor cur, ViewGroup parent) {
        LayoutInflater li = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return li.inflate(R.layout.list_tag, parent, false);
    }

    /**
     * Call this when the data has been updated-- it refreshes the cursor and notifies of the change
     */
    public void notifyDataSetChanged() {
    	super.notifyDataSetChanged();
    }
    
	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		TextView tvTag = (TextView)view.findViewById(R.id.tag_name);
		String tagName = cursor.getString(2);
		tvTag.setText(tagName);

		Database db = new Database(context);

		String[] args = {tagName};
		Cursor c = db.rawQuery("SELECT COUNT(*) FROM tags WHERE tag=?", args);
		int itemCount = c.getInt(0);

		TextView tvItemCount = (TextView)view.findViewById(R.id.item_count);
		tvItemCount.setText("Items: " + itemCount);

		db.close();
	}

}
