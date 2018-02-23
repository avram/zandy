package com.gimranov.zandy.app.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class Creator(
        @SerializedName("creatorType")
        @Expose
        var creatorType: String? = null,
        @SerializedName("name")
        @Expose
        var name: String? = null,
        @SerializedName("firstName")
        @Expose
        var firstName: String? = null,
        @SerializedName("lastName")
        @Expose
        var lastName: String? = null
)
