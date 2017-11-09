package com.gimranov.zandy.app.data

import android.database.Cursor
import android.util.Log
import com.gimranov.zandy.app.Query

object DatabaseAccess {
    val TAG = this.javaClass.simpleName!!

    private val sortOptions = arrayOf("item_year, item_title COLLATE NOCASE",
            "item_creator COLLATE NOCASE, item_year",
            "item_title COLLATE NOCASE, item_year",
            "timestamp ASC, item_title COLLATE NOCASE")

    fun collections(db: Database): Cursor {
        val args = arrayOf("false")
        val cursor = db.query("collections", Database.COLLCOLS, "collection_parent=?", args, null, null, "collection_name", null)
        if (cursor == null) {
            Log.e(TAG, "cursor is null")
        }

        return cursor
    }

    fun collectionsForParent(db: Database, parent: ItemCollection): Cursor {
        val args = arrayOf(parent.key)
        return db.query("collections", Database.COLLCOLS, "collection_parent=?", args, null, null, "collection_name", null)
    }

    fun items(db: Database, parent: ItemCollection?, sortRule: String?): Cursor {
        val sortClause = sortRule ?: sortOptions[0]

        when (parent) {
            null -> Query().query(db)
            else -> {
                val args = arrayOf(parent.dbId)
                return db.rawQuery("SELECT item_title, item_type, item_content, etag, dirty, items._id, item_key, item_year, item_creator, timestamp, item_children  FROM items, itemtocollections WHERE items._id = item_id AND collection_id=? ORDER BY $sortClause",
                        args)
            }
        }
        return Query().query(db)
    }

    fun items(db: Database, query: String, sortRule: String?): Cursor {
        val sortClause = sortRule ?: sortOptions[0]

        val args = arrayOf("%$query%", "%$query%")
        return db.rawQuery("SELECT item_title, item_type, item_content, etag, dirty, _id, item_key, item_year, item_creator, timestamp, item_children  FROM items WHERE item_title LIKE ? OR item_creator LIKE ? ORDER BY $sortClause",
                args)
    }
}