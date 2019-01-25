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

import com.gimranov.zandy.app.ItemDisplayUtil;
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

    private static final String TAG = Item.class.getSimpleName();

    /**
     * The next two types are arrays of information on items that we need
     * elsewhere
     */
    public static final String[] ITEM_TYPES_EN = {"Artwork",
            "Audio Recording", "Bill", "Blog Post", "Book", "Book Section",
            "Case", "Computer Program", "Conference Paper", "Dictionary Entry",
            "Document", "E-mail", "Encyclopedia Article", "Film", "Forum Post",
            "Hearing", "Instant Message", "Interview", "Journal Article",
            "Letter", "Magazine Article", "Manuscript", "Map",
            "Newspaper Article", "Note", "Patent", "Podcast", "Presentation",
            "Radio Broadcast", "Report", "Statute", "TV Broadcast", "Thesis",
            "Video Recording", "Web Page"};

    public static final String[] ITEM_TYPES = {"artwork", "audioRecording",
            "bill", "blogPost", "book", "bookSection", "case",
            "computerProgram", "conferencePaper", "dictionaryEntry",
            "document", "email", "encyclopediaArticle", "film", "forumPost",
            "hearing", "instantMessage", "interview", "journalArticle",
            "letter", "magazineArticle", "manuscript", "map",
            "newspaperArticle", "note", "patent", "podcast", "presentation",
            "radioBroadcast", "report", "statute", "tvBroadcast", "thesis",
            "videoRecording", "webpage"};

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
        key = "zandy:" + UUID.randomUUID().toString();
        dirty = APIRequest.API_NEW;
        this.type = type;
    }

    public boolean equals(Item b) {
        if (b == null) return false;
        Log.d(TAG, "Comparing myself (" + key + ") to " + b.key);
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
				    b.putString("content", String.valueOf(ItemDisplayUtil.INSTANCE.formatCreatorList(values.getJSONArray(i))));
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
                                - (mt2 ? 300 : 0)
                                + (mt1 ? 300 : 0));
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
            String[] args = {title, key, type, year, creatorSummary,
                    content.toString(), etag, dirty, timestamp, children};
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
            String[] args = {title, type, year, creatorSummary,
                    content.toString(), etag, dirty, timestamp, key, children, dbId};
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
        String[] args = {dbId};
        db.rawQuery("delete from items where _id=?", args);
        db.rawQuery("delete from itemtocreators where item_id=?", args);
        db.rawQuery("delete from itemtocollections where item_id=?", args);
        ArrayList<Attachment> atts = Attachment.forItem(this, db);
        for (Attachment a : atts) {
            a.delete(db);
        }
        // Don't prepare deletion requests for unsynced new items
        if (!APIRequest.API_NEW.equals(dirty)) {
            String[] args2 = {key, etag};
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
        String[] args = {itemKey};
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
        String[] args = {itemDbId};
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
     * <p>
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

        switch (label) {
            case "title":
                item.title = content;
                break;
            case "itemType":
                item.type = content;
                break;
            case "children":
                item.children = content;
                break;
            case "date":
                item.year = content.replaceAll("^.*?(\\d{4}).*$", "$1");
                break;
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
     * <p>
     * If oldTag is empty or null, the new tag is appended.
     * <p>
     * We make it into a user tag, which the desktop client does as well.
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
            Log.e(TAG, "Caught JSON exception when we tried to modify the JSON content", e);
        }
        item.dirty = APIRequest.API_DIRTY;
        item.save(db);
    }

    /**
     * Static method for setting creators
     * <p>
     * Add, change, or replace a creator. If creator is null, the old creator is
     * simply deleted.
     * <p>
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
                sb.append(((JSONObject) creators.get(j)).optString("lastName", ""));
            }
            item.creatorSummary = sb.toString();
            item.content.put("creators", newCreators);
            Log.d(TAG, "New: " + newCreators.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Caught JSON exception when we tried to modify the JSON content");
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
        String[] args = {APIRequest.API_CLEAN};
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
        } while (cur.moveToNext());

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

        switch (type) {
            case "artwork":
                return R.drawable.picture;
            // if (type.equals("audioRecording")) return
            // R.drawable.ic_menu_close_clear_cancel;
            // if (type.equals("bill")) return
            // R.drawable.ic_menu_close_clear_cancel;
            // if (type.equals("blogPost")) return
            // R.drawable.ic_menu_close_clear_cancel;
            case "book":
                return R.drawable.book;
            case "bookSection":
                return R.drawable.book_open;
            // if (type.equals("case")) return
            // R.drawable.ic_menu_close_clear_cancel;
            // if (type.equals("computerProgram")) return
            // R.drawable.ic_menu_close_clear_cancel;
            // if (type.equals("conferencePaper")) return
            // R.drawable.ic_menu_close_clear_cancel;
            case "dictionaryEntry":
                return R.drawable.page_white_width;
            case "document":
                return R.drawable.page_white;
            // if (type.equals("email")) return
            // R.drawable.ic_menu_close_clear_cancel;
            case "encyclopediaArticle":
                return R.drawable.page_white_text_width;
            case "film":
                return R.drawable.film;
            // if (type.equals("forumPost")) return
            // R.drawable.ic_menu_close_clear_cancel;
            // if (type.equals("hearing")) return
            // R.drawable.ic_menu_close_clear_cancel;
            case "instantMessage":
                return R.drawable.comment;
            // if (type.equals("interview")) return
            // R.drawable.ic_menu_close_clear_cancel;
            case "journalArticle":
                return R.drawable.page_white_text;
            case "letter":
                return R.drawable.email;
            case "magazineArticle":
                return R.drawable.layout;
            case "manuscript":
                return R.drawable.script;
            case "map":
                return R.drawable.map;
            case "newspaperArticle":
                return R.drawable.newspaper;
            // if (type.equals("patent")) return
            // R.drawable.ic_menu_close_clear_cancel;
            // if (type.equals("podcast")) return
            // R.drawable.ic_menu_close_clear_cancel;
            case "presentation":
                return R.drawable.page_white_powerpoint;
            // if (type.equals("radioBroadcast")) return
            // R.drawable.ic_menu_close_clear_cancel;
            case "report":
                return R.drawable.report;
            // if (type.equals("statute")) return
            // R.drawable.ic_menu_close_clear_cancel;
            case "thesis":
                return R.drawable.report_user;
            case "tvBroadcast":
                return R.drawable.television;
            case "videoRecording":
                return R.drawable.film;
            case "webpage":
                return R.drawable.page;

            // Not item types, but still something
            case "collection":
                return R.drawable.folder;
            case "application/pdf":
                return R.drawable.page_white_acrobat;
            case "note":
                return R.drawable.note;
            // if (type.equals("snapshot")) return
            // R.drawable.ic_menu_close_clear_cancel;
            // if (type.equals("link")) return
            // R.drawable.ic_menu_close_clear_cancel;

            // Return something generic if all else fails
            default:
                return R.drawable.page_white;
        }
    }

    /**
     * Provides the human-readable equivalent for strings
     * <p>
     * TODO This should pull the data from the API as a fallback, but we will
     * hard-code the list for now
     *
     * @param s
     * @return
     */
    // XXX i18n
    public static String localizedStringForString(String s) {
        if (s == null) {
            Log.e(TAG, "Received null string in localizedStringForString");
            return "";
        }
        // Item fields from the API
        switch (s) {
            case "numPages":
                return "# of Pages";
            case "numberOfVolumes":
                return "# of Volumes";
            case "abstractNote":
                return "Abstract";
            case "accessDate":
                return "Accessed";
            case "applicationNumber":
                return "Application Number";
            case "archive":
                return "Archive";
            case "artworkSize":
                return "Artwork Size";
            case "assignee":
                return "Assignee";
            case "billNumber":
                return "Bill Number";
            case "blogTitle":
                return "Blog Title";
            case "bookTitle":
                return "Book Title";
            case "callNumber":
                return "Call Number";
            case "caseName":
                return "Case Name";
            case "code":
                return "Code";
            case "codeNumber":
                return "Code Number";
            case "codePages":
                return "Code Pages";
            case "codeVolume":
                return "Code Volume";
            case "committee":
                return "Committee";
            case "company":
                return "Company";
            case "conferenceName":
                return "Conference Name";
            case "country":
                return "Country";
            case "court":
                return "Court";
            case "DOI":
                return "DOI";
            case "date":
                return "Date";
            case "dateDecided":
                return "Date Decided";
            case "dateEnacted":
                return "Date Enacted";
            case "dictionaryTitle":
                return "Dictionary Title";
            case "distributor":
                return "Distributor";
            case "docketNumber":
                return "Docket Number";
            case "documentNumber":
                return "Document Number";
            case "edition":
                return "Edition";
            case "encyclopediaTitle":
                return "Encyclopedia Title";
            case "episodeNumber":
                return "Episode Number";
            case "extra":
                return "Extra";
            case "audioFileType":
                return "File Type";
            case "filingDate":
                return "Filing Date";
            case "firstPage":
                return "First Page";
            case "audioRecordingFormat":
                return "Format";
            case "videoRecordingFormat":
                return "Format";
            case "forumTitle":
                return "Forum/Listserv Title";
            case "genre":
                return "Genre";
            case "history":
                return "History";
            case "ISBN":
                return "ISBN";
            case "ISSN":
                return "ISSN";
            case "institution":
                return "Institution";
            case "issue":
                return "Issue";
            case "issueDate":
                return "Issue Date";
            case "issuingAuthority":
                return "Issuing Authority";
            case "journalAbbreviation":
                return "Journal Abbr";
            case "label":
                return "Label";
            case "language":
                return "Language";
            case "programmingLanguage":
                return "Language";
            case "legalStatus":
                return "Legal Status";
            case "legislativeBody":
                return "Legislative Body";
            case "libraryCatalog":
                return "Library Catalog";
            case "archiveLocation":
                return "Loc. in Archive";
            case "interviewMedium":
                return "Medium";
            case "artworkMedium":
                return "Medium";
            case "meetingName":
                return "Meeting Name";
            case "nameOfAct":
                return "Name of Act";
            case "network":
                return "Network";
            case "pages":
                return "Pages";
            case "patentNumber":
                return "Patent Number";
            case "place":
                return "Place";
            case "postType":
                return "Post Type";
            case "priorityNumbers":
                return "Priority Numbers";
            case "proceedingsTitle":
                return "Proceedings Title";
            case "programTitle":
                return "Program Title";
            case "publicLawNumber":
                return "Public Law Number";
            case "publicationTitle":
                return "Publication";
            case "publisher":
                return "Publisher";
            case "references":
                return "References";
            case "reportNumber":
                return "Report Number";
            case "reportType":
                return "Report Type";
            case "reporter":
                return "Reporter";
            case "reporterVolume":
                return "Reporter Volume";
            case "rights":
                return "Rights";
            case "runningTime":
                return "Running Time";
            case "scale":
                return "Scale";
            case "section":
                return "Section";
            case "series":
                return "Series";
            case "seriesNumber":
                return "Series Number";
            case "seriesText":
                return "Series Text";
            case "seriesTitle":
                return "Series Title";
            case "session":
                return "Session";
            case "shortTitle":
                return "Short Title";
            case "studio":
                return "Studio";
            case "subject":
                return "Subject";
            case "system":
                return "System";
            case "title":
                return "Title";
            case "thesisType":
                return "Type";
            case "mapType":
                return "Type";
            case "manuscriptType":
                return "Type";
            case "letterType":
                return "Type";
            case "presentationType":
                return "Type";
            case "url":
                return "URL";
            case "university":
                return "University";
            case "version":
                return "Version";
            case "volume":
                return "Volume";
            case "websiteTitle":
                return "Website Title";
            case "websiteType":
                return "Website Type";

            // Item fields not in the API
            case "creators":
                return "Creators";
            case "tags":
                return "Tags";
            case "itemType":
                return "Item Type";
            case "children":
                return "Attachments";
            case "collections":
                return "Collections";

            // And item types
            case "artwork":
                return "Artwork";
            case "audioRecording":
                return "Audio Recording";
            case "bill":
                return "Bill";
            case "blogPost":
                return "Blog Post";
            case "book":
                return "Book";
            case "bookSection":
                return "Book Section";
            case "case":
                return "Case";
            case "computerProgram":
                return "Computer Program";
            case "conferencePaper":
                return "Conference Paper";
            case "dictionaryEntry":
                return "Dictionary Entry";
            case "document":
                return "Document";
            case "email":
                return "E-mail";
            case "encyclopediaArticle":
                return "Encyclopedia Article";
            case "film":
                return "Film";
            case "forumPost":
                return "Forum Post";
            case "hearing":
                return "Hearing";
            case "instantMessage":
                return "Instant Message";
            case "interview":
                return "Interview";
            case "journalArticle":
                return "Journal Article";
            case "letter":
                return "Letter";
            case "magazineArticle":
                return "Magazine Article";
            case "manuscript":
                return "Manuscript";
            case "map":
                return "Map";
            case "newspaperArticle":
                return "Newspaper Article";
            case "note":
                return "Note";
            case "patent":
                return "Patent";
            case "podcast":
                return "Podcast";
            case "presentation":
                return "Presentation";
            case "radioBroadcast":
                return "Radio Broadcast";
            case "report":
                return "Report";
            case "statute":
                return "Statute";
            case "tvBroadcast":
                return "TV Broadcast";
            case "thesis":
                return "Thesis";
            case "videoRecording":
                return "Video Recording";
            case "webpage":
                return "Web Page";

            // Creator types
            case "artist":
                return "Artist";
            case "attorneyAgent":
                return "Attorney/Agent";
            case "author":
                return "Author";
            case "bookAuthor":
                return "Book Author";
            case "cartographer":
                return "Cartographer";
            case "castMember":
                return "Cast Member";
            case "commenter":
                return "Commenter";
            case "composer":
                return "Composer";
            case "contributor":
                return "Contributor";
            case "cosponsor":
                return "Cosponsor";
            case "counsel":
                return "Counsel";
            case "director":
                return "Director";
            case "editor":
                return "Editor";
            case "guest":
                return "Guest";
            case "interviewee":
                return "Interview With";
            case "interviewer":
                return "Interviewer";
            case "inventor":
                return "Inventor";
            case "performer":
                return "Performer";
            case "podcaster":
                return "Podcaster";
            case "presenter":
                return "Presenter";
            case "producer":
                return "Producer";
            case "programmer":
                return "Programmer";
            case "recipient":
                return "Recipient";
            case "reviewedAuthor":
                return "Reviewed Author";
            case "scriptwriter":
                return "Scriptwriter";
            case "seriesEditor":
                return "Series Editor";
            case "sponsor":
                return "Sponsor";
            case "translator":
                return "Translator";
            case "wordsBy":
                return "Words By";

            // Or just leave it the way it is
            default:
                return s;
        }
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
     * <p>
     * Remapping of creator types on item type conversion is a separate issue.
     *
     * @param s itemType to provide creatorTypes for
     * @return
     */
    public static String[] creatorTypesForItemType(String s) {
        Log.d(TAG, "Getting creator types for item type: " + s);
        switch (s) {
            case "artwork":
                return new String[]{"artist", "contributor"};
            case "audioRecording":
                return new String[]{"performer", "composer", "contributor", "wordsBy"};
            case "bill":
                return new String[]{"sponsor", "contributor", "cosponsor"};
            case "blogPost":
                return new String[]{"author", "commenter", "contributor"};
            case "book":
                return new String[]{"author", "contributor", "editor", "seriesEditor",
                        "translator"};
            case "bookSection":
                return new String[]{"author", "bookAuthor", "contributor", "editor",
                        "seriesEditor", "translator"};
            case "case":
                return new String[]{"author", "contributor", "counsel"};
            case "computerProgram":
                return new String[]{"programmer", "contributor"};
            case "conferencePaper":
                return new String[]{"author", "contributor", "editor", "seriesEditor",
                        "translator"};
            case "dictionaryEntry":
                return new String[]{"author", "contributor", "editor", "seriesEditor",
                        "translator"};
            case "document":
                return new String[]{"author", "contributor", "editor",
                        "reviewedAuthor", "translator"};
            case "email":
                return new String[]{"author", "contributor", "recipient"};
            case "encyclopediaArticle":
                return new String[]{"author", "contributor", "editor", "seriesEditor",
                        "translator"};
            case "film":
                return new String[]{"director", "contributor", "producer",
                        "scriptwriter"};
            case "forumPost":
                return new String[]{"author", "contributor"};
            case "hearing":
                return new String[]{"contributor"};
            case "instantMessage":
                return new String[]{"author", "contributor", "recipient"};
            case "interview":
                return new String[]{"interviewee", "contributor", "interviewer",
                        "translator"};
            case "journalArticle":
                return new String[]{"author", "contributor", "editor",
                        "reviewedAuthor", "translator"};
            case "letter":
                return new String[]{"author", "contributor", "recipient"};
            case "magazineArticle":
                return new String[]{"author", "contributor", "reviewedAuthor",
                        "translator"};
            case "manuscript":
                return new String[]{"author", "contributor", "translator"};
            case "map":
                return new String[]{"cartographer", "contributor", "seriesEditor"};
            case "newspaperArticle":
                return new String[]{"author", "contributor", "reviewedAuthor",
                        "translator"};
            case "patent":
                return new String[]{"inventor", "attorneyAgent", "contributor"};
            case "podcast":
                return new String[]{"podcaster", "contributor", "guest"};
            case "presentation":
                return new String[]{"presenter", "contributor"};
            case "radioBroadcast":
                return new String[]{"director", "castMember", "contributor", "guest",
                        "producer", "scriptwriter"};
            case "report":
                return new String[]{"author", "contributor", "seriesEditor",
                        "translator"};
            case "statute":
                return new String[]{"author", "contributor"};
            case "tvBroadcast":
                return new String[]{"director", "castMember", "contributor", "guest",
                        "producer", "scriptwriter"};
            case "thesis":
                return new String[]{"author", "contributor"};
            case "videoRecording":
                return new String[]{"director", "castMember", "contributor",
                        "producer", "scriptwriter"};
            case "webpage":
                return new String[]{"author", "contributor", "translator"};
            default:
                return null;
        }
    }

    /**
     * Gets JSON template for the specified item type.
     * <p>
     * Remapping of fields on item type conversion is a separate issue.
     *
     * @param c Current context, needed to fetch string resources with
     *          templates
     * @param s itemType to provide fields for
     * @return
     */
    private static JSONObject fieldsForItemType(Context c, String s) {
        JSONObject template = new JSONObject();
        String json = "";
        try {
            // And item types
            switch (s) {
                case "artwork":
                    json = c.getString(R.string.template_artwork);
                    break;
                case "audioRecording":
                    json = c.getString(R.string.template_audioRecording);
                    break;
                case "bill":
                    json = c.getString(R.string.template_bill);
                    break;
                case "blogPost":
                    json = c.getString(R.string.template_blogPost);
                    break;
                case "book":
                    json = c.getString(R.string.template_book);
                    break;
                case "bookSection":
                    json = c.getString(R.string.template_bookSection);
                    break;
                case "case":
                    json = c.getString(R.string.template_case);
                    break;
                case "computerProgram":
                    json = c.getString(R.string.template_computerProgram);
                    break;
                case "conferencePaper":
                    json = c.getString(R.string.template_conferencePaper);
                    break;
                case "dictionaryEntry":
                    json = c.getString(R.string.template_dictionaryEntry);
                    break;
                case "document":
                    json = c.getString(R.string.template_document);
                    break;
                case "email":
                    json = c.getString(R.string.template_email);
                    break;
                case "encyclopediaArticle":
                    json = c.getString(R.string.template_encyclopediaArticle);
                    break;
                case "film":
                    json = c.getString(R.string.template_film);
                    break;
                case "forumPost":
                    json = c.getString(R.string.template_forumPost);
                    break;
                case "hearing":
                    json = c.getString(R.string.template_hearing);
                    break;
                case "instantMessage":
                    json = c.getString(R.string.template_instantMessage);
                    break;
                case "interview":
                    json = c.getString(R.string.template_interview);
                    break;
                case "journalArticle":
                    json = c.getString(R.string.template_journalArticle);
                    break;
                case "letter":
                    json = c.getString(R.string.template_letter);
                    break;
                case "magazineArticle":
                    json = c.getString(R.string.template_magazineArticle);
                    break;
                case "manuscript":
                    json = c.getString(R.string.template_manuscript);
                    break;
                case "map":
                    json = c.getString(R.string.template_map);
                    break;
                case "newspaperArticle":
                    json = c.getString(R.string.template_newspaperArticle);
                    break;
                case "note":
                    json = c.getString(R.string.template_note);
                    break;
                case "patent":
                    json = c.getString(R.string.template_patent);
                    break;
                case "podcast":
                    json = c.getString(R.string.template_podcast);
                    break;
                case "presentation":
                    json = c.getString(R.string.template_presentation);
                    break;
                case "radioBroadcast":
                    json = c.getString(R.string.template_radioBroadcast);
                    break;
                case "report":
                    json = c.getString(R.string.template_report);
                    break;
                case "statute":
                    json = c.getString(R.string.template_statute);
                    break;
                case "tvBroadcast":
                    json = c.getString(R.string.template_tvBroadcast);
                    break;
                case "thesis":
                    json = c.getString(R.string.template_thesis);
                    break;
                case "videoRecording":
                    json = c.getString(R.string.template_videoRecording);
                    break;
                case "webpage":
                    json = c.getString(R.string.template_webpage);
                    break;
            }
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
            Log.d(TAG, "Got JSON template: " + template.toString(4));

        } catch (JSONException e) {
            Log.e(TAG, "JSON exception parsing item template", e);
        }

        return template;
    }

    public static int sortValueForLabel(String s) {
        // First type, and the bare minimum...
        switch (s) {
            case "itemType":
                return 0;
            case "title":
                return 1;
            case "creators":
                return 2;
            case "date":
                return 3;


            // Then container titles, with one value
            case "publicationTitle":
                return 5;
            case "blogTitle":
                return 5;
            case "bookTitle":
                return 5;
            case "dictionaryTitle":
                return 5;
            case "encyclopediaTitle":
                return 5;
            case "forumTitle":
                return 5;
            case "proceedingsTitle":
                return 5;
            case "programTitle":
                return 5;
            case "websiteTitle":
                return 5;
            case "meetingName":
                return 5;

            // Abstracts
            case "abstractNote":
                return 10;

            // Publishing data
            case "publisher":
                return 12;
            case "place":
                return 13;

            // Series, publication numbers
            case "pages":
                return 14;
            case "numPages":
                return 16;
            case "series":
                return 16;
            case "seriesTitle":
                return 17;
            case "seriesText":
                return 18;
            case "volume":
                return 19;
            case "numberOfVolumes":
                return 20;
            case "issue":
                return 20;
            case "section":
                return 21;
            case "publicationNumber":
                return 22;
            case "edition":
                return 23;

            // Locators
            case "DOI":
                return 50;
            case "archive":
                return 51;
            case "archiveLocation":
                return 52;
            case "ISBN":
                return 53;
            case "ISSN":
                return 54;
            case "libraryCatalog":
                return 55;
            case "callNumber":
                return 56;

            // Housekeeping and navigation, at the very end
            case "attachments":
                return 250;
            case "tags":
                return 251;
            case "related":
                return 252;
            default:
                return 200;
        }
    }
}
