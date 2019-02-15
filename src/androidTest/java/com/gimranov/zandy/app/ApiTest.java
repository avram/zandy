package com.gimranov.zandy.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import com.gimranov.zandy.app.data.Database;
import com.gimranov.zandy.app.task.APIException;
import com.gimranov.zandy.app.task.APIRequest;
import com.gimranov.zandy.app.test.BuildConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;


@RunWith(AndroidJUnit4.class)
@SmallTest
public class ApiTest {

	private Context mContext;
	private Database mDb;
	private ServerCredentials mCred;
	
	/**
	 * Access information for the Zandy test user on Zotero.org
	 */
	private static final String TEST_UID = BuildConfig.TEST_USER_ID;
	private static final String TEST_KEY = BuildConfig.TEST_USER_KEY_READONLY;
	private static final String TEST_COLLECTION = "U8GNSSF3";
	
	// unlikely to exist
	private static final String TEST_MISSING_ITEM = "ZZZZZZZZ";

	@Before
	public void setUp() {
		mContext = getTargetContext();
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

	@Test
	public void testPreConditions() {
		// Make sure we do indeed have the key set up
		assertTrue(ServerCredentials.check(mContext));
	}

	@Test
	public void testItemsRequest() throws APIException {
		APIRequest items = APIRequest.fetchItems(false, mCred);
		items.issue(mDb, mCred);
	}

	@Test
	public void testCollectionsRequest() throws APIException {
		APIRequest collections = APIRequest.fetchCollections(mCred);
		collections.issue(mDb, mCred);
	}

	@Test
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
			fail();
		} catch (APIException e) {
			// We expect only one specific exception message
			if (!"Item does not exist".equals(e.getCause().getMessage()))
				throw e;
		}
	}
}
