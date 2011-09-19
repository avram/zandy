package org.zotero.client.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.zotero.client.R;
import org.zotero.client.task.APIRequest;

import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;

public class Item  {
	private String id;
	private String title;
	private String type;
	private String owner;
	private String key;
	private String etag;
	// TODO We don't know how to update this either
	private String year;
	// TODO We don't know how to update creatorSummary
	private String creatorSummary;
	private JSONObject content;
	
	public String dbId;
	
	/**
	 * Queue of dirty items to be sent to the server
	 */
	public static ArrayList<Item> queue = new ArrayList<Item>();
	
	private static final String TAG = "org.zotero.client.data.Item";
	
	/**
	 * The next two types are arrays of information on items that we need elsewhere
	 */
	public static final String[] ITEM_TYPES_EN =
	{"Artwork",
		"Audio Recording",
		"Bill",
		"Blog Post",
		"Book",
		"Book Section",
		"Case",
		"Computer Program",
		"Conference Paper",
		"Dictionary Entry",
		"Document",
		"E-mail",
		"Encyclopedia Article",
		"Film",
		"Forum Post",
		"Hearing",
		"Instant Message",
		"Interview",
		"Journal Article",
		"Letter",
		"Magazine Article",
		"Manuscript",
		"Map",
		"Newspaper Article",
		"Note",
		"Patent",
		"Podcast",
		"Presentation",
		"Radio Broadcast",
		"Report",
		"Statute",
		"TV Broadcast",
		"Thesis",
		"Video Recording",
		"Web Page" };
	
	public static final String[] ITEM_TYPES =
	{"artwork",
		"audioRecording",
		"bill",
		"blogPost",
		"book",
		"bookSection",
		"case",
		"computerProgram",
		"conferencePaper",
		"dictionaryEntry",
		"document",
		"email",
		"encyclopediaArticle",
		"film",
		"forumPost",
		"hearing",
		"instantMessage",
		"interview",
		"journalArticle",
		"letter",
		"magazineArticle",
		"manuscript",
		"map",
		"newspaperArticle",
		"note",
		"patent",
		"podcast",
		"presentation",
		"radioBroadcast",
		"report",
		"statute",
		"tvBroadcast",
		"thesis",
		"videoRecording",
		"webpage"};
	
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
	
	/**
	 * Makes ArrayList<Bundle> from the present item
	 * This was moved from ItemDataActivity, but it's most likely to
	 * be used by such display activities
	 */
	public ArrayList<Bundle> toBundleArray() {
		JSONObject itemContent = this.content;
	    /* Here we walk through the data and make Bundles to send to the
	     * ArrayAdapter. There should be no real risk of JSON exceptions,
	     * since the JSON was checked when initialized in the Item object.
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
	        		try {
	        			JSONArray tagArray = values.getJSONArray(i);
	        			b.putString("content", tagArray.join("; "));
	        		} catch (JSONException e) {
	        			// Fall back to empty
	        			b.putString("content", "");
	        		}
	        	} else if (fields.getString(i).equals("creators")) {
	        		/* Creators should be labeled with role and listed nicely
	        		 * This logic isn't as good as it could be. */
	    			JSONArray creatorArray = values.getJSONArray(i);
	    			JSONObject creator;
	        		StringBuilder sb = new StringBuilder();
	        		for (int j = 0; j < creatorArray.length(); j++) {
	        			creator = creatorArray.getJSONObject(j);
	        			if (creator.getString("creatorType").equals("author")) {
	        				if (creator.has("name")) sb.append(creator.getString("name"));
	        				else sb.append(creator.getString("firstName") + " " + creator.getString("lastName"));
	        			} else {
	        				if (creator.has("name")) sb.append(creator.getString("name"));
	        				else sb.append(creator.getString("firstName") 
	        						+ " " + creator.getString("lastName")
	        						+ " (" + creator.getString("creatorType") + ")");
	        			}
	        			if (j < creatorArray.length() - 1) sb.append(", ");
	        		}
	        		b.putString("content", sb.toString());
	        	} else if (fields.getString(i).equals("itemType")) {
	        		// We want to show the localized or human-readable type
	        		b.putString("content", Item.localizedStringForString(values.getString(i)));
	        	} else {
	        		// All other data is treated as just text
		        	b.putString("content", values.getString(i));
	        	}
	        	b.putString("label", fields.getString(i));
	        	b.putString("itemKey", getKey());
	        	rows.add(b);
	        }
	    } catch (JSONException e) {
	    	/* 
	    	 * We could die here, but I'd rather not, since this shouldn't 
	    	 * be possible.
	    	 */
	    	Log.e(TAG, "JSON parse exception making bundles!",e);
	    }
	    
	    /* We'd like to put these in a certain order, so let's try! */
	    Collections.sort(rows, new Comparator<Bundle>() {
	    	@Override
	    	public int compare(Bundle b1, Bundle b2) {
	    		return Item.sortValueForLabel(b1.getString("label")) 
	    			- Item.sortValueForLabel(b2.getString("label"));
	    	}
	    });
	    return rows;
	}
	
	/**
	 * Saves the item's current state. Marking dirty should happen before this
	 */
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
	
	/**
	 * Static method for modification of items in the database
	 */
	public static void set(String itemKey, String label, String content) {
		// Load the item
		Item item = load(itemKey);
		if (label.equals("title")) {
			item.title = content;
		}
		
		if (label.equals("itemType")) {
			item.type = content;
		}
		
		try {
			item.content.put(label, content);
		} catch (JSONException e) {
			Log.e(TAG, "Caught JSON exception when we tried to modify the JSON content");
		}
		item.dirty = APIRequest.API_DIRTY;
		item.save();
	}
	
	/**
	 * Identifies dirty items in the database and queues them for syncing
	 */
	public static void queue() {
		Log.d(TAG,"Clearing item dirty queue before repopulation");		
		queue.clear();
		Item item;
		String[] cols = Database.ITEMCOLS;
		String[] args = { APIRequest.API_CLEAN };
		Cursor cur = db.query("items", cols, "dirty!=?", args, null, null, null, null);
		
		if (cur == null) {
			Log.d(TAG,"No dirty items found in database");
			queue.clear();
			return;
		}
		
		do {
			Log.d(TAG,"Adding item to dirty queue");
			item = load(cur);
			queue.add(item);
		} while (cur.moveToNext() != false);
		
		if (cur != null) cur.close();
	}
	
	/**
	 * Maps types to the resources providing images for them.
	 * 
	 * @param type
	 * @return	A resource representing an image for the item or other type
	 */
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

	/**
	 * Provides the human-readable equivalent for strings
	 * 
	 * TODO This should pull the data from the API as a fallback,
	 * but we will hard-code the list for now
	 * @param s
	 * @return
	 */
	public static String localizedStringForString(String s) {
		// Item fields from the API
		if (s.equals("numPages")) return "# of Pages";
		if (s.equals("numberOfVolumes")) return "# of Volumes";
		if (s.equals("abstractNote")) return "Abstract";
		if (s.equals("accessDate")) return "Accessed";
		if (s.equals("applicationNumber")) return "Application Number";
		if (s.equals("archive")) return "Archive";
		if (s.equals("artworkSize")) return "Artwork Size";
		if (s.equals("assignee")) return "Assignee";
		if (s.equals("billNumber")) return "Bill Number";
		if (s.equals("blogTitle")) return "Blog Title";
		if (s.equals("bookTitle")) return "Book Title";
		if (s.equals("callNumber")) return "Call Number";
		if (s.equals("caseName")) return "Case Name";
		if (s.equals("code")) return "Code";
		if (s.equals("codeNumber")) return "Code Number";
		if (s.equals("codePages")) return "Code Pages";
		if (s.equals("codeVolume")) return "Code Volume";
		if (s.equals("committee")) return "Committee";
		if (s.equals("company")) return "Company";
		if (s.equals("conferenceName")) return "Conference Name";
		if (s.equals("country")) return "Country";
		if (s.equals("court")) return "Court";
		if (s.equals("DOI")) return "DOI";
		if (s.equals("date")) return "Date";
		if (s.equals("dateDecided")) return "Date Decided";
		if (s.equals("dateEnacted")) return "Date Enacted";
		if (s.equals("dictionaryTitle")) return "Dictionary Title";
		if (s.equals("distributor")) return "Distributor";
		if (s.equals("docketNumber")) return "Docket Number";
		if (s.equals("documentNumber")) return "Document Number";
		if (s.equals("edition")) return "Edition";
		if (s.equals("encyclopediaTitle")) return "Encyclopedia Title";
		if (s.equals("episodeNumber")) return "Episode Number";
		if (s.equals("extra")) return "Extra";
		if (s.equals("audioFileType")) return "File Type";
		if (s.equals("filingDate")) return "Filing Date";
		if (s.equals("firstPage")) return "First Page";
		if (s.equals("audioRecordingFormat")) return "Format";
		if (s.equals("videoRecordingFormat")) return "Format";
		if (s.equals("forumTitle")) return "Forum/Listserv Title";
		if (s.equals("genre")) return "Genre";
		if (s.equals("history")) return "History";
		if (s.equals("ISBN")) return "ISBN";
		if (s.equals("ISSN")) return "ISSN";
		if (s.equals("institution")) return "Institution";
		if (s.equals("issue")) return "Issue";
		if (s.equals("issueDate")) return "Issue Date";
		if (s.equals("issuingAuthority")) return "Issuing Authority";
		if (s.equals("journalAbbreviation")) return "Journal Abbr";
		if (s.equals("label")) return "Label";
		if (s.equals("language")) return "Language";
		if (s.equals("programmingLanguage")) return "Language";
		if (s.equals("legalStatus")) return "Legal Status";
		if (s.equals("legislativeBody")) return "Legislative Body";
		if (s.equals("libraryCatalog")) return "Library Catalog";
		if (s.equals("archiveLocation")) return "Loc. in Archive";
		if (s.equals("interviewMedium")) return "Medium";
		if (s.equals("artworkMedium")) return "Medium";
		if (s.equals("meetingName")) return "Meeting Name";
		if (s.equals("nameOfAct")) return "Name of Act";
		if (s.equals("network")) return "Network";
		if (s.equals("pages")) return "Pages";
		if (s.equals("patentNumber")) return "Patent Number";
		if (s.equals("place")) return "Place";
		if (s.equals("postType")) return "Post Type";
		if (s.equals("priorityNumbers")) return "Priority Numbers";
		if (s.equals("proceedingsTitle")) return "Proceedings Title";
		if (s.equals("programTitle")) return "Program Title";
		if (s.equals("publicLawNumber")) return "Public Law Number";
		if (s.equals("publicationTitle")) return "Publication";
		if (s.equals("publisher")) return "Publisher";
		if (s.equals("references")) return "References";
		if (s.equals("reportNumber")) return "Report Number";
		if (s.equals("reportType")) return "Report Type";
		if (s.equals("reporter")) return "Reporter";
		if (s.equals("reporterVolume")) return "Reporter Volume";
		if (s.equals("rights")) return "Rights";
		if (s.equals("runningTime")) return "Running Time";
		if (s.equals("scale")) return "Scale";
		if (s.equals("section")) return "Section";
		if (s.equals("series")) return "Series";
		if (s.equals("seriesNumber")) return "Series Number";
		if (s.equals("seriesText")) return "Series Text";
		if (s.equals("seriesTitle")) return "Series Title";
		if (s.equals("session")) return "Session";
		if (s.equals("shortTitle")) return "Short Title";
		if (s.equals("studio")) return "Studio";
		if (s.equals("subject")) return "Subject";
		if (s.equals("system")) return "System";
		if (s.equals("title")) return "Title";
		if (s.equals("thesisType")) return "Type";
		if (s.equals("mapType")) return "Type";
		if (s.equals("manuscriptType")) return "Type";
		if (s.equals("letterType")) return "Type";
		if (s.equals("presentationType")) return "Type";
		if (s.equals("url")) return "URL";
		if (s.equals("university")) return "University";
		if (s.equals("version")) return "Version";
		if (s.equals("volume")) return "Volume";
		if (s.equals("websiteTitle")) return "Website Title";
		if (s.equals("websiteType")) return "Website Type";
		
		// Item fields not in the API
		if (s.equals("creators")) return "Creators";
		if (s.equals("tags")) return "Tags";
		if (s.equals("itemType")) return "Item Type";
		
		// And item types
		if (s.equals("artwork")) return "Artwork";
		if (s.equals("audioRecording")) return "Audio Recording";
		if (s.equals("bill")) return "Bill";
		if (s.equals("blogPost")) return "Blog Post";
		if (s.equals("book")) return "Book";
		if (s.equals("bookSection")) return "Book Section";
		if (s.equals("case")) return "Case";
		if (s.equals("computerProgram")) return "Computer Program";
		if (s.equals("conferencePaper")) return "Conference Paper";
		if (s.equals("dictionaryEntry")) return "Dictionary Entry";
		if (s.equals("document")) return "Document";
		if (s.equals("email")) return "E-mail";
		if (s.equals("encyclopediaArticle")) return "Encyclopedia Article";
		if (s.equals("film")) return "Film";
		if (s.equals("forumPost")) return "Forum Post";
		if (s.equals("hearing")) return "Hearing";
		if (s.equals("instantMessage")) return "Instant Message";
		if (s.equals("interview")) return "Interview";
		if (s.equals("journalArticle")) return "Journal Article";
		if (s.equals("letter")) return "Letter";
		if (s.equals("magazineArticle")) return "Magazine Article";
		if (s.equals("manuscript")) return "Manuscript";
		if (s.equals("map")) return "Map";
		if (s.equals("newspaperArticle")) return "Newspaper Article";
		if (s.equals("note")) return "Note";
		if (s.equals("patent")) return "Patent";
		if (s.equals("podcast")) return "Podcast";
		if (s.equals("presentation")) return "Presentation";
		if (s.equals("radioBroadcast")) return "Radio Broadcast";
		if (s.equals("report")) return "Report";
		if (s.equals("statute")) return "Statute";
		if (s.equals("tvBroadcast")) return "TV Broadcast";
		if (s.equals("thesis")) return "Thesis";
		if (s.equals("videoRecording")) return "Video Recording";
		if (s.equals("webpage")) return "Web Page";
		
		// Or just leave it the way it is
		return s;
	}
	
	public static int sortValueForLabel(String s) {
		// First type, and the bare minimum...
		if (s.equals("itemType")) return 0;
		if (s.equals("title")) return 1;
		if (s.equals("creators")) return 2;
		if (s.equals("date")) return 3;
		
		// Then container titles, with one value
		if (s.equals("publicationTitle")) return 5;
		if (s.equals("blogTitle")) return 5;
		if (s.equals("bookTitle")) return 5;
		if (s.equals("dictionaryTitle")) return 5;
		if (s.equals("encyclopediaTitle")) return 5;
		if (s.equals("forumTitle")) return 5;
		if (s.equals("proceedingsTitle")) return 5;
		if (s.equals("programTitle")) return 5;
		if (s.equals("websiteTitle")) return 5;
		if (s.equals("meetingName")) return 5;

		// Abstracts
		if (s.equals("abstractNote")) return 10;
		
		// Publishing data
		if (s.equals("publisher")) return 12;
		if (s.equals("place")) return 13;
		
		// Series, publication numbers
		if (s.equals("pages")) return 14;
		if (s.equals("numPages")) return 16;
		if (s.equals("series")) return 16;
		if (s.equals("seriesTitle")) return 17;
		if (s.equals("seriesText")) return 18;
		if (s.equals("volume")) return 19;
		if (s.equals("numberOfVolumes")) return 20;
		if (s.equals("issue")) return 20;		
		if (s.equals("section")) return 21;
		if (s.equals("publicationNumber")) return 22;
		if (s.equals("edition")) return 23;
		
		
		// Locators
		if (s.equals("DOI")) return 50;
		if (s.equals("archive")) return 51;
		if (s.equals("archiveLocation")) return 52;
		if (s.equals("ISBN")) return 53;
		if (s.equals("ISSN")) return 54;
		
		return 200;
	}
}