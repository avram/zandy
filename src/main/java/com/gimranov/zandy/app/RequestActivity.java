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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.gimranov.zandy.app.data.Database;
import com.gimranov.zandy.app.task.APIRequest;

/**
 * This activity exists only for debugging, at least at this point
 * 
 * Potentially, this could let users cancel pending requests and do things
 * like resolve sync conflicts.
 * 
 * @author ajlyon
 *
 */
public class RequestActivity extends ListActivity {

	@SuppressWarnings("unused")
	private static final String TAG = "com.gimranov.zandy.app.RequestActivity";
	private Database db;
		
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        db = new Database(this);
        
        setContentView(R.layout.requests);
        
        this.setTitle(getResources().getString(R.string.sync_pending_requests));
        
        setListAdapter(new ResourceCursorAdapter(this, android.R.layout.simple_list_item_2, create()) {
			@Override
			public void bindView(View view, Context c, Cursor cur) {
				APIRequest req = new APIRequest(cur);
				TextView tvTitle = (TextView)view.findViewById(android.R.id.text1);
				TextView tvInfo = (TextView)view.findViewById(android.R.id.text2);
							
				tvTitle.setText(req.query);
				// Set to an html-formatted representation of the request
				tvInfo.setText(Html.fromHtml(req.toHtmlString()));
			}
        	
        });
        
        ListView lv = getListView();
        lv.setOnItemClickListener(new OnItemClickListener() {
        	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        		ResourceCursorAdapter adapter = (ResourceCursorAdapter) parent.getAdapter();
        		Cursor cur = adapter.getCursor();
        		// Place the cursor at the selected item
        		if (cur.moveToPosition(position)) {
        			// and replace the cursor with one for the selected collection
        			APIRequest req = new APIRequest(cur);
        			// toast for now-- later do something
            		Toast.makeText(getApplicationContext(),
            				req.query, 
            				Toast.LENGTH_SHORT).show();
        		} else {
        			// failed to move cursor; should do something
        		}
          }
        });
        
    }
    
	public Cursor create() {
		String[] cols = Database.REQUESTCOLS;
		String[] args = { };

		Cursor cur = db.query("apirequests", cols, "", args, null, null,
				null, null);

		return cur;
	}
    
}
