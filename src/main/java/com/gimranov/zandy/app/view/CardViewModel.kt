package com.gimranov.zandy.app.view

import com.gimranov.zandy.app.data.Item
import com.gimranov.zandy.app.data.ItemCollection

sealed class CardViewModel {

}
data class ItemViewModel(val item: Item) : CardViewModel()
data class CollectionViewModel(val collection: ItemCollection) : CardViewModel()