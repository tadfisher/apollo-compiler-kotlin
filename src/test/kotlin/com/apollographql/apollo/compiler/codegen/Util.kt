package com.apollographql.apollo.compiler.codegen

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.PropertySpec

fun String.dropImports() =
        lines().dropWhile { it.startsWith("import") || it.isBlank() }.joinToString("\n")

fun PropertySpec.wrapInFile() = FileSpec.builder("", "code").addProperty(this).build()