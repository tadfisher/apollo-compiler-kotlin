package com.apollographql.apollo.compiler.codegen

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

fun String.dropImports() =
        lines().dropWhile { it.startsWith("import") || it.isBlank() }.joinToString("\n")

fun TypeSpec.wrapInFile() = FileSpec.get("", this)
fun FunSpec.wrapInFile() = FileSpec.builder("", "code").addFunction(this).build()
fun PropertySpec.wrapInFile() = FileSpec.builder("", "code").addProperty(this).build()