package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.api.Input
import com.apollographql.apollo.api.internal.Optional
import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.APOLLO_OPTIONAL
import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.GUAVA_OPTIONAL
import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.INPUT_OPTIONAL
import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.JAVA_OPTIONAL
import com.apollographql.apollo.compiler.ir.TypeRef
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName

fun TypeRef.typeName(nullableWrapper: ClassName? = null): TypeName {
    val rawType = ClassName.bestGuess(jvmName)
    val typeName = if (parameters.isNotEmpty()) {
        rawType.parameterizedBy(
                *(parameters.map { it.typeName(nullableWrapper) }.toTypedArray())
        )
    } else {
        rawType
    }

    return if (isOptional) {
        nullableWrapper?.parameterizedBy(typeName) ?: typeName.asNullable()
    } else {
        typeName
    }
}

fun TypeName.isList() = this is ParameterizedTypeName && rawType == ClassNames.LIST

fun TypeName.isOptional(expectedOptionalType: ClassName? = null): Boolean {
    val rawType = (this as? ParameterizedTypeName)?.rawType ?: this
    return if (expectedOptionalType == null) {
        return when (rawType) {
            APOLLO_OPTIONAL, GUAVA_OPTIONAL, JAVA_OPTIONAL, INPUT_OPTIONAL -> true
            else -> return nullable
        }
    } else {
        rawType == expectedOptionalType
    }
}

fun TypeName.unwrapOptionalType(): TypeName {
    return if (this is ParameterizedTypeName && isOptional()) {
        typeArguments.first().asNonNullable()
    } else {
        this.asNonNullable()
    }
}

fun TypeName.unwrapOptionalValue(
        varName: String,
        checkIfPresent : Boolean = true,
        transformation: ((CodeBlock) -> CodeBlock) = { it }
): CodeBlock {
    return if (isOptional() && this is ParameterizedTypeName) {
        if (rawType == INPUT_OPTIONAL) {
            val valueCode = if (checkIfPresent) {
                CodeBlock.of("%L.value?", varName)
            } else {
                CodeBlock.of("%L.value", varName)
            }
            transformation(valueCode)
        } else {
            val valueCode = CodeBlock.of("%L.get()", varName)
            if (checkIfPresent) {
                CodeBlock.of("%L.takeIf { it.isPresent() }?", varName, transformation(valueCode))
            } else {
                transformation(valueCode)
            }
        }
    } else {
        val valueCode = CodeBlock.of("%L", varName)
        if (nullable && checkIfPresent) {
            CodeBlock.of("%L?", varName, transformation(valueCode))
        } else {
            transformation(valueCode)
        }
    }
}

object ClassNames {
    val APOLLO_OPTIONAL = Optional::class.asClassName()
    val GUAVA_OPTIONAL = ClassName("com.google.common.base", "Optional")
    val JAVA_OPTIONAL = ClassName("java.util", "Optional")
    val INPUT_OPTIONAL = Input::class.asClassName()
    val LIST = List::class.asClassName()
}