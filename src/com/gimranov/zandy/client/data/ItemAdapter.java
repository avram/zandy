package com.gimranov.zandy.client.data;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.gimranov.zandy.client.R;

/**
 * Exposes items to be displayed by a ListView
 * @author ajlyon
 *
 */
public class ItemAdapter extends ResourceCursorAdapter {
	private static final String TAG = "com.gimranov.zandy.client.data.ItemAdapter";
	
	private ItemCollection parent;

	public ItemAdapter(Context context, Cursor cursor) {
		super(context, R.layout.list_item, cursor, false);
	}
	
    public View newView(Context context, Cursor cur, ViewGroup parent) {
        LayoutInflater li = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return li.inflate(R.layout.list_item, parent, false);
    }

    /**
     * Utility function to return parent collection, or null
     * if there is no parent collection being shown.
     * @return
     */
    public ItemCollection getParent() {
    	return parent;
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

	public static ItemAdapter create(Context context) {
		Database db = new Database(context);
		Cursor cursor = db.query("items", Database.ITEMCOLS, null, null, null, null, "item_year, item_title", null);
		if (cursor == null) {
			Log.e(TAG, "cursor is null");
		}
		Log.e(TAG, "created itemadapter");
		ItemAdapter adapter = new ItemAdapter(context, cursor);
		return adapter;
	}
	
	public static ItemAdapter create(Context context, ItemCollection parent) {
		Database db = new Database(context);
		String[] args = { parent.dbId };
		Cursor cursor = db.rawQuery("SELECT item_title, item_type, item_content, etag, dirty, " +
				"items._id, item_key, item_year, item_creator, timestamp " +
				" FROM items, itemtocollections WHERE items._id = item_id AND collection_id=? ORDER BY item_year, item_title",
				args);
		if (cursor == null) {
			Log.e(TAG, "cursor is null");
		}
		Log.e(TAG, "created itemadapter");
		ItemAdapter adapter = new ItemAdapter(context, cursor);
		adapter.parent = parent;
		return adapter;
	}
}
