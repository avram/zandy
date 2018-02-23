package com.gimranov.zandy.app.api

import com.gimranov.zandy.app.model.Collection
import com.gimranov.zandy.app.model.Item
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path

interface ZoteroService {
    @Headers("Zotero-API-Version: 3")
    @GET("{userId}/items")
    fun getItemsForUser(@Path("userId") userId: String): Call<List<Item>>

    @Headers("Zotero-API-Version: 3")
    @GET("{userId}/items/{itemKey}")
    fun getItemForUser(@Path("userId") userId: String, @Path("itemKey") itemKey: String): Call<Item>

    @Headers("Zotero-API-Version: 3")
    @GET("{userId}/collections")
    fun getCollectionsForUser(@Path("userId") userId: String): Call<List<Collection>>

    @Headers("Zotero-API-Version: 3")
    @GET("{userId}/items?format=keys")
    fun getItemKeysForUser(@Path("userId") userId: String): Call<ResponseBody>
}