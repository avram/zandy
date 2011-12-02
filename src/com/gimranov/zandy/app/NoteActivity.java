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

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.gimranov.zandy.app.data.Attachment;
import com.gimranov.zandy.app.data.Database;

/**
 * This Activity handles displaying and editing of notes.
 * 
 * @author mlt
 *
 */
public class NoteActivity extends Activity {

	private static final String TAG = "com.gimranov.zandy.app.NoteActivity";
	
	static final int DIALOG_CONFIRM_NAVIGATE = 4;	
	static final int DIALOG_FILE_PROGRESS = 6;	
	static final int DIALOG_CONFIRM_DELETE = 5;	
	static final int DIALOG_NOTE = 3;
	static final int DIALOG_NEW = 1;
	
	public Attachment att;
	private Database db;
    private WebView mWebView;
	
	private class HelloWebViewClient extends WebViewClient {
	    @Override
	    public boolean shouldOverrideUrlLoading(WebView view, String url) {
	        view.loadUrl(url);
	        return true;
	    }
	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setDefaultKeyMode(DEFAULT_KEYS_DISABLE);
        setContentView(R.layout.note);

        db = new Database(this);
        
        /* Get the incoming data from the calling activity */
        final String attKey = getIntent().getStringExtra("com.gimranov.zandy.app.attKey");
        Attachment att = Attachment.load(attKey, db);
        this.att = att;
        
        if (att == null) {
        	Log.e(TAG, "NoteActivity started without attKey; finishing.");
        	finish();
        	return;
        }
        
        // FIXME: why is it causing exception?
        // this.setTitle(getResources().getString(R.string.note_for_item,att.title));
        //file:///android_assets/tinymce/
        mWebView = (WebView) findViewById(R.id.webview);
        mWebView.getSettings().setJavaScriptEnabled(true);
        // FIXME: can we template it somehow?
        String data =
        "<html xmlns=\"http://www.w3.org/1999/xhtml\">" +
        "<head>" +
        "<script type=\"text/javascript\" src=\"tiny_mce.js\"></script>" +
        "<script type=\"text/javascript\">" +
    	"tinyMCE.init({" +
    	"	mode : \"textareas\"," +
    	"	theme : \"simple\"" +
    	"});" +
        "</script>" +
        "</head>" +
    	"<body>" +
        "<textarea id=\"wysiwyg\">" +
        att.content.optString("note", "") +
        "</textarea>" +
        "</body>" +
        "</html>";

        mWebView.loadDataWithBaseURL("file:///android_asset/tiny_mce/", data, "text/html", "UTF-8", null);
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        }
        return true;
//        return super.onKeyDown(keyCode, event);
    }    
    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        return true;
    }    
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return true;
    }    
}
