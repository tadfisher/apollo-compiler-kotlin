package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.compiler.ir.VariableSpec
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec

fun VariableSpec.operationParameterSpec(): ParameterSpec {
    return ParameterSpec.builder(propertyName, type.kotlin())
            .apply {
                if (defaultValue != null) {
                    defaultValue(type.initializerCode(defaultValue))
                }
            }
            .build()
}

fun VariableSpec.variablesParameterSpec(): ParameterSpec {
    return ParameterSpec.builder(propertyName, type.kotlin()).build()
}

fun VariableSpec.propertySpec(): PropertySpec {
    return PropertySpec.builder(propertyName, type.kotlin())
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
