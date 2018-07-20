package com.apollographql.apollo.compiler.util

object StringUtil {
    fun orList(items: List<String>, limit: Int = 5): String {
        return if (items.isEmpty()) {
            throw IllegalArgumentException("List is empty")
        } else {
            items.take(limit).joinToString(", ")
        }
    }

    fun quotedOrList(items: List<String>, limit: Int = 5): String = orList(items.map { "'$it'" }, limit)
}