package com.apollographql.apollo.compiler.codegen

import com.apollographql.apollo.api.ResponseField

val ResponseField.Type.factoryMethod get() = responseFieldFactoryMethods[this]

private val responseFieldFactoryMethods = mapOf(
        ResponseField.Type.STRING to "forString",
        ResponseField.Type.INT to "forInt",
        ResponseField.Type.LONG to "forLong",
        ResponseField.Type.DOUBLE to "forDouble",
        ResponseField.Type.BOOLEAN to "forBoolean",
        ResponseField.Type.ENUM to "forString",
        ResponseField.Type.OBJECT to "forObject",
        ResponseField.Type.LIST to "forList",
        ResponseField.Type.CUSTOM to "forCustomType",
        ResponseField.Type.FRAGMENT to "forFragment",
        ResponseField.Type.INLINE_FRAGMENT to "forInlineFragment"
)