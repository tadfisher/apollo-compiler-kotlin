package com.apollographql.apollo.compiler.ir

object Builtins {

    val stringTypeRef = TypeRef(
            name = "String",
            jvmName = String::class.java.canonicalName,
            kind = TypeKind.STRING,
            isOptional = false
    )

    val idTypeSpec = ScalarTypeSpec(
            name = "ID",
            type = stringTypeRef
    )
}