package com.gimranov.zandy.app;

import java.util.ArrayList;

import android.database.Cursor;
import android.os.Bundle;

import com.gimranov.zandy.app.data.Database;

/**
 * This class is intended to provide ways of handling queries to the database.
 * 
 * TODO
 * Needed functions:
 *  - specify sets of fields and terms for those fields
 *  	* related need for mapping of fields-- i.e., publicationTitle = journalTitle
 *  - provide reasonable efficiency through use of indexes; in SQLite or in Java
 *  - return data in efficient fashion, preferably by exposing a Cursor or
 *  	some other paged access method.
 *  - normalize queries and data to let 
 *  - allow saving of queries
 *  
 *  Some of this will mean changes to other parts of Zandy's data storage model;
 *  specifically, the raw JSON we're using now won't get us much further. We
 *  could in theory maintain an index with tokens drawn from the JSON that
 *  we populate on original save... Not sure about this.
 * 
 * @author ajlyon
 *
 */
public class Query {
	
	private ArrayList<Bundle> parameters;
	
	private String sortBy;
	
	public Query () {
		parameters = new ArrayList<Bundle>();
	}
	
	public void set(String field, String value) {
		Bundle b = new Bundle();
		b.putString("field", field);
		b.putString("value", value);
		parameters.add(b);
	}
	
	public void sortBy(String term) {
		sortBy = term;
	}
	
	public Cursor query(Database db) {
		StringBuilder sb = new StringBuilder();
		String[] args = new String[parameters.size()];
		int i = 0;
		for (Bundle b : parameters) {
			if (b.getString("field").equals("tag")) {
				sb.append("item_content LIKE ?");
				args[i] = "%"+b.getString("value")+"%";
			} else {
				sb.append(b.getString("field") + "=?");
				args[i] = b.getString("value");
			}
			i++;
			if (i < parameters.size()) sb.append(",");
		}
		Cursor cursor = db.query("items", Database.ITEMCOLS, sb.toString(), args, null, null, this.sortBy, null);
		return cursor;
	}
}
