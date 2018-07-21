package com.apollographql.apollo.compiler.ast

data class AstLocation(
        val index: Int,
        val line: Int,
        val column: Int
)

interface AstNode {
    val location: AstLocation?
}

interface NameValue : AstNode {
    val name: String
    val value: Value
}

interface Selection : AstNode, WithDirectives

interface ConditionalFragment {
    val typeCondition: NamedType?
}

interface WithArguments {
    val arguments: List<Argument>
}

interface WithDescription {
    val description: StringValue?
}

interface WithDirectives {
    val directives: List<Directive>
}

interface SelectionContainer {
    val selections: List<Selection>
}

sealed class Definition : AstNode

sealed class ExecutableDefinition : Definition(), WithDirectives, SelectionContainer

sealed class ObjectLikeTypeExtension : TypeExtension() {
    abstract val fields: List<FieldDefinition>
}

data class Document(
        val definitions: List<Definition>,
        override val location: AstLocation? = null
        // TODO val sourceMapper: SourceMapper? = null
) : AstNode {
    val operations: Map<String?, OperationDefinition> by lazy {
        definitions.filterIsInstance<OperationDefinition>().associateBy { it.name }
    }

    val fragments: Map<String, FragmentDefinition> by lazy {
        definitions.filterIsInstance<FragmentDefinition>().associateBy { it.name }
    }

    // TODO val source: String = sourceMapper.map

    fun operationType(operationName: String? = null): OperationType? =
            operation(operationName)?.operationType

    fun operation(operationName: String? = null): OperationDefinition? {
        return if (operationName.isNullOrEmpty() && operations.size != 1) { null }
        else { operations[operationName] }
    }

    // TODO fun withoutSourceMapper = copy(sourceMapper = null)

    fun merge(other: Document) = listOf(this, other).merge()

    operator fun plus(other: Document) = merge(other)

    // TODO val analyzer by lazy { DocumentAnalyzer(this) }

    // TODO lazy val separateOperations: Map<String?, Document> = analyzer.separateOperations

    // TODO fun separateOperation(definition: OperationDefinition) = analyzer.separateOperation(definition)
    // TODO fun separateOperation(operationName: String?) = analyzer.separateOperation(operationName)

    companion object {
        val emptyStub: Document = Document(definitions = listOf(ObjectTypeDefinition(
                name = "Query",
                interfaces = emptyList(),
                fields = listOf(FieldDefinition(
                        name = "stub",
                        fieldType = NamedType("String"),
                        arguments = emptyList()
                ))
        )))
    }
}

// TODO merge sourceMappers
fun Iterable<Document>.merge(): Document =
        Document(definitions = flatMap { it.definitions })

data class OperationDefinition(
        val operationType: OperationType = OperationType.Query,
        val name: String? = null,
        val variables: List<VariableDefinition> = emptyList(),
        override val directives: List<Directive> = emptyList(),
        override val selections: List<Selection>,
        override val location: AstLocation? = null
) : ExecutableDefinition(), WithDirectives

enum class OperationType { Query, Mutation, Subscription }

data class Field(
        val alias: String?,
        val name: String,
        override val arguments: List<Argument>,
        override val directives: List<Directive>,
        override val selections: List<Selection>,
        override val location: AstLocation? = null
) : Selection, SelectionContainer, WithArguments

data class Argument(
        override val name: String,
        override val value: Value,
        override val location: AstLocation? = null
) : NameValue

data class FragmentSpread(
        val name: String,
        override val directives: List<Directive>,
        override val location: AstLocation? = null
) : Selection

data class InlineFragment(
        override val typeCondition: NamedType?,
        override val directives: List<Directive>,
        override val selections: List<Selection>,
        override val location: AstLocation? = null
) : Selection, ConditionalFragment, SelectionContainer

data class FragmentDefinition(
        val name: String,
        override val typeCondition: NamedType,
        override val directives: List<Directive>,
        override val selections: List<Selection>,
        override val location: AstLocation? = null
) : ExecutableDefinition(), ConditionalFragment, WithDirectives

sealed class Value : AstNode

sealed class ScalarValue : Value()

data class IntValue(
        val value: Int,
        override val location: AstLocation? = null
) : ScalarValue()

data class FloatValue(
        val value: Double,
        override val location: AstLocation? = null
) : ScalarValue()

data class StringValue(
        val value: String,
        val block: Boolean = false,
        val blockRawValue: String? = null,
        override val location: AstLocation? = null
) : ScalarValue()

data class BooleanValue(
        val value: Boolean,
        override val location: AstLocation? = null
) : ScalarValue()

data class NullValue(
        override val location: AstLocation? = null
) : Value()

data class EnumValue(
        val value: String,
        override val location: AstLocation? = null
) : Value()

data class ListValue(
        val value: List<Value>,
        override val location: AstLocation? = null
) : Value()

data class ObjectValue(
        val fields: List<ObjectField>,
        override val location: AstLocation? = null
) : Value() {
    val fieldsByName by lazy {
        fields.associate { it.name to it.value }
    }
}

data class ObjectField(
        override val name: String,
        override val value: Value,
        override val location: AstLocation? = null
) : NameValue

data class VariableDefinition(
        val name: String,
        val type: Type,
        val defaultValue: Value?,
        override val location: AstLocation? = null
) : AstNode

data class VariableValue(
        val name: String,
        override val location: AstLocation? = null
) : Value()

sealed class Type : AstNode {
    val namedType: NamedType
        get() {
            tailrec fun loop(type: Type): NamedType = when (type) {
                is NonNullType -> loop(type.ofType)
                is ListType -> loop(type.ofType)
                is NamedType -> type
            }
            return loop(this)
        }
}

data class NamedType(val name: String, override val location: AstLocation? = null) : Type()
data class ListType(val ofType: Type, override val location: AstLocation? = null) : Type()
data class NonNullType(val ofType: Type, override val location: AstLocation? = null) : Type()

data class Directive(
        val name: String,
        override val arguments: List<Argument>,
        override val location: AstLocation? = null
) : AstNode, WithArguments

sealed class TypeSystemDefinition : Definition()
sealed class TypeSystemExtension : Definition()

data class SchemaDefinition(
        val operationTypes: List<OperationTypeDefinition>,
        override val directives: List<Directive>,
        override val location: AstLocation? = null
) : TypeSystemDefinition(), WithDirectives

data class SchemaExtension(
        val operationTypes: List<OperationTypeDefinition>,
        override val directives: List<Directive> = emptyList(),
        override val location: AstLocation? = null
) : TypeSystemExtension(), WithDirectives

data class OperationTypeDefinition(
        val operation: OperationType,
        val type: NamedType,
        override val location: AstLocation? = null
) : TypeSystemDefinition()

sealed class TypeDefinition : TypeSystemDefinition(), WithDirectives, WithDescription {
    abstract val name: String
}

sealed class TypeExtension : TypeSystemExtension(), WithDirectives {
    abstract val name: String
}

data class ScalarTypeDefinition(
        override val name: String,
        override val directives: List<Directive> = emptyList(),
        override val description: StringValue? = null,
        override val location: AstLocation? = null
) : TypeDefinition()

data class ScalarTypeExtension(
        override val name: String,
        override val directives: List<Directive> = emptyList(),
        override val location: AstLocation? = null
) : TypeExtension()

data class ObjectTypeDefinition(
        override val name: String,
        val interfaces: List<NamedType>,
        val fields: List<FieldDefinition>,
        override val directives: List<Directive> = emptyList(),
        override val description: StringValue? = null,
        override val location: AstLocation? = null
) : TypeDefinition()

data class ObjectTypeExtension(
        override val name: String,
        val interfaces: List<NamedType>,
        override val fields: List<FieldDefinition>,
        override val directives: List<Directive> = emptyList(),
        override val location: AstLocation? = null
) : ObjectLikeTypeExtension()

data class FieldDefinition(
        val name: String,
        val fieldType: Type,
        val arguments: List<InputValueDefinition>,
        override val directives: List<Directive> = emptyList(),
        override val description: StringValue? = null,
        override val location: AstLocation? = null
) : AstNode, WithDirectives, WithDescription

data class InputValueDefinition(
        val name: String,
        val valueType: Type,
        val defaultValue: Value?,
        override val directives: List<Directive> = emptyList(),
        override val description: StringValue? = null,
        override val location: AstLocation? = null
) : AstNode, WithDirectives, WithDescription

data class InterfaceTypeDefinition(
        override val name: String,
        val fields: List<FieldDefinition>,
        override val directives: List<Directive> = emptyList(),
        override val description: StringValue? = null,
        override val location: AstLocation? = null
) : TypeDefinition()

data class InterfaceTypeExtension(
        override val name: String,
        override val fields: List<FieldDefinition>,
        override val directives: List<Directive> = emptyList(),
        override val location: AstLocation? = null
) : ObjectLikeTypeExtension()

data class UnionTypeDefinition(
        override val name: String,
        val types: List<NamedType>,
        override val directives: List<Directive> = emptyList(),
        override val description: StringValue? = null,
        override val location: AstLocation? = null
) : TypeDefinition()

data class UnionTypeExtension(
        override val name: String,
        val types: List<NamedType>,
        override val directives: List<Directive> = emptyList(),
        override val location: AstLocation? = null
) : TypeExtension()

data class EnumTypeDefinition(
        override val name: String,
        val values: List<EnumValueDefinition>,
        override val directives: List<Directive> = emptyList(),
        override val description: StringValue? = null,
        override val location: AstLocation? = null
) : TypeDefinition()

data class EnumValueDefinition(
        val name: String,
        override val directives: List<Directive> = emptyList(),
        override val description: StringValue? = null,
        override val location: AstLocation? = null
) : AstNode, WithDirectives, WithDescription

data class EnumTypeExtension(
        override val name: String,
        val values: List<EnumValueDefinition>,
        override val directives: List<Directive> = emptyList(),
        override val location: AstLocation? = null
) : TypeExtension()

data class InputObjectTypeDefinition(
        override val name: String,
        val fields: List<InputValueDefinition>,
        override val directives: List<Directive> = emptyList(),
        override val description: StringValue? = null,
        override val location: AstLocation? = null
) : TypeDefinition(), WithDescription

data class InputObjectTypeExtension(
        override val name: String,
        val fields: List<InputValueDefinition>,
        override val directives: List<Directive> = emptyList(),
        override val location: AstLocation? = null
) : TypeExtension()

data class DirectiveDefinition(
        val name: String,
        val arguments: List<InputValueDefinition>,
        val locations: List<DirectiveLocation>,
        override val description: StringValue? = null,
        override val location: AstLocation? = null
) : TypeSystemDefinition(), WithDescription

enum class DirectiveLocation {
    // Executable
    QUERY,
    MUTATION,
    SUBSCRIPTION,
    FIELD,
    FRAGMENT_DEFINITION,
    FRAGMENT_SPREAD,
    INLINE_FRAGMENT,

    // Type system
    SCHEMA,
    SCALAR,
    OBJECT,
    FIELD_DEFINITION,
    ARGUMENT_DEFINITION,
    INTERFACE,
    UNION,
    ENUM,
    ENUM_VALUE,
    INPUT_OBJECT,
    INPUT_FIELD_DEFINITION
}
