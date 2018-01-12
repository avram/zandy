
package com.gimranov.zandy.app.model;

import java.io.Serializable;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Alternate_ implements Serializable
{

    @SerializedName("href")
    @Expose
    public String href;
    @SerializedName("type")
    @Expose
    public String type;
    private final static long serialVersionUID = -7466182983022921676L;

}
