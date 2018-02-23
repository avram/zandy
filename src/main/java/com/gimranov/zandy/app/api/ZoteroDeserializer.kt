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

package com.gimranov.zandy.app.api

import com.google.gson.*
import java.lang.reflect.Type

class ZoteroDeserializer<T> : JsonDeserializer<T> {


    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): T? {
            return when {
                json.isJsonArray -> {
                    val responseArray = json.asJsonArray
                    val list = responseArray.map { Gson().fromJson<T>(it.asJsonObject.getAsJsonObject("data"), typeOfT) }
                    @Suppress("UNCHECKED_CAST")
                    list as T
                }
                else -> Gson().fromJson<T>(json.asJsonObject.getAsJsonObject("data"), typeOfT)
            }

    }
}
