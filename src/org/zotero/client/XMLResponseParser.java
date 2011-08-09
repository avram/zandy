package org.zotero.client;

import java.io.InputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.json.JSONException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;
import org.zotero.client.data.Database;
import org.zotero.client.data.Item;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.sax.Element;
import android.sax.ElementListener;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Log;
import android.util.Xml;

public class XMLResponseParser extends DefaultHandler {
	private InputStream input;
	private Item item; 
	public static Database db;
	
	static final String ATOM_NAMESPACE = "http://www.w3.org/2005/Atom";
	static final String Z_NAMESPACE = "http://zotero.org/ns/api";

	public XMLResponseParser(InputStream in) {
		input = in;
	}

	public void parse() {
		// we have a different root for indiv. items
        RootElement root = new RootElement(ATOM_NAMESPACE, "feed");
        Element entry = root.getChild(ATOM_NAMESPACE, "entry");
        entry.setElementListener(new ElementListener() {
            public void start(Attributes attributes) {
            	item = new Item();
            	Log.i("xml-parse", "new entry");
            }

            public void end() {
            	item.save();
            	Log.i("xml-parse", "Done parsing an entry.");
            }
        });
        entry.getChild(ATOM_NAMESPACE, "title").setEndTextElementListener(new EndTextElementListener(){
            public void end(String body) {
            	item.setTitle(body);
            	Log.i("xml-parse", body);
            }
        });
        entry.getChild(Z_NAMESPACE, "key").setEndTextElementListener(new EndTextElementListener(){
            public void end(String body) {
            	item.setKey(body);
            	Log.i("xml-parse", body);
            }
        });
        entry.getChild(ATOM_NAMESPACE, "id").setEndTextElementListener(new EndTextElementListener(){
            public void end(String body) {
            	item.setId(body);
            	Log.i("xml-parse", body);
            }
        });
        entry.getChild(ATOM_NAMESPACE, "content").setStartElementListener(new StartElementListener(){
            public void start(Attributes attributes) {
            	String etag = attributes.getValue(Z_NAMESPACE, "etag");
            	item.setEtag(etag);
            	Log.i("xml-parse", etag);
            }
        });
        entry.getChild(ATOM_NAMESPACE, "content").setEndTextElementListener(new EndTextElementListener(){
            public void end(String body) {
            	try {
            		item.setContent(body);
            	} catch (JSONException e) {
            		Log.e("xml-parse", "content", e);
            	}
            	Log.i("xml-parse", body);
            }
        });
        try {
            Xml.parse(this.input, Xml.Encoding.UTF_8, root.getContentHandler());
            db.close();
/*        } catch (SAXParseException e) {
        	try {
        		
        		Xml.parse(this.input, Xml.Encoding.UTF_8, root.getContentHandler());
        	} catch (Exception e) {
        		throw new RuntimeException(e);
        	}*/
        } catch (Exception e) {
        	throw new RuntimeException(e);
        }
    }
}
