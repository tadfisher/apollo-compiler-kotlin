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

fun Value.argumentValueCode(): CodeBlock {
    return when (this) {
        is ScalarValue -> scalarArgumentValueCode()
        is NullValue -> CodeBlock.of("%S", "null")
        is EnumValue -> CodeBlock.of("%S", value)
        is ListValue -> listArgumentValueCode()
        is ObjectValue -> objectArgumentValueCode()
        is VariableValue -> variableArgumentValueCode()
    }
}

fun ScalarValue.scalarArgumentValueCode() = when (this) {
    is IntValue -> CodeBlock.of("%L", value.intValueExact())
    is FloatValue -> CodeBlock.of("%L", value.toDouble())
    is StringValue -> CodeBlock.of("%S", value)
    is BooleanValue -> CodeBlock.of("%L", value)
}

fun ListValue.listArgumentValueCode(): CodeBlock {
    return value.map { it.argumentValueCode() }
            .join(",\n", prefix = "listOf(\n%>", suffix = "\n%<)")
}

fun ObjectValue.objectArgumentValueCode(): CodeBlock {
    return CodeBlock.builder()
            .add("mapOf(\n")
            .indent()
            .apply {
                fields.forEachIndexed { i, (name, value) ->
                    add("%S to %L", name, value.argumentValueCode())
                    if (i < fields.lastIndex) add(",")
                    add("\n")
                }
            }
            .unindent()
            .add(")")
            .build()
}

fun VariableValue.variableArgumentValueCode(): CodeBlock {
    return CodeBlock.builder()
            .add("mapOf(\n")
            .indent()
            .add("%S to %S,\n", "kind", "Variable")
            .add("%S to %S\n", "variableName", name)
            .unindent()
            .add(")")
            .build()
}