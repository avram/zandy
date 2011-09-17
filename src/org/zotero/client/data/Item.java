package org.zotero.client.data;

import org.json.JSONException;
import org.json.JSONObject;
import org.zotero.client.R;
import org.zotero.client.task.APIRequest;

import android.database.Cursor;
import android.util.Log;

public class Item  {
	private String id;
	private String title;
	private String type;
	private String owner;
	private String key;
	private String etag;
	private String year;
	private String creatorSummary;
	private JSONObject content;
	
	public String dbId;
	
	private static final String TAG = "org.zotero.client.data.Item";
	
	/**
	 * Represents whether the item has been dirtied
	 * Dirty items have changes that haven't been applied to the API
	 */
	public String dirty;
	public static Database db;
	
	public Item() {
		content = new JSONObject();
		year = "";
		creatorSummary = "";
		key = "";
		owner = "";
		type = "";
		title = "";
		id = "";
	}

	public String getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		if (this.title != title) {
			this.content.remove("title");
			
			try {
				this.content.put("title", title);
				this.title = title;
				this.dirty = APIRequest.API_DIRTY;
			} catch (JSONException e) {
				Log.e(TAG, "Exception setting title", e);
			}
		}
	}

	/* These can't be propagated, so they only make sense before the item
	 * has been saved to the API.
	 */
	public void setId(String id) {
		if (this.id != id) {
			this.id = id;
		}
	}

	public void setOwner(String owner) {
		if (this.owner != owner) {
			this.owner = owner;
		}
	}

	public void setKey(String key) {
		if (this.key != key) {
			this.key = key;
		}
	}

	public void setEtag(String etag) {
		if (this.etag != etag) {
			this.etag = etag;
		}
	}

	public String getOwner() {
		return owner;
	}

	public String getKey() {
		return key;
	}

	public JSONObject getContent() {
		return content;
	}

	public void setContent(JSONObject content) {
		if (this.content != content) {
			this.dirty = APIRequest.API_DIRTY;
			this.content = content;
		}
	}
	
	public void setContent(String content) throws JSONException {
		JSONObject con = new JSONObject(content);
		if (this.content != con) {
			this.dirty = APIRequest.API_DIRTY;
			this.content = con;
		}
	}
	
	public void setType(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public String getEtag() {
		return etag;
	}
	
	public String getYear() {
		return year;
	}

	public void setYear(String year) {
		this.year = year;
	}

	public String getCreatorSummary() {
		return creatorSummary;
	}

	public void setCreatorSummary(String creatorSummary) {
		this.creatorSummary = creatorSummary;
	}
	
	public void save() {
		Item existing = load(key);
		if (existing == null) {
			String[] args = { title, key, type, year, creatorSummary, content.toString(), etag, dirty };
			Cursor cur = db.rawQuery(
					"insert into items (item_title, item_key, item_type, item_year, item_creator, item_content, etag, dirty) " +
					"values (?, ?, ?, ?, ?, ?, ?, ?)", args);
			if (cur != null) cur.close();
			Item fromDB = load(key);
			dbId = fromDB.dbId;
		} else {
			dbId = existing.dbId;
			String[] args = { title, type, year, creatorSummary, content.toString(), etag, dirty, dbId };
			Log.i(TAG, "Updating existing item.");
			Cursor cur = db.rawQuery(
					"update items set item_title=?, item_type=?, item_year=?, item_creator=?, item_content=?, etag=?, dirty=? " +
					" where _id=?", args);
			if (cur != null) cur.close();
		}
	}
	
	/**
	 * Loads and returns an Item for the specified item key
	 * Returns null when no match is found for the specified itemKey
	 * 
	 * @param itemKey
	 * @return
	 */
	public static Item load(String itemKey) {
		String[] cols = Database.ITEMCOLS;
		String[] args = { itemKey };
		Cursor cur = db.query("items", cols, "item_key=?", args, null, null, null, null);

		Item item = load(cur);
		if (cur != null) cur.close();
		return item;
	}
	
	/**
	 * Loads an item from the specified Cursor, where the cursor was created using
	 * the recommended query in Database.ITEMCOLS
	 * 
	 * Does not close the cursor!
	 * 
	 * @param cur
	 * @return			An Item object for the current row of the Cursor
	 */
	public static Item load(Cursor cur) {
		Item item = new Item();
		if (cur == null) {
			Log.e(TAG, "Didn't find an item for update");
			return null;
		}
		
		item.setTitle(cur.getString(0));
		item.setType(cur.getString(1));
		try {
			item.setContent(cur.getString(2));
		} catch (JSONException e) {
			Log.e(TAG, "JSON error loading item", e);
		}
		item.setEtag(cur.getString(3));
		item.dirty = cur.getString(4);
		item.dbId = cur.getString(5);
		item.setKey(cur.getString(6));
		item.setYear(cur.getString(7));
		item.setCreatorSummary(cur.getString(8));
		return item;
	}
	
	public static int resourceForType(String type) {
		if (type == null || type.equals("")) return R.drawable.page_white;
		
		if (type.equals("artwork")) return R.drawable.picture;
//		if (type.equals("audioRecording")) return R.drawable.ic_menu_close_clear_cancel;
//		if (type.equals("bill")) return R.drawable.ic_menu_close_clear_cancel;
//		if (type.equals("blogPost")) return R.drawable.ic_menu_close_clear_cancel;
		if (type.equals("book")) return R.drawable.book;
		if (type.equals("bookSection")) return R.drawable.book_open;
//		if (type.equals("case")) return R.drawable.ic_menu_close_clear_cancel;
//		if (type.equals("computerProgram")) return R.drawable.ic_menu_close_clear_cancel;
//		if (type.equals("conferencePaper")) return R.drawable.ic_menu_close_clear_cancel;
		if (type.equals("dictionaryEntry")) return R.drawable.page_white_width;
		if (type.equals("document")) return R.drawable.page_white;
//		if (type.equals("email")) return R.drawable.ic_menu_close_clear_cancel;
		if (type.equals("encyclopediaArticle")) return R.drawable.page_white_text_width;
		if (type.equals("film")) return R.drawable.film;
//		if (type.equals("forumPost")) return R.drawable.ic_menu_close_clear_cancel;
//		if (type.equals("hearing")) return R.drawable.ic_menu_close_clear_cancel;
		if (type.equals("instantMessage")) return R.drawable.comment;
//		if (type.equals("interview")) return R.drawable.ic_menu_close_clear_cancel;
		if (type.equals("journalArticle")) return R.drawable.page_white_text;
		if (type.equals("letter")) return R.drawable.email;
		if (type.equals("magazineArticle")) return R.drawable.layout;
		if (type.equals("manuscript")) return R.drawable.script;
		if (type.equals("map")) return R.drawable.map;
		if (type.equals("newspaperArticle")) return R.drawable.newspaper;
		if (type.equals("patent")) return R.drawable.ic_menu_close_clear_cancel;
//		if (type.equals("podcast")) return R.drawable.ic_menu_close_clear_cancel;
		if (type.equals("presentation")) return R.drawable.page_white_powerpoint;
//		if (type.equals("radioBroadcast")) return R.drawable.ic_menu_close_clear_cancel;
		if (type.equals("report")) return R.drawable.report;
//		if (type.equals("statute")) return R.drawable.ic_menu_close_clear_cancel;
		if (type.equals("thesis")) return R.drawable.report_user;
		if (type.equals("tvBroadcast")) return R.drawable.television;
		if (type.equals("videoRecording")) return R.drawable.film;
		if (type.equals("webpage")) return R.drawable.page;

		// Not item types, but still something
		if (type.equals("collection")) return R.drawable.folder;
//		if (type.equals("pdf")) return R.drawable.ic_menu_close_clear_cancel;
//		if (type.equals("snapshot")) return R.drawable.ic_menu_close_clear_cancel;
//		if (type.equals("link")) return R.drawable.ic_menu_close_clear_cancel;
		
		// Return something generic if all else fails
		return R.drawable.page_white;
	}

}