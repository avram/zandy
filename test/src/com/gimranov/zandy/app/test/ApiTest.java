package com.gimranov.zandy.app.test;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.test.AndroidTestCase;
import android.test.IsolatedContext;

import com.gimranov.zandy.app.ServerCredentials;
import com.gimranov.zandy.app.data.Database;
import com.gimranov.zandy.app.task.APIException;
import com.gimranov.zandy.app.task.APIRequest;


public class ApiTest extends AndroidTestCase {

	private IsolatedContext mContext;
	private Database mDb;
	private ServerCredentials mCred;
	
	/**
	 * Access information for the Zandy test user on Zotero.org
	 */
	private static final String TEST_UID = "743083";
	private static final String TEST_KEY = "JFRP2k4qvhRUm62kuDHXUUX3";
	private static final String TEST_COLLECTION = "U8GNSSF3";
	
	// unlikely to exist
	private static final String TEST_MISSING_ITEM = "ZZZZZZZZ";
	
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mContext = new IsolatedContext(null, getContext());
		mDb = new Database(mContext);
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
		SharedPreferences.Editor editor = settings.edit();
		// For Zotero, the key and secret are identical, it seems
		editor.putString("user_key", TEST_KEY);
		editor.putString("user_secret", TEST_KEY);
		editor.putString("user_id", TEST_UID);
		editor.commit();
		
		mCred = new ServerCredentials(mContext);
	}
	
	public void testPreConditions() {
		// Make sure we do indeed have the key set up
		assertTrue(ServerCredentials.check(mContext));
	}
	
	public void testItemsRequest() throws APIException {
		APIRequest items = APIRequest.fetchItems(false, mCred);
		items.issue(mDb, mCred);
	}
	
	public void testCollectionsRequest() throws APIException {
		APIRequest collections = APIRequest.fetchCollections(mCred);
		collections.issue(mDb, mCred);
	}
	
	public void testItemsForCollection() throws APIException {
		APIRequest collection = APIRequest.fetchItems(TEST_COLLECTION, false, mCred);
		collection.issue(mDb, mCred);
	}
	
	// verify that we fail on this item, which should be missing
	public void testMissingItem() throws APIException {
		APIRequest missingItem = APIRequest.fetchItem(TEST_MISSING_ITEM, mCred);
		try {
			missingItem.issue(mDb, mCred);
			// We shouldn't get here
			assertTrue(false);
		} catch (APIException e) {
			// We expect only one specific exception message
			if (!"Item does not exist".equals(e.getMessage()))
				throw e;
		}
	}
}
