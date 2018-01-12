
package com.gimranov.zandy.app.model;

import java.io.Serializable;
import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Data implements Serializable
{

    @SerializedName("key")
    @Expose
    public String key;
    @SerializedName("version")
    @Expose
    public int version;
    @SerializedName("itemType")
    @Expose
    public String itemType;
    @SerializedName("title")
    @Expose
    public String title;
    @SerializedName("creators")
    @Expose
    public List<Creator> creators = null;
    @SerializedName("abstractNote")
    @Expose
    public String abstractNote;
    @SerializedName("series")
    @Expose
    public String series;
    @SerializedName("seriesNumber")
    @Expose
    public String seriesNumber;
    @SerializedName("volume")
    @Expose
    public String volume;
    @SerializedName("numberOfVolumes")
    @Expose
    public String numberOfVolumes;
    @SerializedName("edition")
    @Expose
    public String edition;
    @SerializedName("place")
    @Expose
    public String place;
    @SerializedName("publisher")
    @Expose
    public String publisher;
    @SerializedName("date")
    @Expose
    public String date;
    @SerializedName("numPages")
    @Expose
    public String numPages;
    @SerializedName("language")
    @Expose
    public String language;
    @SerializedName("ISBN")
    @Expose
    public String iSBN;
    @SerializedName("shortTitle")
    @Expose
    public String shortTitle;
    @SerializedName("url")
    @Expose
    public String url;
    @SerializedName("accessDate")
    @Expose
    public String accessDate;
    @SerializedName("archive")
    @Expose
    public String archive;
    @SerializedName("archiveLocation")
    @Expose
    public String archiveLocation;
    @SerializedName("libraryCatalog")
    @Expose
    public String libraryCatalog;
    @SerializedName("callNumber")
    @Expose
    public String callNumber;
    @SerializedName("rights")
    @Expose
    public String rights;
    @SerializedName("extra")
    @Expose
    public String extra;
    @SerializedName("dateAdded")
    @Expose
    public String dateAdded;
    @SerializedName("dateModified")
    @Expose
    public String dateModified;
    @SerializedName("tags")
    @Expose
    public List<Tag> tags = null;
    @SerializedName("collections")
    @Expose
    public List<String> collections = null;
    @SerializedName("relations")
    @Expose
    public Relations relations;
    private final static long serialVersionUID = 6882430443711812702L;

}
