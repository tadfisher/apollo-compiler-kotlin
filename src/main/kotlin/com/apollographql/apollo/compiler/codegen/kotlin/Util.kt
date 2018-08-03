package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.compiler.ir.PropertyWithDoc
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeSpec
import javax.annotation.Generated

fun TypeSpec.Builder.addDeprecatedAnnotation(reason: String): TypeSpec.Builder {
    return addAnnotation(AnnotationSpec.builder(Deprecated::class)
            .addMember("%S", reason)
            .build())
}

fun TypeSpec.Builder.addGeneratedAnnotation(): TypeSpec.Builder {
    return addAnnotation(AnnotationSpec.builder(Generated::class)
            .addMember("%S", "Apollo GraphQL")
            .build())
}

fun List<CodeBlock>.join(
        separator: String = ",%W",
        prefix: String = "",
        suffix: String = "",
        transform: (CodeBlock) -> CodeBlock = { it }
): CodeBlock {
    return CodeBlock.builder()
            .add(prefix)
            .apply {
                mapIndexed { i, block ->
                    add(transform(block))
                    if (i < lastIndex) add(separator)
                }
            }
            .add(suffix)
            .build()
}

fun <T> List<T>.joinToCodeBlock(
        separator: String = ",%W",
        prefix: String = "",
        suffix: String = "",
        transform: (T) -> CodeBlock = { CodeBlock.of("%L", it) }
): CodeBlock = map { transform(it) }.join(separator, prefix, suffix)

fun List<PropertyWithDoc>.parameterKdoc(): CodeBlock {
    return mapNotNull { item ->
        item.doc.takeUnless { it.isEmpty() }?.let { Pair(item.propertyName, it) }
    }.joinToCodeBlock("\n", suffix = "\n") { (name, doc) ->
        CodeBlock.of("@param %L %L", name, doc)
    }
}