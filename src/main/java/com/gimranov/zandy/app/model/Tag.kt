package com.gimranov.zandy.app.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class Tag(
        @SerializedName("tag")
        @Expose
        var tag: String? = null,
        @SerializedName("type")
        @Expose
        var type: Int = 0
)
