
package com.gimranov.zandy.app.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

import lombok.Data;

@Data
public class Meta implements Serializable
{

    @SerializedName("creatorSummary")
    @Expose
    public String creatorSummary;
    @SerializedName("parsedDate")
    @Expose
    public String parsedDate;
    @SerializedName("numChildren")
    @Expose
    public int numChildren;
    private final static long serialVersionUID = 8211351907864568885L;

}
