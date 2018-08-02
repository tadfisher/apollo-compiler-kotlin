package com.apollographql.apollo.compiler.util

infix fun <T> Sequence<T>.lazyPlus(otherGenerator: () -> Sequence<T>): Sequence<T> {
    return object : Sequence<T> {
        private val thisIterator: Iterator<T> by lazy { this@lazyPlus.iterator() }
        private val otherIterator: Iterator<T> by lazy { otherGenerator().iterator() }

        override fun iterator() = object : Iterator<T> {
            override fun next(): T {
                return if (thisIterator.hasNext()) {
                    thisIterator.next()
                } else {
                    otherIterator.next()
                }
            }

            override fun hasNext(): Boolean = thisIterator.hasNext() || otherIterator.hasNext()
        }
    }
}