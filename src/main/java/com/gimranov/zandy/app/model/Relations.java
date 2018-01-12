
package com.gimranov.zandy.app.model;

import java.io.Serializable;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Relations implements Serializable
{

    @SerializedName("owl:sameAs")
    @Expose
    public String owlSameAs;
    private final static long serialVersionUID = 4806079105216034030L;

}
