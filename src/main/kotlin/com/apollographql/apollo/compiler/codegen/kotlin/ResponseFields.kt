package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.compiler.ir.ResponseFieldSpec
import com.apollographql.apollo.compiler.ir.TypeKind
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec

fun ResponseFieldSpec.constructorParameterSpec(maybeOptional: Boolean): ParameterSpec {
    val typeName = type.typeName(maybeOptional)
            .let { if (!maybeOptional && type.isOptional) it.asNullable() else it }

    return ParameterSpec.builder(responseName, typeName).build()
}

fun ResponseFieldSpec.propertySpec(): PropertySpec {
    return PropertySpec.builder(responseName, type.typeName())
            .initializer("%L", responseName)
            .build()
}

fun ResponseFieldSpec.factoryCode(): CodeBlock {
    fun customTypeFactoryCode(): CodeBlock {
        return CodeBlock.of("%T.%L(%S, %S, %L, %L, %T, %L)",
                ResponseField::class,
                type.kind.factoryMethod,
                responseName, name,
                argumentsCode(),
                type.isOptional,
                ClassName.bestGuess(type.name),
                conditionsCode())
    }

    fun fragmentFactoryCode(): CodeBlock {
        val conditions = typeConditions.takeIf { it.isNotEmpty() }
                ?.joinToCodeBlock { CodeBlock.of("%S", it.name) }
                ?: CodeBlock.of("emptyList()")

        return CodeBlock.of("%T.%L(%S, %S, %L)", ResponseField::class, type.kind.factoryMethod,
                responseName, name, conditions)
    }

    fun genericFactoryCode(): CodeBlock {
        return CodeBlock.of("%T.%L(%S, %S, %L, %L, %L)",
                ResponseField::class,
                type.kind.factoryMethod,
                responseName,
                name,
                argumentsCode(),
                type.isOptional,
                conditionsCode())
    }

    return when (type.kind) {
        TypeKind.STRING,
        TypeKind.INT,
        TypeKind.LONG,
        TypeKind.DOUBLE,
        TypeKind.BOOLEAN,
        TypeKind.ENUM,
        TypeKind.OBJECT,
        TypeKind.LIST -> genericFactoryCode()
        TypeKind.CUSTOM -> customTypeFactoryCode()
        TypeKind.FRAGMENT,
        TypeKind.INLINE_FRAGMENT -> fragmentFactoryCode()
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
            skipIf.map { Pair(it.name, true) },
            includeIf.map { Pair(it.name, false) }
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
