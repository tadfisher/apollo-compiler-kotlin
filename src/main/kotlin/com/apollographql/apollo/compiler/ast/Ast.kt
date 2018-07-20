package com.apollographql.apollo.compiler.ast

import java.math.BigDecimal
import java.math.BigInteger

interface ConditionalFragment {
    val typeCondition: NamedType?
}

interface WithComments {
    val comments: List<Comment>
}

interface WithDescription {
    val description: StringValue?
}

interface WithTrailingComments {
    val trailingComments: List<Comment>
}

interface SelectionContainer : WithComments, WithTrailingComments {
    val selections: List<Selection>
}

data class OperationDefinition(
        val operationType: OperationType = OperationType.Query,
        val name: String? = null,
        val variables: List<VariableDefinition> = emptyList(),
        override val directives: List<Directive> = emptyList(),
        override val selections: List<Selection>,
        override val comments: List<Comment> = emptyList(),
        override val trailingComments: List<Comment> = emptyList(),
        override val location: AstLocation? = null
) : Definition(), WithDirectives, SelectionContainer

data class FragmentDefinition(
        val name: String,
        override val typeCondition: NamedType,
        override val directives: List<Directive>,
        override val selections: List<Selection>,
        val variables: List<VariableDefinition> = emptyList(),
        override val comments: List<Comment> = emptyList(),
        override val trailingComments: List<Comment> = emptyList(),
        override val location: AstLocation? = null
) : Definition(), ConditionalFragment, WithDirectives, SelectionContainer

enum class OperationType { Query, Mutation, Subscription }

data class VariableDefinition(
        val name: String,
        val type: Type,
        val defaultValue: Value?,
        override val comments: List<Comment> = emptyList(),
        override val location: AstLocation? = null
) : AstNode(), WithComments

data class NamedType(val name: String, override val location: AstLocation? = null) : Type()
data class NotNullType(val ofType: Type, override val location: AstLocation? = null) : Type()
data class ListType(val ofType: Type, override val location: AstLocation? = null) : Type()

interface WithArguments {
    val arguments: List<Argument>
}

data class Field(
        val alias: String?,
        val name: String,
        override val arguments: List<Argument>,
        override val directives: List<Directive>,
        override val selections: List<Selection>,
        override val comments: List<Comment> = emptyList(),
        override val trailingComments: List<Comment> = emptyList(),
        override val location: AstLocation? = null
) : Selection(), SelectionContainer, WithArguments

data class FragmentSpread(
        val name: String,
        override val directives: List<Directive>,
        override val comments: List<Comment> = emptyList(),
        override val location: AstLocation? = null
) : Selection()

data class InlineFragment(
        override val typeCondition: NamedType?,
        override val directives: List<Directive>,
        override val selections: List<Selection>,
        override val comments: List<Comment> = emptyList(),
        override val trailingComments: List<Comment> = emptyList(),
        override val location: AstLocation? = null
) : Selection(), ConditionalFragment, SelectionContainer

sealed class NameValue : AstNode(), WithComments {
    abstract val name: String
    abstract val value: Value
}

interface WithDirectives {
    val directives: List<Directive>
}

data class Directive(
        val name: String,
        override val arguments: List<Argument>,
        val comments: List<Comment> = emptyList(),
        override val location: AstLocation? = null
) : AstNode(), WithArguments

data class Argument(
        override val name: String,
        override val value: Value,
        override val comments: List<Comment> = emptyList(),
        override val location: AstLocation? = null
) : NameValue()

sealed class Value : AstNode(), WithComments {
    // TODO fun renderPretty: String
}

sealed class ScalarValue : Value()

data class IntValue(
        val value: Int,
        override val comments: List<Comment> = emptyList(),
        override val location: AstLocation? = null
) : ScalarValue()

data class BigIntValue(
        val value: BigInteger,
        override val comments: List<Comment> = emptyList(),
        override val location: AstLocation? = null
) : ScalarValue()

data class FloatValue(
        val value: Double,
        override val comments: List<Comment> = emptyList(),
        override val location: AstLocation? = null
) : ScalarValue()

data class BigDecimalValue(
        val value: BigDecimal,
        override val comments: List<Comment> = emptyList(),
        override val location: AstLocation? = null
) : ScalarValue()

data class StringValue(
        val value: String,
        val block: Boolean = false,
        val blockRawValue: String? = null,
        override val comments: List<Comment> = emptyList(),
        override val location: AstLocation? = null
) : ScalarValue()

data class BooleanValue(
        val value: Boolean,
        override val comments: List<Comment> = emptyList(),
        override val location: AstLocation? = null
) : ScalarValue()

data class EnumValue(
        val value: String,
        override val comments: List<Comment> = emptyList(),
        override val location: AstLocation? = null
) : Value()

data class ListValue(
        val value: List<Value>,
        override val comments: List<Comment> = emptyList(),
        override val location: AstLocation? = null
) : Value()

data class VariableValue(
        val name: String,
        override val comments: List<Comment> = emptyList(),
        override val location: AstLocation? = null
) : Value()

data class NullValue(
        override val comments: List<Comment> = emptyList(),
        override val location: AstLocation? = null
) : Value()

data class ObjectValue(
        val fields: List<ObjectField>,
        override val comments: List<Comment> = emptyList(),
        override val location: AstLocation? = null
) : Value() {
    val fieldsByName by lazy {
        fields.associate { it.name to it.value }
    }
}

data class ObjectField(
        override val name: String,
        override val value: Value,
        override val comments: List<Comment> = emptyList(),
        override val location: AstLocation? = null
) : NameValue()

data class Comment(val text: String, override val location: AstLocation? = null) : AstNode()

// Schema definition

data class ScalarTypeDefinition(
        override val name: String,
        override val directives: List<Directive> = emptyList(),
        override val description: StringValue? = null,
        override val comments: List<Comment> = emptyList(),
        override val location: AstLocation? = null
) : TypeDefinition()

data class FieldDefinition(
        val name: String,
        val fieldType: Type,
        val arguments: List<Argument>,
        override val directives: List<Directive> = emptyList(),
        override val description: StringValue? = null,
        override val comments: List<Comment> = emptyList(),
        override val location: AstLocation? = null
) : SchemaAstNode(), WithDirectives, WithDescription

data class InputValueDefinition(
        val name: String,
        val valueType: Type,
        val defaultValue: Value?,
        override val directives: List<Directive> = emptyList(),
        override val description: StringValue? = null,
        override val comments: List<Comment> = emptyList(),
        override val location: AstLocation? = null
) : SchemaAstNode(), WithDirectives, WithDescription

data class ObjectTypeDefinition(
        override val name: String,
        val interfaces: List<NamedType>,
        val fields: List<FieldDefinition>,
        override val directives: List<Directive>,
        override val description: StringValue? = null,
        override val comments: List<Comment> = emptyList(),
        override val trailingComments: List<Comment> = emptyList(),
        override val location: AstLocation? = null
) : TypeDefinition(), WithTrailingComments

data class InterfaceTypeDefinition(
        override val name: String,
        val fields: List<FieldDefinition>,
        override val directives: List<Directive> = emptyList(),
        override val description: StringValue? = null,
        override val comments: List<Comment> = emptyList(),
        override val trailingComments: List<Comment> = emptyList(),
        override val location: AstLocation? = null
) : TypeDefinition(), WithTrailingComments

data class UnionTypeDefinition(
        override val name: String,
        val types: List<NamedType>,
        override val directives: List<Directive> = emptyList(),
        override val description: StringValue? = null,
        override val comments: List<Comment> = emptyList(),
        override val trailingComments: List<Comment> = emptyList(),
        override val location: AstLocation? = null
) : TypeDefinition(), WithTrailingComments

data class EnumTypeDefinition(
        override val name: String,
        val values: List<EnumValueDefinition>,
        override val directives: List<Directive> = emptyList(),
        override val description: StringValue? = null,
        override val comments: List<Comment> = emptyList(),
        override val trailingComments: List<Comment> = emptyList(),
        override val location: AstLocation? = null
) : TypeDefinition(), WithTrailingComments

data class EnumValueDefinition(
        val name: String,
        override val directives: List<Directive> = emptyList(),
        override val description: StringValue? = null,
        override val comments: List<Comment> = emptyList(),
        override val location: AstLocation? = null
) : SchemaAstNode(), WithDirectives, WithDescription

data class InputObjectTypeDefinition(
        override val name: String,
        val fields: List<InputValueDefinition>,
        override val directives: List<Directive> = emptyList(),
        override val description: StringValue? = null,
        override val comments: List<Comment> = emptyList(),
        override val trailingComments: List<Comment> = emptyList(),
        override val location: AstLocation? = null
) : TypeDefinition(), WithTrailingComments, WithDescription

data class ObjectTypeExtensionDefinition(
        override val name: String,
        val interfaces: List<NamedType>,
        override val fields: List<FieldDefinition>,
        override val directives: List<Directive> = emptyList(),
        override val comments: List<Comment> = emptyList(),
        override val trailingComments: List<Comment> = emptyList(),
        override val location: AstLocation? = null
) : ObjectLikeTypeExtensionDefinition(), WithTrailingComments

data class InterfaceTypeExtensionDefinition(
        override val name: String,
        override val fields: List<FieldDefinition>,
        override val directives: List<Directive> = emptyList(),
        override val comments: List<Comment> = emptyList(),
        override val trailingComments: List<Comment> = emptyList(),
        override val location: AstLocation? = null
) : ObjectLikeTypeExtensionDefinition(), WithTrailingComments

data class InputObjectTypeExtensionDefinition(
        override val name: String,
        val fields: List<InputValueDefinition>,
        override val directives: List<Directive> = emptyList(),
        override val comments: List<Comment> = emptyList(),
        override val trailingComments: List<Comment> = emptyList(),
        override val location: AstLocation? = null
) : TypeExtensionDefinition(), WithTrailingComments

data class EnumTypeExtensionDefinition(
        override val name: String,
        val values: List<EnumValueDefinition>,
        override val directives: List<Directive> = emptyList(),
        override val comments: List<Comment> = emptyList(),
        override val trailingComments: List<Comment> = emptyList(),
        override val location: AstLocation? = null
) : TypeExtensionDefinition(), WithTrailingComments

data class UnionTypeExtensionDefinition(
        override val name: String,
        val types: List<NamedType>,
        override val directives: List<Directive> = emptyList(),
        override val comments: List<Comment> = emptyList(),
        override val location: AstLocation? = null
) : TypeExtensionDefinition()

data class ScalarTypeExtensionDefinition(
        override val name: String,
        override val directives: List<Directive> = emptyList(),
        override val comments: List<Comment> = emptyList(),
        override val location: AstLocation? = null
) : TypeExtensionDefinition()

data class SchemaExtensionDefinition(
        val operationTypes: List<OperationTypeDefinition>,
        override val directives: List<Directive> = emptyList(),
        override val comments: List<Comment> = emptyList(),
        override val trailingComments: List<Comment> = emptyList(),
        override val location: AstLocation? = null
) : TypeSystemExtensionDefinition(), WithDirectives, WithTrailingComments

data class DirectiveDefinition(
        val name: String,
        val arguments: List<InputValueDefinition>,
        val locations: List<DirectiveLocation>,
        override val description: StringValue? = null,
        override val comments: List<Comment> = emptyList(),
        override val location: AstLocation? = null
) : TypeSystemDefinition(), WithDescription

data class DirectiveLocation(
        val name: String,
        override val comments: List<Comment> = emptyList(),
        override val location: AstLocation? = null
) : SchemaAstNode()

data class SchemaDefinition(
        val operationTypes: List<OperationTypeDefinition>,
        override val directives: List<Directive>,
        override val comments: List<Comment> = emptyList(),
        override val trailingComments: List<Comment> = emptyList(),
        override val location: AstLocation? = null
) : TypeSystemDefinition(), WithTrailingComments, WithDirectives

data class OperationTypeDefinition(
        val operation: OperationType,
        val type: NamedType,
        override val comments: List<Comment> = emptyList(),
        override val location: AstLocation? = null
) : SchemaAstNode()

data class AstLocation(
        val sourceId: String = "",
        val index: Int,
        val line: Int,
        val column: Int
)

sealed class AstNode {
    abstract val location: AstLocation?
}

sealed class Selection : AstNode(), WithDirectives, WithComments

sealed class Definition : AstNode()

sealed class Type : AstNode() {
    val namedType: NamedType
        get() {
            fun loop(type: Type): NamedType = when (type) {
                is NotNullType -> loop(type.ofType)
                is ListType -> loop(type.ofType)
                is NamedType -> type
            }
            return loop(this)
        }
}

sealed class SchemaAstNode : AstNode(), WithComments
sealed class TypeSystemDefinition : SchemaAstNode()
sealed class TypeSystemExtensionDefinition : SchemaAstNode()

sealed class TypeDefinition : TypeSystemDefinition(), WithDirectives, WithDescription {
    abstract val name: String
}

sealed class TypeExtensionDefinition : TypeSystemExtensionDefinition(), WithDirectives {
    abstract val name: String
}

sealed class ObjectLikeTypeExtensionDefinition : TypeExtensionDefinition() {
    abstract val fields: List<FieldDefinition>
}