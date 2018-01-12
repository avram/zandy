
package com.gimranov.zandy.app.model;

import java.io.Serializable;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Creator implements Serializable
{

    @SerializedName("creatorType")
    @Expose
    public String creatorType;
    @SerializedName("name")
    @Expose
    public String name;
    @SerializedName("firstName")
    @Expose
    public String firstName;
    @SerializedName("lastName")
    @Expose
    public String lastName;
    private final static long serialVersionUID = -1651706056337494667L;

}
