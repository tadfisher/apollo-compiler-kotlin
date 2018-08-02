package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.compiler.ast.BooleanValue
import com.apollographql.apollo.compiler.ast.EnumValue
import com.apollographql.apollo.compiler.ast.FloatValue
import com.apollographql.apollo.compiler.ast.IntValue
import com.apollographql.apollo.compiler.ast.ListValue
import com.apollographql.apollo.compiler.ast.NullValue
import com.apollographql.apollo.compiler.ast.ObjectValue
import com.apollographql.apollo.compiler.ast.ScalarValue
import com.apollographql.apollo.compiler.ast.StringValue
import com.apollographql.apollo.compiler.ast.Value
import com.apollographql.apollo.compiler.ast.VariableValue
import com.squareup.kotlinpoet.CodeBlock

fun Value.valueCode(): CodeBlock {
    return when (this) {
        is ScalarValue -> valueCode()
        is NullValue -> CodeBlock.of("null")
        is EnumValue -> valueCode()
        is ListValue -> valueCode()
        is ObjectValue -> valueCode()
        is VariableValue -> valueCode()
    }
}

fun ScalarValue.valueCode(): CodeBlock {
    return when (this) {
        is IntValue -> CodeBlock.of("%L", value.intValueExact())
        is FloatValue -> CodeBlock.of("%L", value.toDouble())
        is StringValue -> CodeBlock.of("%S", value)
        is BooleanValue -> CodeBlock.of("%L", value)
    }
}

fun EnumValue.valueCode() = CodeBlock.of("%L", value)

fun ListValue.valueCode(): CodeBlock {
    return CodeBlock.of("""
        listOf(
        %>%L
        %<)
    """.trimIndent(), value.map { CodeBlock.of("%L", it.valueCode()) }.join(",\n"))
}

fun ObjectValue.valueCode() = CodeBlock.of("%L", fields.map {
    CodeBlock.of("%L = %L", it.name, it.value.valueCode() )
}.join("\n"))
