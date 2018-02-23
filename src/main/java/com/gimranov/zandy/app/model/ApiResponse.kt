package com.gimranov.zandy.app.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(

        @SerializedName("key")
        @Expose
        var key: String? = null,
        @SerializedName("version")
        @Expose
        var version: Int = 0,
        @SerializedName("library")
        @Expose
        var library: Library? = null,
        @SerializedName("links")
        @Expose
        var links: Links? = null,
        @SerializedName("meta")
        @Expose
        var meta: Meta? = null,
        @SerializedName("data")
        @Expose
        var data: T? = null
)
