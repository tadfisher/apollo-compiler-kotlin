package com.apollographql.apollo.compiler.introspection

enum class TypeKind {
    Scalar,
    Object,
    Interface,
    Union,
    Enum,
    InputObject,
    List,
    NonNull;


}

data class IntrospectionSchema(
        val queryType: IntrospectionNamedTypeRef,
        val mutationType: IntrospectionNamedTypeRef?,
        val subscriptionType: IntrospectionNamedTypeRef?,
        val types: List<IntrospectionType>,
        val directives: List<IntrospectionDirective>
)

sealed class IntrospectionType {
    abstract val kind: TypeKind.Value
}