package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.compiler.ast.BooleanValue
import com.apollographql.apollo.compiler.ast.EnumValue
import com.apollographql.apollo.compiler.ast.FloatValue
import com.apollographql.apollo.compiler.ast.IntValue
import com.apollographql.apollo.compiler.ast.ListValue
import com.apollographql.apollo.compiler.ast.NullValue
import com.apollographql.apollo.compiler.ast.ObjectField
import com.apollographql.apollo.compiler.ast.ObjectValue
import com.apollographql.apollo.compiler.ast.ScalarValue
import com.apollographql.apollo.compiler.ast.StringValue
import com.apollographql.apollo.compiler.ast.Value
import com.apollographql.apollo.compiler.ast.VariableValue
import com.squareup.kotlinpoet.CodeBlock

fun Value.code(): CodeBlock {
    return when (this) {
        is ScalarValue -> code()
        is NullValue -> CodeBlock.of("null")
        is EnumValue -> code()
        is ListValue -> code()
        is ObjectValue -> code()
        is VariableValue -> code()
    }
}

fun ScalarValue.code(): CodeBlock {
    return when (this) {
        is IntValue -> CodeBlock.of("%L", value.intValueExact())
        is FloatValue -> CodeBlock.of("%L", value.toDouble())
        is StringValue -> CodeBlock.of("%S", value)
        is BooleanValue -> CodeBlock.of("%L", value)
    }
}

fun EnumValue.code() = CodeBlock.of("%L", value)

fun ListValue.code(): CodeBlock {
    return CodeBlock.builder()
            .add("listOf(\n")
            .indent()
            .apply {
                value.dropLast(1).forEach { addStatement("%L,\n", it.code()) }
                value.lastOrNull()?.let { addStatement("%L\n", it.code() )}
            }
            .unindent()
            .add(")\n")
            .build()
}

fun ObjectValue.code(): CodeBlock {
    return CodeBlock.builder()
            .apply {
                fields.dropLast(1).forEach { add("%L = %L, ", it.name, it.value.code()) }
                fields.lastOrNull()?.let { add("%L = %L", it.name, it.value.code())}
            }
            .build()
}