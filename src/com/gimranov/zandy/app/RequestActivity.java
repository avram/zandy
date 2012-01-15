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
package com.gimranov.zandy.app;

import android.app.ListActivity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.gimranov.zandy.app.data.CollectionAdapter;
import com.gimranov.zandy.app.data.Database;
import com.gimranov.zandy.app.task.APIRequest;

/**
 * This activity exists only for debugging, at least at this point
 * @author ajlyon
 *
 */
public class RequestActivity extends ListActivity {

	private static final String TAG = "com.gimranov.zandy.app.RequestActivity";
	private Database db;
		
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        db = new Database(this);
        
        setContentView(R.layout.collections);
        
        this.setTitle(getResources().getString(R.string.collections));
        
        setListAdapter(new ResourceCursorAdapter(this, R.layout.list_collection, create()) {

			@Override
			public void bindView(View view, Context c, Cursor cur) {
				APIRequest req = new APIRequest(cur);
				TextView tvTitle = (TextView)view.findViewById(R.id.collection_title);
				TextView tvInfo = (TextView)view.findViewById(R.id.collection_info);
							
				tvTitle.setText(req.query);
				// Set to an html-formatted representation of the request
				tvInfo.setText(Html.fromHtml(req.toHtmlString()));
			}
        	
        });
        
    }
    
    protected void onResume() {
		CollectionAdapter adapter = (CollectionAdapter) getListAdapter();
		Cursor newCursor = create();
		adapter.changeCursor(newCursor);
		adapter.notifyDataSetChanged();
		if (db == null) db = new Database(this);
    	super.onResume();
    }
    
	public Cursor create() {
		String[] cols = Database.REQUESTCOLS;
		String[] args = { };

		Cursor cur = db.query("apirequests", cols, "", args, null, null,
				null, null);

		return cur;
	}
    
}
