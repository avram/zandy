package com.gimranov.zandy.app.api;

import com.gimranov.zandy.app.model.Collection;
import com.gimranov.zandy.app.model.Item;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;

public interface ZoteroService {
    @Headers("Zotero-API-Version: 3")
    @GET("{userId}/items")
    Call<List<Item>> getItemsForUser(@Path("userId") String userId);

    @Headers("Zotero-API-Version: 3")
    @GET("{userId}/items/{itemkey}")
    Call<Item> getItemForUser(@Path("userId") String userId, @Path("itemkey") String itemKey);

    @Headers("Zoter-API-Version: 3")
    @GET("{userId}/collections")
    Call<List<Collection>> getCollectonsForUser(@Path("userId") String userId);
}