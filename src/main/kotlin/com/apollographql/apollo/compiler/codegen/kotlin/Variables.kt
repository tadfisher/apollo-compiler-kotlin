package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.compiler.ir.VariableSpec
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec

fun VariableSpec.operationParameterSpec(): ParameterSpec {
    val typeName = type.typeName()
    return ParameterSpec.builder(propertyName, typeName)
            .apply {
                if (defaultValue != null) {
                    defaultValue(type.initializerCode(defaultValue))
                }
            }
            .build()
}

fun VariableSpec.variablesParameterSpec(): ParameterSpec {
    val typeName = type.typeName()
    return ParameterSpec.builder(propertyName, typeName).build()
}

fun VariableSpec.propertySpec(): PropertySpec {
    val typeName = type.typeName()
    return PropertySpec.builder(propertyName, typeName)
            .initializer(propertyName)
            .build()
}

fun VariableSpec.valueMapEntryCode(): CodeBlock {
    return if (type.isOptional) {
        CodeBlock.of("(%S to %L).takeIf { %L.defined }", name, propertyName, propertyName)
    } else {
        CodeBlock.of("%S to %L", name, propertyName)
    }
}
