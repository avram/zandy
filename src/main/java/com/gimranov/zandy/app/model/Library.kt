package com.gimranov.zandy.app.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class Library(

        @SerializedName("type")
        @Expose
        var type: String? = null,
        @SerializedName("id")
        @Expose
        var id: Int = 0,
        @SerializedName("name")
        @Expose
        var name: String? = null,
        @SerializedName("links")
        @Expose
        var links: Links? = null
)