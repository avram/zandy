
package com.gimranov.zandy.app.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

@lombok.Data
public class Self implements Serializable
{

    @SerializedName("href")
    @Expose
    public String href;
    @SerializedName("type")
    @Expose
    public String type;
    private final static long serialVersionUID = 8159796745537384054L;

}
