package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.api.Input
import com.apollographql.apollo.api.InputFieldWriter
import com.apollographql.apollo.api.ResponseWriter
import com.apollographql.apollo.api.internal.Optional
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName

object ClassNames {
    val APOLLO_OPTIONAL = Optional::class.asClassName()
    val GUAVA_OPTIONAL = ClassName("com.google.common.base", "Optional")
    val JAVA_OPTIONAL = ClassName("java.util", "Optional")
    val INPUT_OPTIONAL = Input::class.asClassName()
    val LIST = List::class.asClassName()

    val INPUT_FIELD_LIST_WRITER = InputFieldWriter.ListWriter::class.asClassName()
    val RESPONSE_LIST_WRITER = ResponseWriter.ListWriter::class.asClassName()
}