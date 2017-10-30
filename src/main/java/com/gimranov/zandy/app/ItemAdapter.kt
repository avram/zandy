package com.gimranov.zandy.app

import android.graphics.Typeface
import android.os.Build
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableRow
import android.widget.TextView
import com.gimranov.zandy.app.data.Database
import com.gimranov.zandy.app.data.DatabaseAccess
import com.gimranov.zandy.app.data.Item
import com.gimranov.zandy.app.databinding.ItemCardBinding
import kotlinx.android.synthetic.main.item_card.view.*


class ItemAdapter(val database: Database,
                  itemListingRule: ItemListingRule,
                  private val onItemNavigate: (Item, ItemAction) -> Unit) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {
    val cursor = when (itemListingRule) {
        is AllItems -> DatabaseAccess.items(database, null, null)
        is Children -> DatabaseAccess.items(database, itemListingRule.parent, null)
        is SearchResults -> DatabaseAccess.items(database, itemListingRule.query, null)
    }


    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ItemViewHolder {
        val layoutInflater = LayoutInflater.from(parent!!.context)
        val cardBinding = ItemCardBinding.inflate(layoutInflater, parent, false)
        return ItemViewHolder(cardBinding, onItemNavigate)
    }

    override fun onBindViewHolder(holder: ItemViewHolder?, position: Int) {
        if (!cursor.moveToPosition(position)) {
            return
        }

        holder?.bind(Item.load(cursor))
    }

    override fun onViewRecycled(holder: ItemViewHolder?) {
        holder?.unbind()
    }

    override fun getItemCount(): Int {
        return cursor.count
    }

    class ItemViewHolder(cardBinding: ItemCardBinding,
                         private val onItemNavigate: (Item, ItemAction) -> Unit) : RecyclerView.ViewHolder(cardBinding.root) {

        private val binding = cardBinding
        private var expanded = false

        private fun toggle() {
            if (expanded) {
                hide()
            } else {
                show()
            }
        }

        private fun hide() {
            expanded = false
            binding.cardExpandedContent.visibility = View.GONE
            binding.cardButtonBar.visibility = View.GONE
        }

        private fun show() {
            expanded = true
            binding.cardExpandedContent.visibility = View.VISIBLE
            binding.cardButtonBar.visibility = View.VISIBLE
        }

        fun bind(item: Item) {
            binding.item = item
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                binding.itemTypeIcon = binding.root.context.getDrawable(Item.resourceForType(item.type))
            } else {
                @Suppress("DEPRECATION")
                binding.itemTypeIcon = binding.root.context.resources.getDrawable(Item.resourceForType(item.type))
            }

            binding.cardHeader.setOnClickListener { toggle() }
            binding.cardButtonBar.card_button_bar_edit
                    .setOnClickListener { onItemNavigate(item, ItemAction.EDIT) }
            binding.cardButtonBar.card_button_bar_organize
                    .setOnClickListener { onItemNavigate(item, ItemAction.ORGANIZE) }

            val keys = item.content.keys().asSequence().sortedBy { Item.sortValueForLabel(it) }.toList()

            keys.forEach {
                val row = TableRow(binding.root.context)
                row.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT)

                val labelField = TextView(binding.root.context)
                val contentField = TextView(binding.root.context)

                labelField.typeface = Typeface.DEFAULT_BOLD
                labelField.setPadding(0, 0, 10, 0)

                val (label, value) = ItemDisplayUtil.datumDisplayComponents(it, item.content.optString(it))

                if (value.isEmpty()) {
                    return
                }

                labelField.text = label
                contentField.text = value

                row.addView(labelField)
                row.addView(contentField)

                binding.cardExpandedContent.addView(row)
            }

            binding.executePendingBindings()
        }

        fun unbind() {
            binding.cardExpandedContent.removeAllViews()
            hide()
        }
    }
}

