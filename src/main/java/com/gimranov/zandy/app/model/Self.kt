package com.gimranov.zandy.app.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class Self(

        @SerializedName("href")
        @Expose
        var href: String? = null,
        @SerializedName("type")
        @Expose
        var type: String? = null
)