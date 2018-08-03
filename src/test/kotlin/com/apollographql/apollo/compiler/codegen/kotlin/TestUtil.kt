package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.compiler.ir.ResponseFieldSpec
import com.apollographql.apollo.compiler.ir.TypeKind
import com.apollographql.apollo.compiler.ir.TypeRef
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

val characterRef = TypeRef(
        name = "Character",
        kind = TypeKind.OBJECT
)

val heroRef = TypeRef(
        name = "HeroWithReview",
        kind = TypeKind.OBJECT
)

val episodeRef = TypeRef(
        name = "Episode",
        kind = TypeKind.ENUM
)

val unitRef = TypeRef(
        name = "Unit",
        jvmName = Unit::class.qualifiedName!!,
        kind = TypeKind.ENUM
)

val intRef = TypeRef(
        name = "Int",
        jvmName = Int::class.qualifiedName!!,
        kind = TypeKind.INT
)

val booleanRef = TypeRef(
        name = "Boolean",
        jvmName = Boolean::class.qualifiedName!!,
        kind = TypeKind.BOOLEAN
)

val stringRef = TypeRef(
        name = "String",
        jvmName = String::class.qualifiedName!!,
        kind = TypeKind.STRING
)

val floatRef = TypeRef(
        name = "Float",
        jvmName = Double::class.qualifiedName!!,
        kind = TypeKind.DOUBLE
)

val listRef = TypeRef(
        name = "List",
        jvmName = List::class.qualifiedName!!,
        kind = TypeKind.LIST,
        isOptional = true,
        parameters = listOf(stringRef)
)

val idRef = TypeRef(
        name = "CustomType.ID",
        jvmName = String::class.qualifiedName!!,
        kind = TypeKind.CUSTOM,
        isOptional = true
)

val colorInputRef = TypeRef(
        name = "ColorInput",
        kind = TypeKind.OBJECT
)

val reviewRef = TypeRef(
        name = "Review",
        kind = TypeKind.OBJECT
)

val dateRef = TypeRef(
        name = "CustomType.DATE",
        kind = TypeKind.CUSTOM
)

val customRef = TypeRef(
        name = "CustomType.CUSTOM",
        kind = TypeKind.CUSTOM
)

val fragmentsRef = TypeRef(
        name = "Fragments",
        kind = TypeKind.FRAGMENT,
        isOptional = false
)

val typenameSpec = ResponseFieldSpec(
        name = "__typename",
        type = stringRef.copy(isOptional = false)
)
