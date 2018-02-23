package com.gimranov.zandy.app.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

import java.io.Serializable

class Links : Serializable {

    @SerializedName("alternate")
    @Expose
    var alternate: Alternate? = null

    companion object {
        private const val serialVersionUID = 1909908181802478791L
    }

}
