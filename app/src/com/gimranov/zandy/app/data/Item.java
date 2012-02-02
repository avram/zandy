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
package com.gimranov.zandy.app.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;

import com.gimranov.zandy.app.R;
import com.gimranov.zandy.app.task.APIRequest;

public class Item {
	private String id;
	private String title;
	private String type;
	private String owner;
	private String key;
	private String etag;
	private String year;
	private String children;

	private String creatorSummary;
	private JSONObject content;

	public String dbId;

	/**
	 * Timestamp of last update from server
	 */
	private String timestamp;

	/**
	 * Queue of dirty items to be sent to the server
	 */
	public static ArrayList<Item> queue = new ArrayList<Item>();

	private static final String TAG = "com.gimranov.zandy.app.data.Item";

	/**
	 * The next two types are arrays of information on items that we need
	 * elsewhere
	 */
	public static final String[] ITEM_TYPES_EN = { "Artwork",
			"Audio Recording", "Bill", "Blog Post", "Book", "Book Section",
			"Case", "Computer Program", "Conference Paper", "Dictionary Entry",
			"Document", "E-mail", "Encyclopedia Article", "Film", "Forum Post",
			"Hearing", "Instant Message", "Interview", "Journal Article",
			"Letter", "Magazine Article", "Manuscript", "Map",
			"Newspaper Article", "Note", "Patent", "Podcast", "Presentation",
			"Radio Broadcast", "Report", "Statute", "TV Broadcast", "Thesis",
			"Video Recording", "Web Page" };

	public static final String[] ITEM_TYPES = { "artwork", "audioRecording",
			"bill", "blogPost", "book", "bookSection", "case",
			"computerProgram", "conferencePaper", "dictionaryEntry",
			"document", "email", "encyclopediaArticle", "film", "forumPost",
			"hearing", "instantMessage", "interview", "journalArticle",
			"letter", "magazineArticle", "manuscript", "map",
			"newspaperArticle", "note",  "patent", "podcast", "presentation",
			"radioBroadcast", "report", "statute", "tvBroadcast", "thesis",
			"videoRecording", "webpage" };

	/**
	 * Represents whether the item has been dirtied Dirty items have changes
	 * that haven't been applied to the API
	 */
	public String dirty;

	public Item() {
		content = new JSONObject();
		year = "";
		creatorSummary = "";
		key = "";
		owner = "";
		type = "";
		title = "";
		id = "";
		etag = "";
		timestamp = "";
		children = "";
		dirty = APIRequest.API_CLEAN;
	}

	public Item(Context c, String type) {
		this();
		content = fieldsForItemType(c, type);
		key = UUID.randomUUID().toString();
		dirty = APIRequest.API_NEW;
		this.type = type;
	}

	public boolean equals(Item b) {
		if (b == null) return false;
		Log.d(TAG,"Comparing myself ("+key+") to "+b.key);
		if (b.key == null || key == null) return false;
		if (b.key.equals(key)) return true;
		return false;
	}
	
	public String getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		if (title == null) title = "";
		if (!this.title.equals(title)) {
			this.content.remove("title");

			try {
				this.content.put("title", title);
				this.title = title;
				if (!APIRequest.API_CLEAN.equals(this.dirty))
					this.dirty = APIRequest.API_DIRTY;
			} catch (JSONException e) {
				Log.e(TAG, "Exception setting title", e);
			}
		}
	}

	/*
	 * These can't be propagated, so they only make sense before the item has
	 * been saved to the API.
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
		if (!this.content.toString().equals(content.toString())) {
			if (!APIRequest.API_CLEAN.equals(this.dirty))
				this.dirty = APIRequest.API_DIRTY;
			this.content = content;
		}
	}

	public void setContent(String content) throws JSONException {
		JSONObject con = new JSONObject(content);
		if (this.content != con) {
			if (!APIRequest.API_CLEAN.equals(this.dirty))
				this.dirty = APIRequest.API_DIRTY;
			this.content = con;
		}
	}

	public void setType(String type) {
		this.type = type;
	}

	public void setChildren(String children) {
		this.children = children;
	}
	
	public String getChildren() {
		return children;
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

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * Makes ArrayList<Bundle> from the present item. This was moved from
	 * ItemDataActivity, but it's most likely to be used by such display
	 * activities
	 */
	public ArrayList<Bundle> toBundleArray(Database db) {
		JSONObject itemContent = this.content;
		/*
		 * Here we walk through the data and make Bundles to send to the
		 * ArrayAdapter. There should be no real risk of JSON exceptions, since
		 * the JSON was checked when initialized in the Item object.
		 * 
		 * Each Bundle has two keys: "label" and "content"
		 */
		JSONArray fields = itemContent.names();
		ArrayList<Bundle> rows = new ArrayList<Bundle>();
		Bundle b;

		try {
			JSONArray values = itemContent.toJSONArray(fields);
			for (int i = 0; i < itemContent.length(); i++) {
				b = new Bundle();

				/* Special handling for some types */
				if (fields.getString(i).equals("tags")) {
					// We display the tags semicolon-delimited
					StringBuilder sb = new StringBuilder();
					try {
						JSONArray tagArray = values.getJSONArray(i);
						for (int j = 0; j < tagArray.length(); j++) {
							sb.append(tagArray.getJSONObject(j)
									.getString("tag"));
							if (j < tagArray.length() - 1)
								sb.append("; ");
						}

						b.putString("content", sb.toString());
					} catch (JSONException e) {
						// Fall back to empty
						Log.e(TAG, "Exception parsing tags, with input: "
								+ values.getString(i), e);
						b.putString("content", "");
					}
				} else if (fields.getString(i).equals("notes")) {
					// TODO handle notes
					continue;
				} else if (fields.getString(i).equals("creators")) {
					/*
					 * Creators should be labeled with role and listed nicely
					 * This logic isn't as good as it could be.
					 */
					JSONArray creatorArray = values.getJSONArray(i);
					JSONObject creator;
					StringBuilder sb = new StringBuilder();
					for (int j = 0; j < creatorArray.length(); j++) {
						creator = creatorArray.getJSONObject(j);
						if (creator.getString("creatorType").equals("author")) {
							if (creator.has("name"))
								sb.append(creator.getString("name"));
							else
								sb.append(creator.getString("firstName") + " "
										+ creator.getString("lastName"));
						} else {
							if (creator.has("name"))
								sb.append(creator.getString("name"));
							else
								sb.append(creator.getString("firstName")
										+ " "
										+ creator.getString("lastName")
										+ " ("
										+ Item.localizedStringForString(creator
												.getString("creatorType"))
										+ ")");
						}
						if (j < creatorArray.length() - 1)
							sb.append(", ");
					}
					b.putString("content", sb.toString());
				} else if (fields.getString(i).equals("itemType")) {
					// We want to show the localized or human-readable type
					b.putString("content", Item.localizedStringForString(values
							.getString(i)));
				} else {
					// All other data is treated as just text
					b.putString("content", values.getString(i));
				}
				b.putString("label", fields.getString(i));
				b.putString("itemKey", getKey());
				rows.add(b);
			}
			b = new Bundle();
			int notes = 0;
			int atts = 0;
			ArrayList<Attachment> attachments = Attachment.forItem(this, db);
			for (Attachment a : attachments) {
				if ("note".equals(a.getType())) notes++;
				else atts++;
			}

			b.putInt("noteCount", notes);
			b.putInt("attachmentCount", atts);
			b.putString("content", "not-empty-so-sorting-works");
			b.putString("label", "children");
			b.putString("itemKey", getKey());
			rows.add(b);
			
			b = new Bundle();
			int collectionCount = ItemCollection.getCollectionCount(this, db);

			b.putInt("collectionCount", collectionCount);
			b.putString("content", "not-empty-so-sorting-works");
			b.putString("label", "collections");
			b.putString("itemKey", getKey());
			rows.add(b);
		} catch (JSONException e) {
			/*
			 * We could die here, but I'd rather not, since this shouldn't be
			 * possible.
			 */
			Log.e(TAG, "JSON parse exception making bundles!", e);
		}

		/* We'd like to put these in a certain order, so let's try! */
		Collections.sort(rows, new Comparator<Bundle>() {
			@Override
			public int compare(Bundle b1, Bundle b2) {
				boolean mt1 = (b1.containsKey("content") && b1.getString("content").equals(""));
				boolean mt2 = (b2.containsKey("content") && b2.getString("content").equals(""));
				/* Put the empty fields at the bottom, same order */

				return (
							Item.sortValueForLabel(b1.getString("label"))
							- Item.sortValueForLabel(b2.getString("label"))
					- 	(mt2 ? 300 : 0)
					+ 	(mt1 ? 300 : 0));
			}
		});
		return rows;
	}

	/**
	 * Makes ArrayList<Bundle> from the present item's tags Primarily for use
	 * with TagActivity, but who knows?
	 */
	public ArrayList<Bundle> tagsToBundleArray() {
		JSONObject itemContent = this.content;
		/*
		 * Here we walk through the data and make Bundles to send to the
		 * ArrayAdapter. There should be no real risk of JSON exceptions, since
		 * the JSON was checked when initialized in the Item object.
		 * 
		 * Each Bundle has three keys: "itemKey", "tag", and "type"
		 */

		ArrayList<Bundle> rows = new ArrayList<Bundle>();

		Bundle b = new Bundle();

		if (!itemContent.has("tags")) {
			return rows;
		}

		try {
			JSONArray tags = itemContent.getJSONArray("tags");
			Log.d(TAG, tags.toString());
			for (int i = 0; i < tags.length(); i++) {
				b = new Bundle();
				// Type is not always specified, but we try to get it
				// and fall back to 0 when missing.
				Log.d(TAG, tags.getJSONObject(i).toString());
				if (tags.getJSONObject(i).has("type"))
					b.putInt("type", tags.getJSONObject(i).optInt("type", 0));
				b.putString("tag", tags.getJSONObject(i).optString("tag"));
				b.putString("itemKey", this.key);
				rows.add(b);
			}
		} catch (JSONException e) {
			Log.e(TAG, "JSON exception caught in tag bundler: ", e);
		}

		return rows;
	}

	/**
	 * Makes ArrayList<Bundle> from the present item's creators. Primarily for
	 * use with CreatorActivity, but who knows?
	 */
	public ArrayList<Bundle> creatorsToBundleArray() {
		JSONObject itemContent = this.content;
		/*
		 * Here we walk through the data and make Bundles to send to the
		 * ArrayAdapter. There should be no real risk of JSON exceptions, since
		 * the JSON was checked when initialized in the Item object.
		 * 
		 * Each Bundle has six keys: "itemKey", "name", "firstName", "lastName",
		 * "creatorType", "position"
		 * 
		 * Field mode is encoded implicitly -- if either of "firstName" and
		 * "lastName" is non-empty ("") or non-null, we treat this as a
		 * two-field name. key The field "name" is the one-field name if the
		 * others are empty, otherwise it's a display version of the two-field
		 * name.
		 */

		ArrayList<Bundle> rows = new ArrayList<Bundle>();

		Bundle b = new Bundle();

		if (!itemContent.has("creators")) {
			return rows;
		}

		try {
			JSONArray creators = itemContent.getJSONArray("creators");
			Log.d(TAG, creators.toString());
			for (int i = 0; i < creators.length(); i++) {
				b = new Bundle();
				Log.d(TAG, creators.getJSONObject(i).toString());
				b.putString("creatorType", creators.getJSONObject(i).getString(
						"creatorType"));
				b.putString("firstName", creators.getJSONObject(i).optString(
						"firstName"));
				b.putString("lastName", creators.getJSONObject(i).optString(
						"lastName"));
				b.putString("name", creators.getJSONObject(i).optString(
								"name"));
				// If name is empty, fill with the others
				if (b.getString("name").equals(""))
					b.putString("name", b.getString("firstName") + " "
							+ b.getString("lastName"));
				b.putString("itemKey", this.key);
				b.putInt("position", i);
				rows.add(b);
			}
		} catch (JSONException e) {
			Log.e(TAG, "JSON exception caught in creator bundler: ", e);
		}

		return rows;
	}

	/**
	 * Saves the item's current state. Marking dirty should happen before this
	 */
	public void save(Database db) {
		Item existing = load(key, db);
		if (dbId == null && existing == null) {
			String[] args = { title, key, type, year, creatorSummary,
					content.toString(), etag, dirty, timestamp, children };
			Cursor cur = db
					.rawQuery(
							"insert into items (item_title, item_key, item_type, item_year, item_creator, item_content, etag, dirty, timestamp, item_children) "
									+ "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
							args);
			if (cur != null)
				cur.close();
			Item fromDB = load(key, db);
			dbId = fromDB.dbId;
		} else {
			if (dbId == null)
				dbId = existing.dbId;
			String[] args = { title, type, year, creatorSummary,
					content.toString(), etag, dirty, timestamp, key, children, dbId };
			Log.i(TAG, "Updating existing item");
			Cursor cur = db
					.rawQuery(
							"update items set item_title=?, item_type=?, item_year=?," +
							" item_creator=?, item_content=?, etag=?, dirty=?," +
							" timestamp=?, item_key=?, item_children=? "
									+ " where _id=?", args);
			if (cur != null)
				cur.close();
		}
	}

	/**
	 * Deletes an item from the database, keeping a record of it in the deleteditems table
	 * We will then send out delete requests via the API to propagate the deletion
	 */
	public void delete(Database db) {
		String[] args = { dbId };
		db.rawQuery("delete from items where _id=?", args);
		db.rawQuery("delete from itemtocreators where item_id=?", args);
		db.rawQuery("delete from itemtocollections where item_id=?", args);
		ArrayList<Attachment> atts = Attachment.forItem(this, db);
		for (Attachment a : atts) {
			a.delete(db);
		}
		// Don't prepare deletion requests for unsynced new items
		if (!APIRequest.API_NEW.equals(dirty)) {
			String[] args2 = { key, etag };
			db.rawQuery("insert into deleteditems (item_key, etag) values (?, ?)", args2);	
		}
	}
	
	/**
	 * Loads and returns an Item for the specified item key Returns null when no
	 * match is found for the specified itemKey
	 * 
	 * @param itemKey
	 * @return
	 */
	public static Item load(String itemKey, Database db) {
		String[] cols = Database.ITEMCOLS;
		String[] args = { itemKey };
		Cursor cur = db.query("items", cols, "item_key=?", args, null, null,
				null, null);
		Item item = load(cur);
		if (cur != null)
			cur.close();
		return item;
	}

	/**
	 * Loads and returns an Item for the specified item DB ID
	 * Returns null when no match is found for the specified DB ID
	 * 
	 * @param itemDbId
	 * @return
	 */
	public static Item loadDbId(String itemDbId, Database db) {
		String[] cols = Database.ITEMCOLS;
		String[] args = { itemDbId };
		Cursor cur = db.query("items", cols, "_id=?", args, null, null,
				null, null);
		Item item = load(cur);
		if (cur != null)
			cur.close();
		return item;
	}
	
	/**
	 * Loads an item from the specified Cursor, where the cursor was created
	 * using the recommended query in Database.ITEMCOLS
	 * 
	 * Does not close the cursor!
	 * 
	 * @param cur
	 * @return An Item object for the current row of the Cursor
	 */
	public static Item load(Cursor cur) {
		Item item = new Item();
		if (cur == null) {
			Log.e(TAG, "Didn't find an item for update");
			return null;
		}
		// {"item_title", "item_type", "item_content", "etag", "dirty", "_id",
		// "item_key", "item_year", "item_creator", "timestamp", "item_children"};

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
		item.setTimestamp(cur.getString(9));
		item.children = cur.getString(10);
		if (item.children == null) item.children = "";
		return item;
	}

	/**
	 * Static method for modification of items in the database
	 */
	public static void set(String itemKey, String label, String content, Database db) {
		// Load the item
		Item item = load(itemKey, db);
		
		// Don't set anything to null
		if (content == null) content = "";
		
		if (label.equals("title")) {
			item.title = content;
		}

		if (label.equals("itemType")) {
			item.type = content;
		}

		if (label.equals("children")) {
			item.children = content;
		}
		
		if (label.equals("date")) {
			item.year = content.replaceAll("^.*?(\\d{4}).*$","$1");
		}

		try {
			item.content.put(label, content);
		} catch (JSONException e) {
			Log
					.e(TAG,
							"Caught JSON exception when we tried to modify the JSON content");
		}
		item.dirty = APIRequest.API_DIRTY;
		item.save(db);
		item = load(itemKey, db);
	}

	/**
	 * Static method for setting tags Add a tag, change a tag, or replace an
	 * existing tag. If newTag is empty ("") or null, the old tag is simply
	 * deleted.
	 * 
	 * If oldTag is empty or null, the new tag is appended.
	 * 
	 * We make it into a user tag, which the desktop client does as well.
	 * 
	 */
	public static void setTag(String itemKey, String oldTag, String newTag,
			int type, Database db) {
		// Load the item
		Item item = load(itemKey, db);

		try {
			JSONArray tags = item.content.getJSONArray("tags");
			JSONArray newTags = new JSONArray();
			Log.d(TAG, "Old: " + tags.toString());
			// Allow adding a new tag
			if (oldTag == null || oldTag.equals("")) {
				Log.d(TAG, "Adding new tag: " + newTag);
				newTags = tags.put(new JSONObject().put("tag", newTag).put(
						"type", type));
			} else {
				// In other cases, we're removing or replacing a tag
				for (int i = 0; i < tags.length(); i++) {
					if (tags.getJSONObject(i).getString("tag").equals(oldTag)) {
						if (newTag != null && !newTag.equals(""))
							newTags.put(new JSONObject().put("tag", newTag)
									.put("type", type));
						else
							Log.d(TAG, "Tag removed: " + oldTag);
					} else {
						newTags.put(tags.getJSONObject(i));
					}
				}
			}
			item.content.put("tags", newTags);
		} catch (JSONException e) {
			Log.e(TAG,"Caught JSON exception when we tried to modify the JSON content",e);
		}
		item.dirty = APIRequest.API_DIRTY;
		item.save(db);
	}

	/**
	 * Static method for setting creators
	 * 
	 * Add, change, or replace a creator. If creator is null, the old creator is
	 * simply deleted.
	 * 
	 * If position is -1, the new creator is appended.
	 */
	public static void setCreator(String itemKey, Creator c, int position, Database db) {
		// Load the item
		Item item = load(itemKey, db);
		

		try {
			JSONArray creators = item.content.getJSONArray("creators");
			JSONArray newCreators = new JSONArray();
			Log.d(TAG, "Old: " + creators.toString());
			// Allow adding a new tag
			if (position < 0) {
				newCreators = creators.put(c.toJSON());
			} else {
				if (c == null) {
					// we have a deletion
					for (int i = 0; i < creators.length(); i++) {
						if (i == position)
							continue; // skip the deleted one
						newCreators.put(creators.get(i));
					}
				} else {
					newCreators = creators.put(position, c.toJSON());
				}
			}
			StringBuilder sb = new StringBuilder();
			for (int j = 0; j < creators.length(); j++) {
				if (j > 0) sb.append(", ");
				sb.append(( (JSONObject) creators.get(j) ).optString("lastName", ""));
			}
			item.creatorSummary = sb.toString();
			item.content.put("creators", newCreators);
			Log.d(TAG, "New: " + newCreators.toString());
		} catch (JSONException e) {
			Log.e(TAG,"Caught JSON exception when we tried to modify the JSON content");
		}
		item.dirty = APIRequest.API_DIRTY;
		item.save(db);
	}

	/**
	 * Identifies dirty items in the database and queues them for syncing
	 */
	public static void queue(Database db) {
		Log.d(TAG, "Clearing item dirty queue before repopulation");
		queue.clear();
		Item item;
		String[] cols = Database.ITEMCOLS;
		String[] args = { APIRequest.API_CLEAN };
		Cursor cur = db.query("items", cols, "dirty!=?", args, null, null,
				null, null);

		if (cur == null) {
			Log.d(TAG, "No dirty items found in database");
			queue.clear();
			return;
		}

		do {
			Log.d(TAG, "Adding item to dirty queue");
			item = load(cur);
			queue.add(item);
		} while (cur.moveToNext() != false);

		if (cur != null)
			cur.close();
	}

	/**
	 * Maps types to the resources providing images for them.
	 * 
	 * @param type
	 * @return A resource representing an image for the item or other type
	 */
	public static int resourceForType(String type) {
		if (type == null || type.equals(""))
			return R.drawable.page_white;

		// TODO Complete this list

		if (type.equals("artwork"))
			return R.drawable.picture;
		// if (type.equals("audioRecording")) return
		// R.drawable.ic_menu_close_clear_cancel;
		// if (type.equals("bill")) return
		// R.drawable.ic_menu_close_clear_cancel;
		// if (type.equals("blogPost")) return
		// R.drawable.ic_menu_close_clear_cancel;
		if (type.equals("book"))
			return R.drawable.book;
		if (type.equals("bookSection"))
			return R.drawable.book_open;
		// if (type.equals("case")) return
		// R.drawable.ic_menu_close_clear_cancel;
		// if (type.equals("computerProgram")) return
		// R.drawable.ic_menu_close_clear_cancel;
		// if (type.equals("conferencePaper")) return
		// R.drawable.ic_menu_close_clear_cancel;
		if (type.equals("dictionaryEntry"))
			return R.drawable.page_white_width;
		if (type.equals("document"))
			return R.drawable.page_white;
		// if (type.equals("email")) return
		// R.drawable.ic_menu_close_clear_cancel;
		if (type.equals("encyclopediaArticle"))
			return R.drawable.page_white_text_width;
		if (type.equals("film"))
			return R.drawable.film;
		// if (type.equals("forumPost")) return
		// R.drawable.ic_menu_close_clear_cancel;
		// if (type.equals("hearing")) return
		// R.drawable.ic_menu_close_clear_cancel;
		if (type.equals("instantMessage"))
			return R.drawable.comment;
		// if (type.equals("interview")) return
		// R.drawable.ic_menu_close_clear_cancel;
		if (type.equals("journalArticle"))
			return R.drawable.page_white_text;
		if (type.equals("letter"))
			return R.drawable.email;
		if (type.equals("magazineArticle"))
			return R.drawable.layout;
		if (type.equals("manuscript"))
			return R.drawable.script;
		if (type.equals("map"))
			return R.drawable.map;
		if (type.equals("newspaperArticle"))
			return R.drawable.newspaper;
		// if (type.equals("patent")) return
		// R.drawable.ic_menu_close_clear_cancel;
		// if (type.equals("podcast")) return
		// R.drawable.ic_menu_close_clear_cancel;
		if (type.equals("presentation"))
			return R.drawable.page_white_powerpoint;
		// if (type.equals("radioBroadcast")) return
		// R.drawable.ic_menu_close_clear_cancel;
		if (type.equals("report"))
			return R.drawable.report;
		// if (type.equals("statute")) return
		// R.drawable.ic_menu_close_clear_cancel;
		if (type.equals("thesis"))
			return R.drawable.report_user;
		if (type.equals("tvBroadcast"))
			return R.drawable.television;
		if (type.equals("videoRecording"))
			return R.drawable.film;
		if (type.equals("webpage"))
			return R.drawable.page;

		// Not item types, but still something
		if (type.equals("collection"))
			return R.drawable.folder;
		 if (type.equals("application/pdf"))
			 return R.drawable.page_white_acrobat;
		 if (type.equals("note"))
			 return R.drawable.note;
		// if (type.equals("snapshot")) return
		// R.drawable.ic_menu_close_clear_cancel;
		// if (type.equals("link")) return
		// R.drawable.ic_menu_close_clear_cancel;

		// Return something generic if all else fails
		return R.drawable.page_white;
	}

	/**
	 * Provides the human-readable equivalent for strings
	 * 
	 * TODO This should pull the data from the API as a fallback, but we will
	 * hard-code the list for now
	 * 
	 * @param s
	 * @return
	 */
	// XXX i18n
	public static String localizedStringForString(String s) {
		if (s==null) {
			Log.e(TAG, "Received null string in localizedStringForString");
			return "";
		}
		// Item fields from the API
		if (s.equals("numPages"))
			return "# of Pages";
		if (s.equals("numberOfVolumes"))
			return "# of Volumes";
		if (s.equals("abstractNote"))
			return "Abstract";
		if (s.equals("accessDate"))
			return "Accessed";
		if (s.equals("applicationNumber"))
			return "Application Number";
		if (s.equals("archive"))
			return "Archive";
		if (s.equals("artworkSize"))
			return "Artwork Size";
		if (s.equals("assignee"))
			return "Assignee";
		if (s.equals("billNumber"))
			return "Bill Number";
		if (s.equals("blogTitle"))
			return "Blog Title";
		if (s.equals("bookTitle"))
			return "Book Title";
		if (s.equals("callNumber"))
			return "Call Number";
		if (s.equals("caseName"))
			return "Case Name";
		if (s.equals("code"))
			return "Code";
		if (s.equals("codeNumber"))
			return "Code Number";
		if (s.equals("codePages"))
			return "Code Pages";
		if (s.equals("codeVolume"))
			return "Code Volume";
		if (s.equals("committee"))
			return "Committee";
		if (s.equals("company"))
			return "Company";
		if (s.equals("conferenceName"))
			return "Conference Name";
		if (s.equals("country"))
			return "Country";
		if (s.equals("court"))
			return "Court";
		if (s.equals("DOI"))
			return "DOI";
		if (s.equals("date"))
			return "Date";
		if (s.equals("dateDecided"))
			return "Date Decided";
		if (s.equals("dateEnacted"))
			return "Date Enacted";
		if (s.equals("dictionaryTitle"))
			return "Dictionary Title";
		if (s.equals("distributor"))
			return "Distributor";
		if (s.equals("docketNumber"))
			return "Docket Number";
		if (s.equals("documentNumber"))
			return "Document Number";
		if (s.equals("edition"))
			return "Edition";
		if (s.equals("encyclopediaTitle"))
			return "Encyclopedia Title";
		if (s.equals("episodeNumber"))
			return "Episode Number";
		if (s.equals("extra"))
			return "Extra";
		if (s.equals("audioFileType"))
			return "File Type";
		if (s.equals("filingDate"))
			return "Filing Date";
		if (s.equals("firstPage"))
			return "First Page";
		if (s.equals("audioRecordingFormat"))
			return "Format";
		if (s.equals("videoRecordingFormat"))
			return "Format";
		if (s.equals("forumTitle"))
			return "Forum/Listserv Title";
		if (s.equals("genre"))
			return "Genre";
		if (s.equals("history"))
			return "History";
		if (s.equals("ISBN"))
			return "ISBN";
		if (s.equals("ISSN"))
			return "ISSN";
		if (s.equals("institution"))
			return "Institution";
		if (s.equals("issue"))
			return "Issue";
		if (s.equals("issueDate"))
			return "Issue Date";
		if (s.equals("issuingAuthority"))
			return "Issuing Authority";
		if (s.equals("journalAbbreviation"))
			return "Journal Abbr";
		if (s.equals("label"))
			return "Label";
		if (s.equals("language"))
			return "Language";
		if (s.equals("programmingLanguage"))
			return "Language";
		if (s.equals("legalStatus"))
			return "Legal Status";
		if (s.equals("legislativeBody"))
			return "Legislative Body";
		if (s.equals("libraryCatalog"))
			return "Library Catalog";
		if (s.equals("archiveLocation"))
			return "Loc. in Archive";
		if (s.equals("interviewMedium"))
			return "Medium";
		if (s.equals("artworkMedium"))
			return "Medium";
		if (s.equals("meetingName"))
			return "Meeting Name";
		if (s.equals("nameOfAct"))
			return "Name of Act";
		if (s.equals("network"))
			return "Network";
		if (s.equals("pages"))
			return "Pages";
		if (s.equals("patentNumber"))
			return "Patent Number";
		if (s.equals("place"))
			return "Place";
		if (s.equals("postType"))
			return "Post Type";
		if (s.equals("priorityNumbers"))
			return "Priority Numbers";
		if (s.equals("proceedingsTitle"))
			return "Proceedings Title";
		if (s.equals("programTitle"))
			return "Program Title";
		if (s.equals("publicLawNumber"))
			return "Public Law Number";
		if (s.equals("publicationTitle"))
			return "Publication";
		if (s.equals("publisher"))
			return "Publisher";
		if (s.equals("references"))
			return "References";
		if (s.equals("reportNumber"))
			return "Report Number";
		if (s.equals("reportType"))
			return "Report Type";
		if (s.equals("reporter"))
			return "Reporter";
		if (s.equals("reporterVolume"))
			return "Reporter Volume";
		if (s.equals("rights"))
			return "Rights";
		if (s.equals("runningTime"))
			return "Running Time";
		if (s.equals("scale"))
			return "Scale";
		if (s.equals("section"))
			return "Section";
		if (s.equals("series"))
			return "Series";
		if (s.equals("seriesNumber"))
			return "Series Number";
		if (s.equals("seriesText"))
			return "Series Text";
		if (s.equals("seriesTitle"))
			return "Series Title";
		if (s.equals("session"))
			return "Session";
		if (s.equals("shortTitle"))
			return "Short Title";
		if (s.equals("studio"))
			return "Studio";
		if (s.equals("subject"))
			return "Subject";
		if (s.equals("system"))
			return "System";
		if (s.equals("title"))
			return "Title";
		if (s.equals("thesisType"))
			return "Type";
		if (s.equals("mapType"))
			return "Type";
		if (s.equals("manuscriptType"))
			return "Type";
		if (s.equals("letterType"))
			return "Type";
		if (s.equals("presentationType"))
			return "Type";
		if (s.equals("url"))
			return "URL";
		if (s.equals("university"))
			return "University";
		if (s.equals("version"))
			return "Version";
		if (s.equals("volume"))
			return "Volume";
		if (s.equals("websiteTitle"))
			return "Website Title";
		if (s.equals("websiteType"))
			return "Website Type";

		// Item fields not in the API
		if (s.equals("creators"))
			return "Creators";
		if (s.equals("tags"))
			return "Tags";
		if (s.equals("itemType"))
			return "Item Type";
		if (s.equals("children"))
			return "Attachments";
		if (s.equals("collections"))
			return "Collections";
		
		// And item types
		if (s.equals("artwork"))
			return "Artwork";
		if (s.equals("audioRecording"))
			return "Audio Recording";
		if (s.equals("bill"))
			return "Bill";
		if (s.equals("blogPost"))
			return "Blog Post";
		if (s.equals("book"))
			return "Book";
		if (s.equals("bookSection"))
			return "Book Section";
		if (s.equals("case"))
			return "Case";
		if (s.equals("computerProgram"))
			return "Computer Program";
		if (s.equals("conferencePaper"))
			return "Conference Paper";
		if (s.equals("dictionaryEntry"))
			return "Dictionary Entry";
		if (s.equals("document"))
			return "Document";
		if (s.equals("email"))
			return "E-mail";
		if (s.equals("encyclopediaArticle"))
			return "Encyclopedia Article";
		if (s.equals("film"))
			return "Film";
		if (s.equals("forumPost"))
			return "Forum Post";
		if (s.equals("hearing"))
			return "Hearing";
		if (s.equals("instantMessage"))
			return "Instant Message";
		if (s.equals("interview"))
			return "Interview";
		if (s.equals("journalArticle"))
			return "Journal Article";
		if (s.equals("letter"))
			return "Letter";
		if (s.equals("magazineArticle"))
			return "Magazine Article";
		if (s.equals("manuscript"))
			return "Manuscript";
		if (s.equals("map"))
			return "Map";
		if (s.equals("newspaperArticle"))
			return "Newspaper Article";
		if (s.equals("note"))
			return "Note";
		if (s.equals("patent"))
			return "Patent";
		if (s.equals("podcast"))
			return "Podcast";
		if (s.equals("presentation"))
			return "Presentation";
		if (s.equals("radioBroadcast"))
			return "Radio Broadcast";
		if (s.equals("report"))
			return "Report";
		if (s.equals("statute"))
			return "Statute";
		if (s.equals("tvBroadcast"))
			return "TV Broadcast";
		if (s.equals("thesis"))
			return "Thesis";
		if (s.equals("videoRecording"))
			return "Video Recording";
		if (s.equals("webpage"))
			return "Web Page";

		// Creator types
		if (s.equals("artist"))
			return "Artist";
		if (s.equals("attorneyAgent"))
			return "Attorney/Agent";
		if (s.equals("author"))
			return "Author";
		if (s.equals("bookAuthor"))
			return "Book Author";
		if (s.equals("cartographer"))
			return "Cartographer";
		if (s.equals("castMember"))
			return "Cast Member";
		if (s.equals("commenter"))
			return "Commenter";
		if (s.equals("composer"))
			return "Composer";
		if (s.equals("contributor"))
			return "Contributor";
		if (s.equals("cosponsor"))
			return "Cosponsor";
		if (s.equals("counsel"))
			return "Counsel";
		if (s.equals("director"))
			return "Director";
		if (s.equals("editor"))
			return "Editor";
		if (s.equals("guest"))
			return "Guest";
		if (s.equals("interviewee"))
			return "Interview With";
		if (s.equals("interviewer"))
			return "Interviewer";
		if (s.equals("inventor"))
			return "Inventor";
		if (s.equals("performer"))
			return "Performer";
		if (s.equals("podcaster"))
			return "Podcaster";
		if (s.equals("presenter"))
			return "Presenter";
		if (s.equals("producer"))
			return "Producer";
		if (s.equals("programmer"))
			return "Programmer";
		if (s.equals("recipient"))
			return "Recipient";
		if (s.equals("reviewedAuthor"))
			return "Reviewed Author";
		if (s.equals("scriptwriter"))
			return "Scriptwriter";
		if (s.equals("seriesEditor"))
			return "Series Editor";
		if (s.equals("sponsor"))
			return "Sponsor";
		if (s.equals("translator"))
			return "Translator";
		if (s.equals("wordsBy"))
			return "Words By";

		// Or just leave it the way it is
		return s;
	}

	/**
	 * Gets creator types, localized, for specified item type.
	 * 
	 * @param s
	 * @return
	 */
	public static String[] localizedCreatorTypesForItemType(String s) {
		String[] types = creatorTypesForItemType(s);
		String[] local = new String[types.length];

		for (int i = 0; i < types.length; i++) {
			local[i] = localizedStringForString(types[i]);
		}
		return local;
	}

	/**
	 * Gets valid creator types for the specified item type. Would be good to
	 * have this draw from the API, but probably easier to hard-code the whole
	 * mess.
	 * 
	 * Remapping of creator types on item type conversion is a separate issue.
	 * 
	 * @param s
	 *            itemType to provide creatorTypes for
	 * @return
	 */
	public static String[] creatorTypesForItemType(String s) {
		Log.d(TAG, "Getting creator types for item type: " + s);
		if (s.equals("artwork")) {
			String[] str = { "artist", "contributor" };
			return str;
		}
		if (s.equals("audioRecording")) {
			String[] str = { "performer", "composer", "contributor", "wordsBy" };
			return str;
		}
		if (s.equals("bill")) {
			String[] str = { "sponsor", "contributor", "cosponsor" };
			return str;
		}
		if (s.equals("blogPost")) {
			String[] str = { "author", "commenter", "contributor" };
			return str;
		}
		if (s.equals("book")) {
			String[] str = { "author", "contributor", "editor", "seriesEditor",
					"translator" };
			return str;
		}
		if (s.equals("bookSection")) {
			String[] str = { "author", "bookAuthor", "contributor", "editor",
					"seriesEditor", "translator" };
			return str;
		}
		if (s.equals("case")) {
			String[] str = { "author", "contributor", "counsel" };
			return str;
		}
		if (s.equals("computerProgram")) {
			String[] str = { "programmer", "contributor" };
			return str;
		}
		if (s.equals("conferencePaper")) {
			String[] str = { "author", "contributor", "editor", "seriesEditor",
					"translator" };
			return str;
		}
		if (s.equals("dictionaryEntry")) {
			String[] str = { "author", "contributor", "editor", "seriesEditor",
					"translator" };
			return str;
		}
		if (s.equals("document")) {
			String[] str = { "author", "contributor", "editor",
					"reviewedAuthor", "translator" };
			return str;
		}
		if (s.equals("email")) {
			String[] str = { "author", "contributor", "recipient" };
			return str;
		}
		if (s.equals("encyclopediaArticle")) {
			String[] str = { "author", "contributor", "editor", "seriesEditor",
					"translator" };
			return str;
		}
		if (s.equals("film")) {
			String[] str = { "director", "contributor", "producer",
					"scriptwriter" };
			return str;
		}
		if (s.equals("forumPost")) {
			String[] str = { "author", "contributor" };
			return str;
		}
		if (s.equals("hearing")) {
			String[] str = { "contributor" };
			return str;
		}
		if (s.equals("instantMessage")) {
			String[] str = { "author", "contributor", "recipient" };
			return str;
		}
		if (s.equals("interview")) {
			String[] str = { "interviewee", "contributor", "interviewer",
					"translator" };
			return str;
		}
		if (s.equals("journalArticle")) {
			String[] str = { "author", "contributor", "editor",
					"reviewedAuthor", "translator" };
			return str;
		}
		if (s.equals("letter")) {
			String[] str = { "author", "contributor", "recipient" };
			return str;
		}
		if (s.equals("magazineArticle")) {
			String[] str = { "author", "contributor", "reviewedAuthor",
					"translator" };
			return str;
		}
		if (s.equals("manuscript")) {
			String[] str = { "author", "contributor", "translator" };
			return str;
		}
		if (s.equals("map")) {
			String[] str = { "cartographer", "contributor", "seriesEditor" };
			return str;
		}
		if (s.equals("newspaperArticle")) {
			String[] str = { "author", "contributor", "reviewedAuthor",
					"translator" };
			return str;
		}
		if (s.equals("patent")) {
			String[] str = { "inventor", "attorneyAgent", "contributor" };
			return str;
		}
		if (s.equals("podcast")) {
			String[] str = { "podcaster", "contributor", "guest" };
			return str;
		}
		if (s.equals("presentation")) {
			String[] str = { "presenter", "contributor" };
			return str;
		}
		if (s.equals("radioBroadcast")) {
			String[] str = { "director", "castMember", "contributor", "guest",
					"producer", "scriptwriter" };
			return str;
		}
		if (s.equals("report")) {
			String[] str = { "author", "contributor", "seriesEditor",
					"translator" };
			return str;
		}
		if (s.equals("statute")) {
			String[] str = { "author", "contributor" };
			return str;
		}
		if (s.equals("tvBroadcast")) {
			String[] str = { "director", "castMember", "contributor", "guest",
					"producer", "scriptwriter" };
			return str;
		}
		if (s.equals("thesis")) {
			String[] str = { "author", "contributor" };
			return str;
		}
		if (s.equals("videoRecording")) {
			String[] str = { "director", "castMember", "contributor",
					"producer", "scriptwriter" };
			return str;
		}
		if (s.equals("webpage")) {
			String[] str = { "author", "contributor", "translator" };
			return str;
		} else {
			return null;
		}
	}

	/**
	 * Gets JSON template for the specified item type.
	 * 
	 * Remapping of fields on item type conversion is a separate issue.
	 * 
	 * @param c
	 *            Current context, needed to fetch string resources with
	 *            templates
	 * @param s
	 *            itemType to provide fields for
	 * @return
	 */
	public static JSONObject fieldsForItemType(Context c, String s) {
		JSONObject template = new JSONObject();
		String json = "";
		try {
			// And item types
			if (s.equals("artwork"))
				json = c.getString(R.string.template_artwork);
			else if (s.equals("audioRecording"))
				json = c.getString(R.string.template_audioRecording);
			else if (s.equals("bill"))
				json = c.getString(R.string.template_bill);
			else if (s.equals("blogPost"))
				json = c.getString(R.string.template_blogPost);
			else if (s.equals("book"))
				json = c.getString(R.string.template_book);
			else if (s.equals("bookSection"))
				json = c.getString(R.string.template_bookSection);
			else if (s.equals("case"))
				json = c.getString(R.string.template_case);
			else if (s.equals("computerProgram"))
				json = c.getString(R.string.template_computerProgram);
			else if (s.equals("conferencePaper"))
				json = c.getString(R.string.template_conferencePaper);
			else if (s.equals("dictionaryEntry"))
				json = c.getString(R.string.template_dictionaryEntry);
			else if (s.equals("document"))
				json = c.getString(R.string.template_document);
			else if (s.equals("email"))
				json = c.getString(R.string.template_email);
			else if (s.equals("encyclopediaArticle"))
				json = c.getString(R.string.template_encyclopediaArticle);
			else if (s.equals("film"))
				json = c.getString(R.string.template_film);
			else if (s.equals("forumPost"))
				json = c.getString(R.string.template_forumPost);
			else if (s.equals("hearing"))
				json = c.getString(R.string.template_hearing);
			else if (s.equals("instantMessage"))
				json = c.getString(R.string.template_instantMessage);
			else if (s.equals("interview"))
				json = c.getString(R.string.template_interview);
			else if (s.equals("journalArticle"))
				json = c.getString(R.string.template_journalArticle);
			else if (s.equals("letter"))
				json = c.getString(R.string.template_letter);
			else if (s.equals("magazineArticle"))
				json = c.getString(R.string.template_magazineArticle);
			else if (s.equals("manuscript"))
				json = c.getString(R.string.template_manuscript);
			else if (s.equals("map"))
				json = c.getString(R.string.template_map);
			else if (s.equals("newspaperArticle"))
				json = c.getString(R.string.template_newspaperArticle);
			else if (s.equals("note"))
				json = c.getString(R.string.template_note);
			else if (s.equals("patent"))
				json = c.getString(R.string.template_patent);
			else if (s.equals("podcast"))
				json = c.getString(R.string.template_podcast);
			else if (s.equals("presentation"))
				json = c.getString(R.string.template_presentation);
			else if (s.equals("radioBroadcast"))
				json = c.getString(R.string.template_radioBroadcast);
			else if (s.equals("report"))
				json = c.getString(R.string.template_report);
			else if (s.equals("statute"))
				json = c.getString(R.string.template_statute);
			else if (s.equals("tvBroadcast"))
				json = c.getString(R.string.template_tvBroadcast);
			else if (s.equals("thesis"))
				json = c.getString(R.string.template_thesis);
			else if (s.equals("videoRecording"))
				json = c.getString(R.string.template_videoRecording);
			else if (s.equals("webpage"))
				json = c.getString(R.string.template_webpage);
			template = new JSONObject(json);
			
			JSONArray names = template.names();
			for (int i = 0; i < names.length(); i++) {
				if (names.getString(i).equals("itemType")) continue;
				if (names.getString(i).equals("tags")) continue;
				if (names.getString(i).equals("notes")) continue;
				if (names.getString(i).equals("creators")) {
					JSONArray roles = template.getJSONArray("creators");
					for (int j = 0; j < roles.length(); j++) {
						roles.put(j, roles.getJSONObject(j).put("firstName", "").put("lastName", ""));
					}
					template.put("creators", roles);
					continue;
				}
				template.put(names.getString(i), "");
			}
			Log.d(TAG, "Got JSON template: "+template.toString(4));

		} catch (JSONException e) {
			Log.e(TAG, "JSON exception parsing item template", e);
		}

		return template;
	}

	public static int sortValueForLabel(String s) {
		// First type, and the bare minimum...
		if (s.equals("itemType"))
			return 0;
		if (s.equals("title"))
			return 1;
		if (s.equals("creators"))
			return 2;
		if (s.equals("date"))
			return 3;

		// Then container titles, with one value
		if (s.equals("publicationTitle"))
			return 5;
		if (s.equals("blogTitle"))
			return 5;
		if (s.equals("bookTitle"))
			return 5;
		if (s.equals("dictionaryTitle"))
			return 5;
		if (s.equals("encyclopediaTitle"))
			return 5;
		if (s.equals("forumTitle"))
			return 5;
		if (s.equals("proceedingsTitle"))
			return 5;
		if (s.equals("programTitle"))
			return 5;
		if (s.equals("websiteTitle"))
			return 5;
		if (s.equals("meetingName"))
			return 5;

		// Abstracts
		if (s.equals("abstractNote"))
			return 10;

		// Publishing data
		if (s.equals("publisher"))
			return 12;
		if (s.equals("place"))
			return 13;

		// Series, publication numbers
		if (s.equals("pages"))
			return 14;
		if (s.equals("numPages"))
			return 16;
		if (s.equals("series"))
			return 16;
		if (s.equals("seriesTitle"))
			return 17;
		if (s.equals("seriesText"))
			return 18;
		if (s.equals("volume"))
			return 19;
		if (s.equals("numberOfVolumes"))
			return 20;
		if (s.equals("issue"))
			return 20;
		if (s.equals("section"))
			return 21;
		if (s.equals("publicationNumber"))
			return 22;
		if (s.equals("edition"))
			return 23;

		// Locators
		if (s.equals("DOI"))
			return 50;
		if (s.equals("archive"))
			return 51;
		if (s.equals("archiveLocation"))
			return 52;
		if (s.equals("ISBN"))
			return 53;
		if (s.equals("ISSN"))
			return 54;
		if (s.equals("libraryCatalog"))
			return 55;
		if (s.equals("callNumber"))
			return 56;
		
		// Housekeeping and navigation, at the very end
		if (s.equals("attachments"))
			return 250;
		if (s.equals("tags"))
			return 251;
		if (s.equals("related"))
			return 252;
		
		return 200;
	}
}
