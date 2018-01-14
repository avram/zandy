package com.gimranov.zandy.app.api;

import com.gimranov.zandy.app.model.ApiResponse;
import com.gimranov.zandy.app.model.Item;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;

public interface ZoteroService {
    @Headers("Zotero-API-Version: 3")
    @GET("{userId}/items")
    Call<List<Item>> getItemsforUser(@Path("userId") String userId);

    @Headers("Zotero-API-Version: 3")
    @GET("{userId}/items/{itemkey}")
    Call<Item> getItemforUser(@Path("userId") String userId, @Path("itemkey") String itemKey);
}