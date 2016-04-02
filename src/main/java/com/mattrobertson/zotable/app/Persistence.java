/*******************************************************************************
 * This file is part of Zotable.
 *
 * Zotable is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Zotable is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Zotable.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.mattrobertson.zotable.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.jetbrains.annotations.Nullable;

public class Persistence {
    private static final String TAG = Persistence.class.getCanonicalName();

    private static final String FILE = "Persistence";

    public static void write(String key, String value) {
        SharedPreferences.Editor editor = Application.getInstance().getSharedPreferences(FILE, Context.MODE_PRIVATE).edit();
        editor.putString(key, value);
        editor.commit();
    }

    @Nullable
    public static String read(String key) {
        SharedPreferences store = Application.getInstance().getSharedPreferences(FILE, Context.MODE_PRIVATE);
        if (!store.contains(key)) return null;

        return store.getString(key, null);
    }
}
