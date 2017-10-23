package com.gimranov.zandy.app

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

class ItemAdapter : RecyclerView.Adapter<ItemAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent!!.context).inflate(R.layout.item_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder?, position: Int) {
        holder?.title?.text = "Aha! $position"
    }

    override fun getItemCount(): Int {
        return 100
    }

    class ViewHolder(itemView: View?) : RecyclerView.ViewHolder(itemView) {
        val title = itemView?.findViewById<TextView>(R.id.card_item_title)
    }
}