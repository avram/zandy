package com.gimranov.zandy.app

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.gimranov.zandy.app.data.Item
import com.gimranov.zandy.app.databinding.ItemCardBinding

class ItemAdapter : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ItemViewHolder {
        val layoutInflater = LayoutInflater.from(parent!!.context);
        val cardBinding = ItemCardBinding.inflate(layoutInflater, parent, false)
        return ItemViewHolder(cardBinding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder?, position: Int) {
        holder?.let {
            it.binding.cardHeader.setOnClickListener { holder.toggle() }
            it.bind(FakeDataUtil.book())
        }
    }

    override fun getItemCount(): Int {
        return 100
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

        fun bind(item : Item) {
            binding.item = item
            binding.executePendingBindings()
        }

    }
}