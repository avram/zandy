package com.gimranov.zandy.app

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.gimranov.zandy.app.data.Database
import com.gimranov.zandy.app.data.DatabaseAccess
import com.gimranov.zandy.app.data.ItemCollection
import com.gimranov.zandy.app.databinding.CollectionCardBinding
import kotlinx.android.synthetic.main.collection_card.view.*

class CollectionAdapter(val database: Database,
                        itemListingRule: ItemListingRule,
                        private val onNavigate: (ItemCollection, ItemAction) -> Unit) : RecyclerView.Adapter<CollectionAdapter.ItemCollectionViewHolder>() {
    val cursor = when (itemListingRule) {
        is AllItems -> DatabaseAccess.collections(database)
        is Children -> when (itemListingRule.parent) {
            null -> DatabaseAccess.collections(database)
            else -> DatabaseAccess.collectionsForParent(database, itemListingRule.parent)
        }
        is SearchResults -> TODO("No collection searches")
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ItemCollectionViewHolder {
        val layoutInflater = LayoutInflater.from(parent!!.context)
        val cardBinding = CollectionCardBinding.inflate(layoutInflater, parent, false)

        return ItemCollectionViewHolder(cardBinding, onNavigate)
    }

    override fun onBindViewHolder(holder: ItemCollectionViewHolder?, position: Int) {
        if (cursor?.moveToPosition(position) != true) {
            return
        }

        holder?.bind(ItemCollection.load(cursor))
    }

    override fun onViewRecycled(holder: ItemCollectionViewHolder?) {
    }

    override fun getItemCount(): Int {
        return cursor?.count ?: 0
    }

    class ItemCollectionViewHolder(cardBinding: CollectionCardBinding,
                                   private val onNavigate: (ItemCollection, ItemAction) -> Unit) : RecyclerView.ViewHolder(cardBinding.root) {

        private val binding = cardBinding

        fun bind(itemCollection: ItemCollection) {
            binding.collection = itemCollection
            binding.root.card_collection.setOnClickListener { onNavigate(binding.collection!!, ItemAction.VIEW) }
            binding.executePendingBindings()
        }
    }
}