package com.gimranov.zandy.app.api;

import com.gimranov.zandy.app.model.ApiResponse;
import com.gimranov.zandy.app.model.Item;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/*
 * This file is part of Zandy.
 *
 * Zandy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Zandy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Zandy.  If not, see <http://www.gnu.org/licenses/>.
 */
public class ZoteroClient {
    public static final String BASE_URL = "https://api.zotero.org/users/";
    private static ZoteroService sInstance;

    public static ZoteroService getInstance() {
        if (sInstance == null) {
            synchronized (ZoteroClient.class) {
                if (sInstance == null) {
                    Gson gson = new GsonBuilder()
                            .registerTypeAdapter(Item.class, new ZoteroDeserializer())
                            .create();

                    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .addConverterFactory(GsonConverterFactory.create(gson))
                            .build();

                    return retrofit.create(ZoteroService.class);
                }
            }
        }
        return sInstance;
    }

}
