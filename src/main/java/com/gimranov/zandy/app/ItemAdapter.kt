package com.gimranov.zandy.app

import android.graphics.Typeface
import android.os.Build
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.NO_ID
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.gimranov.zandy.app.data.Database
import com.gimranov.zandy.app.data.Item
import com.gimranov.zandy.app.databinding.ItemCardBinding
import android.widget.TableRow
import android.widget.TextView
import android.util.DisplayMetrics


class ItemAdapter(val database: Database) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {
    /**
     * Assumes the query never changes-- certainly not realistic!
     *
     * TODO Decouple and properly close cursors!
     */
    private val cursor = Query().query(database)

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ItemViewHolder {
        val layoutInflater = LayoutInflater.from(parent!!.context);
        val cardBinding = ItemCardBinding.inflate(layoutInflater, parent, false)
        return ItemViewHolder(cardBinding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder?, position: Int) {
        if (!cursor.moveToPosition(position)) {
            return
        }

        holder?.let {
            it.binding.cardHeader.setOnClickListener { holder.toggle() }
            it.bind(Item.load(cursor))
        }
    }

    override fun onViewRecycled(holder: ItemViewHolder?) {
        holder?.unbind()
    }

    override fun getItemId(position: Int): Long {
        if (!cursor.moveToPosition(position)) {
            return NO_ID
        }

        return Item.load(cursor).id.hashCode().toLong()
    }

    override fun getItemCount(): Int {
        return cursor.count
    }

    class ItemViewHolder(cardBinding: ItemCardBinding) : RecyclerView.ViewHolder(cardBinding.root) {
        val binding = cardBinding
        private var expanded = false

        fun toggle() {
            expanded = !expanded

            if (expanded) {
                binding.cardExpandedContent.visibility = View.GONE
                binding.cardButtonBar.visibility = View.GONE
            } else {
                binding.cardExpandedContent.visibility = View.VISIBLE
                binding.cardButtonBar.visibility = View.VISIBLE
            }
        }

        fun bind(item: Item) {
            binding.item = item
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                binding.itemTypeIcon = binding.root.context.getDrawable(Item.resourceForType(item.type))
            } else {
                binding.itemTypeIcon = binding.root.context.resources.getDrawable(Item.resourceForType(item.type))
            }

            item.content.keys().forEach {
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
        }

    }
}