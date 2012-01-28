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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.gimranov.zandy.app.R;

/**
 * Exposes items to be displayed by a ListView
 * @author ajlyon
 *
 */
public class ItemAdapter extends ResourceCursorAdapter {
	private static final String TAG = "com.gimranov.zandy.app.data.ItemAdapter";
	
	public ItemAdapter(Context context, Cursor cursor) {
		super(context, R.layout.list_item, cursor, false);
	}
	
    public View newView(Context context, Cursor cur, ViewGroup parent) {
        LayoutInflater li = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return li.inflate(R.layout.list_item, parent, false);
    }
    
    /**
     * Call this when the data has been updated-- it refreshes the cursor and notifies of the change
     */
    public void notifyDataSetChanged() {
    	super.notifyDataSetChanged();
    }
    
	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		TextView tvTitle = (TextView)view.findViewById(R.id.item_title);
		ImageView tvType = (ImageView)view.findViewById(R.id.item_type);
		TextView tvSummary = (TextView)view.findViewById(R.id.item_summary);
	
		if (cursor == null) {
			Log.e(TAG, "cursor is null in bindView");
		}
		Item item = Item.load(cursor);
		
		if (item == null) {
			Log.e(TAG, "item is null in bindView");
		}
		if (tvTitle == null) {
			Log.e(TAG, "tvTitle is null in bindView");
		}
				
		Log.d(TAG, "setting image for item (" + item.getKey() + ") of type: "+item.getType());
		tvType.setImageResource(Item.resourceForType(item.getType()));

		tvSummary.setText(item.getCreatorSummary() + " (" + item.getYear() + ")");
		if (tvSummary.getText().equals(" ()")) tvSummary.setVisibility(View.GONE);
		
		tvTitle.setText(item.getTitle());
		
	}
}
