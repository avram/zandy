
package com.gimranov.zandy.app.model;

import java.io.Serializable;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Links_ implements Serializable
{

    @SerializedName("self")
    @Expose
    public Self self;
    @SerializedName("alternate")
    @Expose
    public Alternate_ alternate;
    private final static long serialVersionUID = -4626930828623708887L;

}
