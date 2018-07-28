package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.compiler.ast.ListType
import com.apollographql.apollo.compiler.ast.NamedType
import com.apollographql.apollo.compiler.ast.NonNullType
import com.apollographql.apollo.compiler.ast.Type
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.plusParameter
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName

fun Type.typeName(): TypeName {
    return when (this) {
        is NamedType -> resolve().asNullable()
        is ListType -> List::class.asClassName().plusParameter(ofType.typeName())
        is NonNullType -> ofType.typeName().asNonNullable()
    }
}

fun NamedType.resolve(): TypeName {

}
