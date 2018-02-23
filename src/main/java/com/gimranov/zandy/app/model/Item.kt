package com.gimranov.zandy.app.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class Item(
        @SerializedName("key")
        @Expose
        var key: String? = null,
        @SerializedName("version")
        @Expose
        var version: Int = 0,
        @SerializedName("itemType")
        @Expose
        var itemType: String? = null,
        @SerializedName("title")
        @Expose
        var title: String? = null,
        @SerializedName("creators")
        @Expose
        var creators: List<Creator>? = null,
        @SerializedName("abstractNote")
        @Expose
        var abstractNote: String? = null,
        @SerializedName("series")
        @Expose
        var series: String? = null,
        @SerializedName("seriesNumber")
        @Expose
        var seriesNumber: String? = null,
        @SerializedName("volume")
        @Expose
        var volume: String? = null,
        @SerializedName("numberOfVolumes")
        @Expose
        var numberOfVolumes: String? = null,
        @SerializedName("edition")
        @Expose
        var edition: String? = null,
        @SerializedName("place")
        @Expose
        var place: String? = null,
        @SerializedName("publisher")
        @Expose
        var publisher: String? = null,
        @SerializedName("date")
        @Expose
        var date: String? = null,
        @SerializedName("numPages")
        @Expose
        var numPages: String? = null,
        @SerializedName("language")
        @Expose
        var language: String? = null,
        @SerializedName("ISBN")
        @Expose
        var iSBN: String? = null,
        @SerializedName("shortTitle")
        @Expose
        var shortTitle: String? = null,
        @SerializedName("url")
        @Expose
        var url: String? = null,
        @SerializedName("accessDate")
        @Expose
        var accessDate: String? = null,
        @SerializedName("archive")
        @Expose
        var archive: String? = null,
        @SerializedName("archiveLocation")
        @Expose
        var archiveLocation: String? = null,
        @SerializedName("libraryCatalog")
        @Expose
        var libraryCatalog: String? = null,
        @SerializedName("callNumber")
        @Expose
        var callNumber: String? = null,
        @SerializedName("rights")
        @Expose
        var rights: String? = null,
        @SerializedName("extra")
        @Expose
        var extra: String? = null,
        @SerializedName("dateAdded")
        @Expose
        var dateAdded: String? = null,
        @SerializedName("dateModified")
        @Expose
        var dateModified: String? = null,
        @SerializedName("tags")
        @Expose
        var tags: List<Tag>? = null,
        @SerializedName("collections")
        @Expose
        var collections: List<String>? = null,
        @SerializedName("relations")
        @Expose
        var relations: Relations? = null
)
