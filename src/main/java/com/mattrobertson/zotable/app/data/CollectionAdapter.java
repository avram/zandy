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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.Image;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.ToggleButton;


import com.mattrobertson.zotable.app.CollectionActivity;
import com.mattrobertson.zotable.app.R;
import com.mattrobertson.zotable.app.task.APIRequest;

/**
 * Exposes collection to be displayed by a ListView
 * @author ajlyon
 *
 */
public class CollectionAdapter extends ResourceCursorAdapter {
	public static final String TAG = "CollectionAdapter";

	public Context context;
	LayoutInflater mInflater;

	Cursor cursor;

	public CollectionAdapter(Context context, Cursor cursor) {
		super(context, R.layout.list_collection, cursor, false);
		this.context = context;
		this.cursor = cursor;
		mInflater = ((Activity)context).getLayoutInflater();
	}

	/**
	 * Call this when the data has been updated-- it refreshes the cursor and notifies of the change
	 */
	public void notifyDataSetChanged() {
		super.notifyDataSetChanged();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		cursor = (Cursor)getItem(position);
		cursor.moveToPosition(position);

		final ViewHolder holder;

		if ( convertView == null )
		{
        	// no view at this position - create a new one
			convertView = mInflater.inflate(R.layout.list_collection, null);
			holder = new ViewHolder();

			holder.tvTitle = (TextView)convertView.findViewById(R.id.collection_title);
			holder.tvInfo = (TextView)convertView.findViewById(R.id.collection_info);
			holder.imgFav = (ImageView)convertView.findViewById(R.id.imgFavorite);
			holder.imgExpand = (ImageView)convertView.findViewById(R.id.imgExpand);

			convertView.setTag (holder);
		}
		else
		{
        // recycle a View that already exists
			holder = (ViewHolder) convertView.getTag ();
		}

		final Database db = new Database(context);

		final ItemCollection collection = ItemCollection.load(cursor);

		if (collection.isFavorite())
			holder.imgFav.setImageResource(R.drawable.ic_star_filled);
		else
			holder.imgFav.setImageResource(R.drawable.ic_star_empty);

		// # of subcollections - used to display # & to determine expansion
		int subSize = collection.getSubcollections(db).size();

		holder.tvTitle.setText(collection.getTitle());

		StringBuilder sb = new StringBuilder();
		sb.append(collection.getSize() + " items");
		sb.append("; " + subSize + " subcollections");

		if(!collection.dirty.equals(APIRequest.API_CLEAN))
			sb.append("; "+collection.dirty);

		holder.tvInfo.setText(sb.toString());

		// Allow expansion to subcollections?
		if (subSize > 0) {
			holder.imgExpand.setVisibility(View.VISIBLE);

			holder.imgExpand.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					Intent i = new Intent(context, CollectionActivity.class);
					i.putExtra("com.mattrobertson.zotable.app.collectionKey", collection.getKey());
					context.startActivity(i);
				}
			});
		}
		else {
			holder.imgExpand.setVisibility(View.GONE);
		}

		// Listener for "Fav" star
		holder.imgFav.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (collection.isFavorite()) {
					holder.imgFav.setImageResource(R.drawable.ic_star_empty);
					collection.setFavorite(false);
					collection.save(db);
				} else {
					holder.imgFav.setImageResource(R.drawable.ic_star_filled);
					collection.setFavorite(true);
					collection.save(db);
				}
			}
		});


		db.close();

		return convertView;
	}

	static class ViewHolder{
		TextView tvTitle;
		TextView tvInfo;
		ImageView imgFav;
		ImageView imgExpand;
	}

    public View newView(Context context, Cursor cur, ViewGroup parent) {
        LayoutInflater li = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return li.inflate(R.layout.list_collection, parent, false);
    }

	@Override
	public void bindView(View view, final Context context, Cursor cursor) {
		TextView tvTitle = (TextView)view.findViewById(R.id.collection_title);
		TextView tvInfo = (TextView)view.findViewById(R.id.collection_info);
		final ImageView imgFav = (ImageView)view.findViewById(R.id.imgFavorite);
		final ImageView imgExpand = (ImageView)view.findViewById(R.id.imgExpand);
		
		final Database db = new Database(context);
	
		final ItemCollection collection = ItemCollection.load(cursor);

		if (collection.isFavorite())
			imgFav.setImageResource(R.drawable.ic_star_filled);
		else
			imgFav.setImageResource(R.drawable.ic_star_empty);

		// # of subcollections - used to display # & to determine expansion
		int subSize = collection.getSubcollections(db).size();

		tvTitle.setText(collection.getTitle());
		StringBuilder sb = new StringBuilder();
		sb.append(collection.getSize() + " items");
		sb.append("; " + subSize + " subcollections");
		if(!collection.dirty.equals(APIRequest.API_CLEAN))
			sb.append("; "+collection.dirty);
		tvInfo.setText(sb.toString());

		// Allow expansion to subcollections?
		if (subSize > 0) {
			imgExpand.setVisibility(View.VISIBLE);

			imgExpand.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					Intent i = new Intent(context, CollectionActivity.class);
					i.putExtra("com.mattrobertson.zotable.app.collectionKey", collection.getKey());
					context.startActivity(i);
				}
			});
		}

		// Listener for "Fav" star
		imgFav.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (collection.isFavorite()) {
					imgFav.setImageResource(R.drawable.ic_star_empty);
					collection.setFavorite(false);
					collection.save(db);
				} else {
					imgFav.setImageResource(R.drawable.ic_star_filled);
					collection.setFavorite(true);
					collection.save(db);
				}
			}
		});


		db.close();
	}
}
