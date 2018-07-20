package com.apollographql.apollo.compiler

interface ToInput<Val, Raw> {
    fun toInput(value: Val):
}