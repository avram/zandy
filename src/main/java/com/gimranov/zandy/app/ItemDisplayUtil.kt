package com.gimranov.zandy.app

import android.os.Build
import android.text.Html
import com.gimranov.zandy.app.data.Item

internal object ItemDisplayUtil {
    fun datumDisplayComponents(label: String,
                               value: String): Pair<CharSequence, CharSequence> {

        /* Since the field names are the API / internal form, we
         * attempt to get a localized, human-readable version. */
        val localizedLabel = Item.localizedStringForString(label)

        if ("itemType" == label) {
            return Pair(localizedLabel, Item.localizedStringForString(value))
        }

        if ("title" == label || "note" == label || "abstractNote" == label) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return Pair(localizedLabel, Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY))
            } else {
                @Suppress("DEPRECATION")
                return Pair(localizedLabel, Html.fromHtml(value))
            }
        } else {
            return Pair(localizedLabel, value)
        }
    }
}