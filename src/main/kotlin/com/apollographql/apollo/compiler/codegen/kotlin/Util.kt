package com.apollographql.apollo.compiler.codegen.kotlin

import com.squareup.kotlinpoet.CodeBlock

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