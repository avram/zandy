package org.zotero.client.data;

import org.zotero.client.R;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

/**
 * Exposes items to be displayed by a ListView
 * @author ajlyon
 *
 */
public class ItemAdapter extends ResourceCursorAdapter {
	private static final String TAG = "org.zotero.client.data.ItemAdapter";

	public ItemAdapter(Context context, Cursor cursor) {
		super(context, R.layout.list_item, cursor, false);
	}
	
    public View newView(Context context, Cursor cur, ViewGroup parent) {
        LayoutInflater li = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Log.d(TAG, "running newView");
        return li.inflate(R.layout.list_item, parent, false);
    }

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		Log.d(TAG, "bindView view is: " + view.getId());
		TextView tvTitle = (TextView)view.findViewById(R.id.item_title);
		TextView tvType = (TextView)view.findViewById(R.id.item_type);
		TextView tvSummary = (TextView)view.findViewById(R.id.item_summary);
		TextView tvDirty = (TextView)view.findViewById(R.id.item_dirty);
	
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
		tvTitle.setText(item.getTitle());
		
		tvType.setText(item.getType());
		tvDirty.setText(item.dirty);
	}

	public static ItemAdapter create(Context context) {
		Database db = new Database(context);
		Cursor cursor = db.query("items", Database.ITEMCOLS, null, null, null, null, "item_title", null);
		if (cursor == null) {
			Log.e(TAG, "cursor is null");
		}
		Log.e(TAG, "created itemadapter");
		ItemAdapter adapter = new ItemAdapter(context, cursor);
		return adapter;
	}
}
