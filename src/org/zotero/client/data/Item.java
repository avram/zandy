package org.zotero.client.data;

import org.json.JSONException;
import org.json.JSONObject;

import android.database.Cursor;

public class Item  {
	private String id;
	private String title;
	private String owner;
	private String key;
	private String etag;
	private JSONObject content;
	public static Database db;
	
	public Item() {
		setId(null);
		setTitle(null);
		setOwner(null);
		setKey(null);
		try {
			setContent("");
		} catch (JSONException e) {
			// do nothing
		}
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public JSONObject getContent() {
		return content;
	}

	public void setContent(JSONObject content) {
		this.content = content;
	}
	
	public void setContent(String content) throws JSONException {
		this.content = new JSONObject(content);
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	public String getEtag() {
		return etag;
	}
	
	public void save() {
		String[] args = { title, "0", "document", content.toString() };
		Cursor cur = db.rawQuery(
				"insert into items (item_title, collection_id, item_type, item_content)" +
				"values (?, ?, ?, ?)", args);
		if (cur != null) cur.close();
	}
	
}