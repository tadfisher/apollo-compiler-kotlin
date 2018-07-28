package com.apollographql.apollo.compiler.codegen

import com.apollographql.apollo.api.ResponseField

object ResponseFields {
    val FACTORY_METHODS = mapOf(
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
    val READ_METHODS = mapOf(
            ResponseField.Type.STRING to "readString",
            ResponseField.Type.INT to "readInt",
            ResponseField.Type.LONG to "readLong",
            ResponseField.Type.DOUBLE to "readDouble",
            ResponseField.Type.BOOLEAN to "readBoolean",
            ResponseField.Type.ENUM to "readString",
            ResponseField.Type.OBJECT to "readObject",
            ResponseField.Type.LIST to "readList",
            ResponseField.Type.CUSTOM to "readCustomType",
            ResponseField.Type.FRAGMENT to "readConditional",
            ResponseField.Type.INLINE_FRAGMENT to "readConditional"
    )
    val WRITE_METHODS = mapOf(
            ResponseField.Type.STRING to "writeString",
            ResponseField.Type.INT to "writeInt",
            ResponseField.Type.LONG to "writeLong",
            ResponseField.Type.DOUBLE to "writeDouble",
            ResponseField.Type.BOOLEAN to "writeBoolean",
            ResponseField.Type.ENUM to "writeString",
            ResponseField.Type.CUSTOM to "writeCustom",
            ResponseField.Type.OBJECT to "writeObject",
            ResponseField.Type.LIST to "writeList"
    )
}