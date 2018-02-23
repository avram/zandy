package com.gimranov.zandy.app.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class Meta(
        @SerializedName("creatorSummary")
        @Expose
        var creatorSummary: String? = null,
        @SerializedName("parsedDate")
        @Expose
        var parsedDate: String? = null,
        @SerializedName("numChildren")
        @Expose
        var numChildren: Int = 0
)
