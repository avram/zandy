package com.gimranov.zandy.app.api;

import com.gimranov.zandy.app.model.ApiResponse;
import com.gimranov.zandy.app.model.Item;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ZoteroService {
    @GET("{userId}/items")
    Call<List<ApiResponse<Item>>> getItemsforUser(@Path("userId") String userId);
}
