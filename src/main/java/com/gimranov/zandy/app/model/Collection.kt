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

package com.gimranov.zandy.app.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class Collection(
        @SerializedName("key")
        @Expose
        var key: String? = null,
        @SerializedName("version")
        @Expose
        var version: Int = 0,
        @SerializedName("name")
        @Expose
        var name: String? = null,
        @SerializedName("parentCollection")
        @Expose
        var parentCollection: Boolean = false,
        @SerializedName("relations")
        @Expose
        var relations: Relations? = null
)
