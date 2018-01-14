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

package com.gimranov.zandy.app.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

import lombok.Data;

@Data
public class Collection implements Serializable {
    @SerializedName("key")
    @Expose
    public String key;
    @SerializedName("version")
    @Expose
    public int version;
    @SerializedName("name")
    @Expose
    public String name;
    @SerializedName("parentCollection")
    @Expose
    public boolean parentCollection;
    @SerializedName("relations")
    @Expose
    public Relations relations;
    private final static long serialVersionUID = 2086885096076287463L;
}
