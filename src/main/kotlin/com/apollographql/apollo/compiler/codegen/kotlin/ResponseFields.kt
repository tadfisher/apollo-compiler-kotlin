package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.compiler.ir.BuiltinType
import com.apollographql.apollo.compiler.ir.BuiltinTypeRef
import com.apollographql.apollo.compiler.ir.CustomTypeRef
import com.apollographql.apollo.compiler.ir.EnumTypeRef
import com.apollographql.apollo.compiler.ir.FragmentTypeRef
import com.apollographql.apollo.compiler.ir.FragmentsWrapperTypeRef
import com.apollographql.apollo.compiler.ir.InlineFragmentTypeRef
import com.apollographql.apollo.compiler.ir.ListTypeRef
import com.apollographql.apollo.compiler.ir.ObjectTypeRef
import com.apollographql.apollo.compiler.ir.OptionalType
import com.apollographql.apollo.compiler.ir.ResponseFieldSpec
import com.apollographql.apollo.compiler.ir.TypeRef
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec

val TypeRef.factoryMethod get() = when (this) {
    is BuiltinTypeRef -> when (kind) {
        BuiltinType.INT -> "forInt"
        BuiltinType.FLOAT -> "forDouble"
        BuiltinType.STRING -> "forString"
        BuiltinType.BOOLEAN -> "forBoolean"
        BuiltinType.ID -> "forString"
    }
    is EnumTypeRef -> "forString"
    is ObjectTypeRef -> "forObject"
    is ListTypeRef -> "forList"
    is CustomTypeRef -> "forCustomType"
    is FragmentsWrapperTypeRef -> "forFragment"
    is FragmentTypeRef -> throw UnsupportedOperationException()
    is InlineFragmentTypeRef -> "forInlineFragment"
}

fun ResponseFieldSpec<*>.constructorParameterSpec(maybeOptional: Boolean): ParameterSpec {
    val typeName = when {
        maybeOptional -> type.kotlin()
        type.isOptional -> type.required().kotlin().asNullable()
        else -> type.required().kotlin()
    }
    return ParameterSpec.builder(responseName, typeName).build()
}

fun ResponseFieldSpec<*>.propertySpec(override: Boolean = false): PropertySpec {
    return with(PropertySpec.builder(responseName, type.kotlin())) {
        if (override) addModifiers(KModifier.OVERRIDE)
        initializer("%L", responseName)
        build()
    }
}

fun ResponseFieldSpec<*>.abstractPropertySpec(): PropertySpec {
    return PropertySpec.builder(responseName, type.kotlin(), KModifier.ABSTRACT)
        .apply { if (doc.isNotEmpty()) addKdoc("%L\n", doc) }
        .build()
}

fun ResponseFieldSpec<*>.factoryCode(): CodeBlock {
    fun customTypeFactoryCode(): CodeBlock {
        return CodeBlock.of("%T.%L(%S, %S, %L, %L, %T, %L)",
            ResponseField::class,
            type.factoryMethod,
            responseName, name,
            argumentsCode(),
            type.optional != OptionalType.NONNULL,
            ClassName.bestGuess(type.name),
            conditionsCode())
    }

    fun fragmentFactoryCode(): CodeBlock {
        val conditions = typeConditions.takeIf { it.isNotEmpty() }
            ?.joinToCodeBlock(prefix = "listOf(", suffix = ")") { CodeBlock.of("%S", it) }
            ?: CodeBlock.of("emptyList()")

        return CodeBlock.of("%T.%L(%S, %S, %L)", ResponseField::class, type.factoryMethod,
            Selections.typenameField, Selections.typenameField, conditions)
    }

    fun genericFactoryCode(): CodeBlock {
        return CodeBlock.of("%T.%L(%S, %S, %L, %L, %L)",
            ResponseField::class,
            type.factoryMethod,
            responseName,
            name,
            argumentsCode(),
            type.optional != OptionalType.NONNULL,
            conditionsCode())
    }

    return when (type) {
        is BuiltinTypeRef,
        is EnumTypeRef,
        is ObjectTypeRef,
        is ListTypeRef -> genericFactoryCode()
        is CustomTypeRef -> customTypeFactoryCode()
        is FragmentsWrapperTypeRef,
        is InlineFragmentTypeRef -> fragmentFactoryCode()
        is FragmentTypeRef -> throw UnsupportedOperationException()
    }
}

fun ResponseFieldSpec<*>.argumentsCode(): CodeBlock {
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

fun ResponseFieldSpec<*>.conditionsCode(): CodeBlock {
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
