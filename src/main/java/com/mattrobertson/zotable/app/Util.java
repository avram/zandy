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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;

import com.mattrobertson.zotable.app.data.Creator;
import com.mattrobertson.zotable.app.data.Database;
import com.mattrobertson.zotable.app.data.Item;
import com.mattrobertson.zotable.app.task.APIRequest;

import org.pdfparse.model.PDFDocCatalog;
import org.pdfparse.model.PDFDocInfo;
import org.pdfparse.model.PDFDocument;

public class Util {
    private static final String TAG = Util.class.getCanonicalName();

    public static final String DOI_PREFIX = "http://dx.doi.org/";

    public static String doiToUri(String doi) {
       if (isDoi(doi)) {
           return DOI_PREFIX + doi.replaceAll("^doi:", "");
       }
        return doi;
    }

    public static boolean isDoi(String doi) {
        return (doi.startsWith("doi:") || doi.startsWith("10."));
    }

    public static void handleUpload(String filename, final Context context, final Database db, final boolean clearStack) {

        final String title;
        final String author;
        final String keywords;

        try {
            // Create document object. Open file
            PDFDocument doc = new PDFDocument(filename);

            // Get document structure elements
            PDFDocInfo info = doc.getDocumentInfo();
            PDFDocCatalog cat = doc.getDocumentCatalog();

            title = info.getTitle();
            author = info.getAuthor();
            keywords = info.getSubject();

        } catch (Exception e) {
            Log.e("ZZZ", e.getMessage());
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getResources().getString(R.string.item_type))
                // XXX i18n
                .setItems(Item.ITEM_TYPES_EN, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int pos) {
                        Item item = new Item(context, Item.ITEM_TYPES[pos]);
                        item.dirty = APIRequest.API_DIRTY;
                        item.setTitle(title);
                        item.save(db);

                        Item.setCreator(item.getKey(), new Creator("author", author, true), 0, db);
                        item = Item.load(item.getKey(),db);

                        String[] arrKeywords = keywords.split(",");
                        for (String keyword : arrKeywords)
                            item.addTag(keyword.trim());

                        item.save(db);

                        Log.d(TAG, "Loading item data with key: " + item.getKey());

                        // We create and issue a specified intent with the necessary data
                        Intent i = new Intent(context, ItemDataActivity.class);
                        i.putExtra("com.mattrobertson.zotable.app.itemKey", item.getKey());
                        if (clearStack)
                            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        context.startActivity(i);
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
