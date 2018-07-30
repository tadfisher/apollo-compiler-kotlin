package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.ResponseField.Type.BOOLEAN
import com.apollographql.apollo.api.ResponseField.Type.CUSTOM
import com.apollographql.apollo.api.ResponseField.Type.DOUBLE
import com.apollographql.apollo.api.ResponseField.Type.ENUM
import com.apollographql.apollo.api.ResponseField.Type.FRAGMENT
import com.apollographql.apollo.api.ResponseField.Type.INLINE_FRAGMENT
import com.apollographql.apollo.api.ResponseField.Type.INT
import com.apollographql.apollo.api.ResponseField.Type.LIST
import com.apollographql.apollo.api.ResponseField.Type.LONG
import com.apollographql.apollo.api.ResponseField.Type.OBJECT
import com.apollographql.apollo.api.ResponseField.Type.STRING
import com.apollographql.apollo.compiler.codegen.factoryMethod
import com.apollographql.apollo.compiler.ir.ResponseFieldSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock

fun ResponseFieldSpec.factoryCode(): CodeBlock {

    fun customTypeFactoryCode(): CodeBlock {
        return CodeBlock.of("%T.%L(%S, %S, %L, %L, %T, %L)",
                ResponseField::class,
                responseType.factoryMethod,
                responseName, name,
                argumentsCode(),
                type.isOptional,
                ClassName.bestGuess(type.name),
                conditionsCode())
    }

    fun fragmentFactoryCode(): CodeBlock {
        val conditions = typeConditions.takeIf { it.isNotEmpty() }
                ?.map { CodeBlock.of("%S", it.name )}
                ?.join(",\n", prefix = "listOf(\n%>", suffix = "\n%<)")
                ?: CodeBlock.of("emptyList()")

        return CodeBlock.of("%T.%L(%S, %S, %L", ResponseField::class, responseType.factoryMethod,
                responseName, name, conditions)
    }

    fun genericFactoryCode(): CodeBlock {
        return CodeBlock.of("%T.%L(%S, %S, %L, %L, %L)",
                ResponseField::class,
                responseType.factoryMethod,
                responseName,
                name,
                argumentsCode(),
                type.isOptional,
                conditionsCode())
    }

    return when (responseType) {
        STRING,
        INT,
        LONG,
        DOUBLE,
        BOOLEAN,
        ENUM,
        OBJECT,
        LIST -> genericFactoryCode()
        CUSTOM -> customTypeFactoryCode()
        FRAGMENT,
        INLINE_FRAGMENT -> fragmentFactoryCode()
    }
}

fun ResponseFieldSpec.argumentsCode(): CodeBlock {
    if (arguments.isEmpty()) return CodeBlock.of("null")

    return CodeBlock.builder()
            .add("mapOf(\n")
            .indent()
            .add(arguments
                    .map { (name, value) ->
                        CodeBlock.of("%S to %L", name, value.argumentValueCode())
                    }
                    .join(",\n", suffix = "\n"))
            .unindent()
            .add(")")
            .build()
}

fun ResponseFieldSpec.conditionsCode(): CodeBlock {
    if (skipIf.isEmpty() && includeIf.isEmpty()) {
        return CodeBlock.of("emptyList()")
    }

    val conditions = listOf(
            skipIf.map { Pair(name, true) },
            includeIf.map { Pair(name, false) }
    ).flatten()

    return CodeBlock.builder()
            .add("listOf(\n")
            .indent()
            .add(conditions
                    .map { (name, inverted) ->
                        CodeBlock.of("%T.booleanCondition(%S, %L)",
                                ResponseField.Condition::class, name, inverted)
                    }
                    .join(",\n", suffix = "\n"))
            .unindent()
            .add(")")
            .build()
}

