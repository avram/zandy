package com.gimranov.zandy.app

import com.gimranov.zandy.app.data.ItemCollection

sealed class ItemListingRule
data class Children(val parent: ItemCollection?, val includeCollections: Boolean): ItemListingRule()
data class SearchResults(val query: String): ItemListingRule()
object AllItems: ItemListingRule()
