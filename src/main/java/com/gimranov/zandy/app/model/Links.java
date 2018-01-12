
package com.gimranov.zandy.app.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

@lombok.Data
public class Links implements Serializable
{

    @SerializedName("alternate")
    @Expose
    public Alternate alternate;
    private final static long serialVersionUID = 1909908181802478791L;

}
