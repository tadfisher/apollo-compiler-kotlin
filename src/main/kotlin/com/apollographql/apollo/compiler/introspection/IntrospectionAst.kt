package com.apollographql.apollo.compiler.introspection

import com.apollographql.apollo.compiler.ast.*

//fun IntrospectionType.toAst(): TypeDefinition {
//    return when (this) {
//        is IntrospectionObjectType -> toAst()
//    }
//}

fun IntrospectionTypeRef.toAst(): Type {
    return when (this) {
        is IntrospectionListTypeRef -> ListType(ofType.toAst())
        is IntrospectionNonNullTypeRef -> NonNullType(ofType.toAst())
        is IntrospectionNamedTypeRef -> NamedType(name)
    }
}

fun IntrospectionObjectType.toAst() = ObjectTypeDefinition(
        name = name,
        interfaces = interfaces.map { NamedType(it.name) },
        fields = fields.map { it.toAst() }
)

fun IntrospectionField.toAst() = FieldDefinition(
        name = name,
        fieldType = type.toAst(),
        arguments = args.map { it.toAst() }
)

fun IntrospectionInputValue.toAst() = InputValueDefinition(
        name = name,
        valueType = type.toAst(),
        defaultValue = null
        // TODO parse input value: defaultValue = defaultValue?.let {  }
)