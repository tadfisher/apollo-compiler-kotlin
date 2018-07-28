package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.compiler.ir.Field
import com.apollographql.apollo.compiler.ir.FieldSpec
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

fun TypeSpec.Builder.addFieldSpec(spec: FieldSpec) {
    val resolvedType = spec.definition.fieldType.
}

//data class FieldSpec(
//        val kdoc: CodeBlock?,
//        val constructorParameter: ParameterSpec,
//        val property: PropertySpec,
//        val responseField: ResponseFieldSpec
//)

fun Field.fieldSpec(): FieldSpec {
    val resolvedType = type.resolve()
    val kdoc = description?.let { CodeBlock.of("@param %L %L", alias, description) }
    val parameter = ParameterSpec.builder(alias, resolvedType).build()
    val property = PropertySpec.builder(alias, resolvedType)
            .initializer(parameter.name)
            .build()
    return FieldSpec(kdoc, parameter, property, ResponseFieldSpec(this))
}
