
package com.gimranov.zandy.app.model;

import java.io.Serializable;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Tag implements Serializable
{

    @SerializedName("tag")
    @Expose
    public String tag;
    @SerializedName("type")
    @Expose
    public int type;
    private final static long serialVersionUID = 2162649914559869225L;

}
