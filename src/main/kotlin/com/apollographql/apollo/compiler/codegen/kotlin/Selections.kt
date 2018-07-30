package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.compiler.ir.SelectionSetSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec

fun SelectionSetSpec.responseFieldsPropertySpec(): PropertySpec {
    val initializerCode = fields.map { it.factoryCode() }
            .join(",\n", prefix = "arrayOf(\n%>", suffix = "\n%<)")

    return PropertySpec.builder("_responseFields", Array<ResponseField>::class, KModifier.INTERNAL)
            .addAnnotation(JvmField::class)
            .initializer(initializerCode)
            .build()
}
