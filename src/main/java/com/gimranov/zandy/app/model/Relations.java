
package com.gimranov.zandy.app.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

@lombok.Data
public class Relations implements Serializable
{

    @SerializedName("owl:sameAs")
    @Expose
    public String owlSameAs;
    private final static long serialVersionUID = 4806079105216034030L;

}
