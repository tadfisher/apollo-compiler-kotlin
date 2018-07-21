package com.apollographql.apollo.compiler.introspection

import com.squareup.moshi.JsonClass

enum class TypeKind {
    SCALAR,
    OBJECT,
    INTERFACE,
    UNION,
    ENUM,
    INPUT_OBJECT,
    LIST,
    NON_NULL;
}

sealed class IntrospectionType {
    abstract val kind: TypeKind
    abstract val name: String
    abstract val description: String?
}

sealed class IntrospectionTypeRef {
    abstract val kind: TypeKind
}

@JsonClass(generateAdapter = true)
data class IntrospectionSchema(
        val queryType: IntrospectionNamedTypeRef,
        val mutationType: IntrospectionNamedTypeRef?,
        val subscriptionType: IntrospectionNamedTypeRef?,
        val types: List<IntrospectionType>,
        val directives: List<IntrospectionDirective>
)

@JsonClass(generateAdapter = true)
data class IntrospectionScalarType(
    override val name: String,
    override val description: String?
) : IntrospectionType() {
    override val kind: TypeKind = TypeKind.SCALAR
}

@JsonClass(generateAdapter = true)
data class IntrospectionObjectType(
    override val name: String,
    override val description: String?,
    val fields: List<IntrospectionField>,
    val interfaces: List<IntrospectionNamedTypeRef>
) : IntrospectionType() {
    override val kind: TypeKind = TypeKind.OBJECT
}

@JsonClass(generateAdapter = true)
data class IntrospectionInputObjectType(
    override val name: String,
    override val description: String?,
    val inputFields: List<IntrospectionInputValue>
) : IntrospectionType() {
    override val kind: TypeKind = TypeKind.INPUT_OBJECT
}

@JsonClass(generateAdapter = true)
data class IntrospectionInterfaceType(
    override val name: String,
    override val description: String?,
    val fields: List<IntrospectionField>,
    val possibleTypes: List<IntrospectionNamedTypeRef>
) : IntrospectionType() {
    override val kind: TypeKind = TypeKind.INTERFACE
}

@JsonClass(generateAdapter = true)
data class IntrospectionUnionType(
    override val name: String,
    override val description: String?,
    val possibleTypes: List<IntrospectionNamedTypeRef>
) : IntrospectionType() {
    override val kind: TypeKind = TypeKind.UNION
}

@JsonClass(generateAdapter = true)
data class IntrospectionEnumType(
    override val name: String,
    override val description: String?,
    val enumValues: List<IntrospectionEnumValue>
) : IntrospectionType() {
    override val kind: TypeKind = TypeKind.ENUM
}

@JsonClass(generateAdapter = true)
data class IntrospectionField(
    val name: String,
    val description: String?,
    val args: List<IntrospectionInputValue>,
    val type: IntrospectionTypeRef,
    val isDeprecated: Boolean = false,
    val deprecationReason: String?
) {
    @delegate:Transient val argsByName by lazy { args.associateBy { it.name } }
}

@JsonClass(generateAdapter = true)
data class IntrospectionEnumValue(
    val name: String,
    val description: String?,
    val isDeprecated: Boolean = false,
    val deprecationReason: String?
)

@JsonClass(generateAdapter = true)
data class IntrospectionInputValue(
    val name: String,
    val description: String?,
    val type: IntrospectionTypeRef,
    val defaultValue: String?
)

@JsonClass(generateAdapter = true)
data class IntrospectionNamedTypeRef(
    override val kind: TypeKind = TypeKind.OBJECT,
    val name: String
) : IntrospectionTypeRef()

@JsonClass(generateAdapter = true)
data class IntrospectionListTypeRef(
    val ofType: IntrospectionTypeRef
) : IntrospectionTypeRef() {
    override val kind: TypeKind = TypeKind.LIST
}

@JsonClass(generateAdapter = true)
data class IntrospectionNonNullTypeRef(
    val ofType: IntrospectionTypeRef
) : IntrospectionTypeRef() {
    override val kind: TypeKind = TypeKind.NON_NULL
}

@JsonClass(generateAdapter = true)
data class IntrospectionDirective(
    val name: String,
    val description: String?,
    // TODO val locations: Set<DirectiveLocation>
    val args: List<IntrospectionInputValue>
)