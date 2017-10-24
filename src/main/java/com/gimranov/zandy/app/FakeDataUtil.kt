package com.gimranov.zandy.app

import com.gimranov.zandy.app.data.Item
import io.bloco.faker.Faker
import org.json.JSONObject
import java.util.*

internal object FakeDataUtil {
    fun book(): Item {
        val faker = Faker()
        val item = Item()
        item.title = faker.book.title()
        item.type = "book"
        item.creatorSummary = faker.name.name() + ", " + faker.name.name()
        item.year = (1900 + Random().nextInt(117)).toString()

        return item
    }
}
