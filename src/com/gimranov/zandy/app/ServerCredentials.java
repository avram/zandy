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

import java.io.File;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

import com.gimranov.zandy.app.task.APIRequest;

public class ServerCredentials {
	/** Application key -- available from Zotero */
	public static final String CONSUMERKEY = "93a5aac13612aed2a236";
	public static final String CONSUMERSECRET = "196d86bd1298cb78511c";
	
	/** This is the zotero:// protocol we intercept
	 * It probably shouldn't be changed. */
	public static final String CALLBACKURL = "zotero://";
	
	/** This is the Zotero API server. Those who set up independent
	 * Zotero installations will need to change this. */
	public static final String APIBASE = "https://api.zotero.org";
	
	/** These are the API GET-only methods */
	public static final String ITEMFIELDS = "/itemFields";
	public static final String ITEMTYPES = "/itemTypes";
	public static final String ITEMTYPECREATORTYPES = "/itemTypeCreatorTypes";
	public static final String CREATORFIELDS = "/creatorFields";
	public static final String ITEMNEW = "/items/new";

	/* These are the manipulation methods */
	// /users/1/items GET, POST, PUT, DELETE
	public static final String ITEMS = "/users/USERID/items";
	public static final String COLLECTIONS = "/users/USERID/collections";
	
	public static final String TAGS = "/tags";
	public static final String GROUPS = "/groups";	
	
	/** And these are the OAuth endpoints we talk to */
	public static final String OAUTHREQUEST = "https://www.zotero.org/oauth/request";
	public static final String OAUTHACCESS = "https://www.zotero.org/oauth/access";
	public static final String OAUTHAUTHORIZE = "https://www.zotero.org/oauth/authorize";
	
	/* More constants */
    public static final File sBaseStorageDir = new File(Environment.getExternalStorageDirectory(), "zandy");
    public static final File sDocumentStorageDir = new File(sBaseStorageDir, "documents");
    public static final File sCacheDir = new File(sBaseStorageDir, "cache");
	
	public static String prep(Context c, String in) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
		String userID = settings.getString("user_id", null);
		return prep(userID, in);
	}
	
	public static String prep(String id, String in) {
		return in.replace("USERID", id);
	}

	public static APIRequest prep(Context c, APIRequest req) {
		req.query = prep(c, req.query);
		return req;
	}
	
	public static APIRequest prep(String id, APIRequest req) {
		req.query = prep(id, req.query);
		return req;
	}
	
	public static boolean check(Context c) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
		if (settings.getString("user_id", null) != null
				&& settings.getString("user_key", null) != null
				&& !settings.getString("user_id", null).equals("")
				&& !settings.getString("user_key", null).equals(""))
			return true;
		else return false;
	}
}
