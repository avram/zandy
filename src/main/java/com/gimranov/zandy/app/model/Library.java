
package com.gimranov.zandy.app.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

@lombok.Data
public class Library implements Serializable
{

    @SerializedName("type")
    @Expose
    public String type;
    @SerializedName("id")
    @Expose
    public int id;
    @SerializedName("name")
    @Expose
    public String name;
    @SerializedName("links")
    @Expose
    public Links links;
    private final static long serialVersionUID = 3455999056633603093L;

}
