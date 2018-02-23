package com.gimranov.zandy.app.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

import java.io.Serializable

class Relations : Serializable {

    @SerializedName("owl:sameAs")
    @Expose
    var owlSameAs: String? = null

    companion object {
        private const val serialVersionUID = 4806079105216034030L
    }

}
