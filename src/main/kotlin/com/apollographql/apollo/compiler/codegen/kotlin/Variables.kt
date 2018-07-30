package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.INPUT_OPTIONAL
import com.apollographql.apollo.compiler.ir.VariableSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec

fun VariableSpec.operationParameterSpec(nullableWrapper: ClassName? = null): ParameterSpec {
    val typeName = type.typeName(nullableWrapper)
    return ParameterSpec.builder(name, typeName)
            .apply {
                if (defaultValue != null) {
                    defaultValue(type.initializerCode(defaultValue, INPUT_OPTIONAL))
                }
            }
            .build()
}

fun VariableSpec.variablesParameterSpec(nullableWrapper: ClassName? = null): ParameterSpec {
    val typeName = type.typeName(nullableWrapper)
    return ParameterSpec.builder(name, typeName).build()
}

fun VariableSpec.propertySpec(nullableWrapper: ClassName? = null): PropertySpec {
    val typeName = type.typeName(nullableWrapper)
    return PropertySpec.builder(name, typeName)
            .initializer(name)
            .build()
}

fun VariableSpec.valueMapEntryCode(): CodeBlock {
    return if (type.isOptional) {
        CodeBlock.of("(%S to %L).takeIf { %L.defined }", name, name, name)
    } else {
        CodeBlock.of("%S to %L", name, name)
    }
}
