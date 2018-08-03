package com.apollographql.apollo.compiler.util

fun <T> MutableList<T>.update(predicate: (T) -> Boolean, transform: (T) -> T): Int {
    var updated = 0
    forEachIndexed { index, item ->
        if (predicate(item)) {
            set(index, transform(item))
            updated += 1
        }
    }
    return updated
}