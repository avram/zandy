package com.gimranov.zandy.app

import android.os.Build
import android.text.Html
import com.gimranov.zandy.app.data.Item
import org.json.JSONArray
import org.json.JSONObject

internal object ItemDisplayUtil {
    fun datumDisplayComponents(label: String,
                               value: String): Pair<CharSequence, CharSequence> {

        /* Since the field names are the API / internal form, we
         * attempt to get a localized, human-readable version. */
        val localizedLabel = Item.localizedStringForString(label)

        if ("itemType" == label) {
            return Pair(localizedLabel, Item.localizedStringForString(value))
        }

        if ("creators" == label) {
            return Pair(localizedLabel, formatCreatorList(JSONArray(value)))
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

    fun formatCreatorList(creators: JSONArray): CharSequence {
        /*
         * Creators should be labeled with role and listed nicely
         * This logic isn't as good as it could be.
         */
        var creator: JSONObject
        val sb = StringBuilder()
        for (j in 0 until creators.length()) {
            creator = creators.getJSONObject(j)
            if (creator.getString("creatorType") == "author") {
                if (creator.has("name"))
                    sb.append(creator.getString("name"))
                else
                    sb.append(creator.getString("firstName") + " "
                            + creator.getString("lastName"))
            } else {
                if (creator.has("name"))
                    sb.append(creator.getString("name"))
                else
                    sb.append(creator.getString("firstName")
                            + " "
                            + creator.getString("lastName")
                            + " ("
                            + Item.localizedStringForString(creator
                            .getString("creatorType"))
                            + ")")
            }
            if (j < creators.length() - 1)
                sb.append(", ")
        }

        return sb.toString()
    }
}