
package com.gimranov.zandy.app.model;

import java.io.Serializable;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Data;

@Data
public class ApiResponse<T> implements Serializable
{

    @SerializedName("key")
    @Expose
    public String key;
    @SerializedName("version")
    @Expose
    public int version;
    @SerializedName("library")
    @Expose
    public Library library;
    @SerializedName("links")
    @Expose
    public Links links;
    @SerializedName("meta")
    @Expose
    public Meta meta;
    @SerializedName("data")
    @Expose
    public T data;
    private final static long serialVersionUID = -8693618652266258725L;

}
