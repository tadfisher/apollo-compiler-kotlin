package com.apollographql.apollo.compiler.codegen.kotlin

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

fun String.dropImports() =
        lines().dropWhile { it.startsWith("import") || it.isBlank() }.joinToString("\n")

fun TypeSpec.code() = FileSpec.get("", this).toString().dropImports().trim()
fun FunSpec.code() =
        FileSpec.builder("", "code").addFunction(this).build().toString().dropImports().trim()
fun PropertySpec.code() =
        FileSpec.builder("", "code").addProperty(this).build().toString().dropImports().trim()
