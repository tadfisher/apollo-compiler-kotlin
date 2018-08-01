package com.apollographql.apollo.compiler.introspection

import com.apollographql.apollo.compiler.ast.Argument
import com.apollographql.apollo.compiler.ast.Definition
import com.apollographql.apollo.compiler.ast.Directive
import com.apollographql.apollo.compiler.ast.DirectiveDefinition
import com.apollographql.apollo.compiler.ast.Document
import com.apollographql.apollo.compiler.ast.EnumTypeDefinition
import com.apollographql.apollo.compiler.ast.EnumValueDefinition
import com.apollographql.apollo.compiler.ast.FieldDefinition
import com.apollographql.apollo.compiler.ast.InputObjectTypeDefinition
import com.apollographql.apollo.compiler.ast.InputValueDefinition
import com.apollographql.apollo.compiler.ast.InterfaceTypeDefinition
import com.apollographql.apollo.compiler.ast.ListType
import com.apollographql.apollo.compiler.ast.NamedType
import com.apollographql.apollo.compiler.ast.NonNullType
import com.apollographql.apollo.compiler.ast.ObjectTypeDefinition
import com.apollographql.apollo.compiler.ast.OperationType
import com.apollographql.apollo.compiler.ast.OperationTypeDefinition
import com.apollographql.apollo.compiler.ast.ScalarTypeDefinition
import com.apollographql.apollo.compiler.ast.SchemaDefinition
import com.apollographql.apollo.compiler.ast.StringValue
import com.apollographql.apollo.compiler.ast.Type
import com.apollographql.apollo.compiler.ast.TypeDefinition
import com.apollographql.apollo.compiler.ast.UnionTypeDefinition

fun IntrospectionSchema.resolveDocument() = Document(
        definitions = listOf<Definition>(schemaDefinition())
                + types.map { it.typeDefinition() }
                + directives.map { it.directiveDefinition() }
)

fun IntrospectionSchema.schemaDefinition() = SchemaDefinition(
        operationTypes = listOfNotNull(
                queryType.operationTypeDefinition(OperationType.QUERY),
                mutationType?.operationTypeDefinition(OperationType.MUTATION),
                subscriptionType?.operationTypeDefinition(OperationType.SUBSCRIPTION)
        )
)

fun IntrospectionNamedTypeRef.operationTypeDefinition(operation: OperationType) =
        OperationTypeDefinition(operation, namedType())

fun IntrospectionType.typeDefinition(): TypeDefinition = when (this) {
    is IntrospectionScalarType -> scalarTypeDefinition()
    is IntrospectionObjectType -> objectTypeDefinition()
    is IntrospectionInputObjectType -> inputObjectTypeDefinition()
    is IntrospectionInterfaceType -> interfaceTypeDefinition()
    is IntrospectionUnionType -> unionTypeDefinition()
    is IntrospectionEnumType -> enumTypeDefinition()
}

fun IntrospectionScalarType.scalarTypeDefinition() = ScalarTypeDefinition(
        name = name,
        description = description?.let { StringValue(it) }
)

fun IntrospectionObjectType.objectTypeDefinition() = ObjectTypeDefinition(
        name = name,
        interfaces = interfaces.map { it.namedType() },
        fields = fields.map { it.fieldDefinition() },
        description = description?.let { StringValue(it) }
)

fun IntrospectionInputObjectType.inputObjectTypeDefinition() = InputObjectTypeDefinition(
        name = name,
        fields = inputFields.map { it.inputValueDefinition() },
        description = description?.let { StringValue(it) }
)

fun IntrospectionInterfaceType.interfaceTypeDefinition() = InterfaceTypeDefinition(
        name = name,
        fields = fields.map { it.fieldDefinition() },
        description = description?.let { StringValue(it) }
)

fun IntrospectionUnionType.unionTypeDefinition() = UnionTypeDefinition(
        name = name,
        types = possibleTypes.map { it.namedType() },
        description = description?.let { StringValue(it) }
)

fun IntrospectionEnumType.enumTypeDefinition() = EnumTypeDefinition(
        name = name,
        values = enumValues.map { it.enumValueDefinition() },
        description = description?.let { StringValue(it) }
)

fun IntrospectionDirective.directiveDefinition() = DirectiveDefinition(
        name = name,
        arguments = args.map { it.inputValueDefinition() },
        locations = locations,
        description = description?.let { StringValue(it) }
)

fun IntrospectionField.fieldDefinition() = FieldDefinition(
        name = name,
        fieldType = type.type(),
        arguments = args.map { it.inputValueDefinition() },
        directives = if (isDeprecated) listOf(deprecatedDirective(deprecationReason)) else emptyList(),
        description = description?.let { StringValue(it) }
)

fun IntrospectionTypeRef.type(): Type = when (this) {
    is IntrospectionNamedTypeRef -> namedType()
    is IntrospectionListTypeRef -> listType()
    is IntrospectionNonNullTypeRef -> nonNullType()
}

fun IntrospectionNamedTypeRef.namedType() = NamedType(name)
fun IntrospectionListTypeRef.listType() = ListType(ofType = ofType.type())
fun IntrospectionNonNullTypeRef.nonNullType() = NonNullType(ofType = ofType.type())

fun IntrospectionInputValue.inputValueDefinition() = InputValueDefinition(
        name = name,
        valueType = type.type(),
        defaultValue = defaultValue,
        description = description?.let { StringValue(it) }
)

fun IntrospectionEnumValue.enumValueDefinition() = EnumValueDefinition(
        name = name,
        directives = if (isDeprecated) listOf(deprecatedDirective(deprecationReason)) else emptyList(),
        description = description?.let { StringValue(it) }
)

fun deprecatedDirective(reason: String?) = Directive(
        name = "deprecated",
        arguments = listOf(Argument(
                name = "reason",
                value =  StringValue(reason ?: "No longer supported")
        ))
)