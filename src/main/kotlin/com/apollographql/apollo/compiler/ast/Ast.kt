package com.apollographql.apollo.compiler.ast

import com.apollographql.apollo.compiler.parsing.SourceMapper
import java.math.BigDecimal
import java.math.BigInteger

data class AstLocation(
        val sourceId: String,
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

    val argumentsByName: Map<String, Value>
        get() = arguments.associate { it.name to it.value }
}

interface WithDescription {
    val description: StringValue?
}

interface WithDirectives {
    val directives: List<Directive>

    val directivesByName: Map<String, Map<String, Value>>
        get() = directives.associate { it.name to it.argumentsByName }
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
        override val location: AstLocation? = null,
        val sourceMapper: SourceMapper? = null
) : AstNode {
    val operations: Map<String?, OperationDefinition> by lazy {
        definitions.filterIsInstance<OperationDefinition>().associateBy { it.name }
    }

    val fragments: Map<String, FragmentDefinition> by lazy {
        definitions.filterIsInstance<FragmentDefinition>().associateBy { it.name }
    }

    val types: Map<String, TypeDefinition> by lazy {
        definitions.filterIsInstance<TypeDefinition>().associateBy { it.name }
    }

    val typeExtensions: Map<String, TypeExtension> by lazy {
        definitions.filterIsInstance<TypeExtension>().associateBy { it.name }
    }

    val directives: Map<String, DirectiveDefinition> by lazy {
        definitions.filterIsInstance<DirectiveDefinition>().associateBy { it.name }
    }

    // TODO val source: String = sourceMapper.map

    fun operationType(operationName: String? = null): OperationType? =
            operation(operationName)?.operationType

    fun operation(operationName: String? = null): OperationDefinition? {
        return if (operationName.isNullOrEmpty() && operations.size != 1) { null }
        else { operations[operationName] }
    }

    fun merge(other: Document) = listOf(this, other).merge()

    operator fun plus(other: Document) = merge(other)

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
        val operationType: OperationType = OperationType.QUERY,
        val name: String? = null,
        val variables: List<VariableDefinition> = emptyList(),
        override val directives: List<Directive> = emptyList(),
        override val selections: List<Selection>,
        override val location: AstLocation? = null
) : ExecutableDefinition(), WithDirectives

enum class OperationType { QUERY, MUTATION, SUBSCRIPTION }

data class Field(
        val name: String,
        val alias: String? = null,
        override val arguments: List<Argument> = emptyList(),
        override val directives: List<Directive> = emptyList(),
        override val selections: List<Selection> = emptyList(),
        override val location: AstLocation? = null
) : Selection, SelectionContainer, WithArguments {
    val responseName: String by lazy { alias ?: name }

    val skipIf: VariableValue? by lazy {
        directivesByName["skip"]?.get("if") as? VariableValue
    }

    val includeIf: VariableValue? by lazy {
        directivesByName["include"]?.get("if") as? VariableValue
    }
}

data class Argument(
        override val name: String,
        override val value: Value,
        override val location: AstLocation? = null
) : NameValue

data class FragmentSpread(
        val name: String,
        override val directives: List<Directive>,
        override val location: AstLocation? = null
) : Selection {
    val skipIf: VariableValue? by lazy {
        directivesByName["skip"]?.get("if") as? VariableValue
    }

    val includeIf: VariableValue? by lazy {
        directivesByName["include"]?.get("if") as? VariableValue
    }
}

data class InlineFragment(
        override val typeCondition: NamedType?,
        override val directives: List<Directive>,
        override val selections: List<Selection>,
        override val location: AstLocation? = null
) : Selection, ConditionalFragment, SelectionContainer {
    val skipIf: VariableValue? by lazy {
        directivesByName["skip"]?.get("if") as? VariableValue
    }

    val includeIf: VariableValue? by lazy {
        directivesByName["include"]?.get("if") as? VariableValue
    }
}

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
        val value: BigInteger,
        override val location: AstLocation? = null
) : ScalarValue()

data class FloatValue(
        val value: BigDecimal,
        override val location: AstLocation? = null
) : ScalarValue()

data class StringValue(
        val value: String,
        val block: Boolean = false,
        val blockRawValue: String? = null,
        override val location: AstLocation? = null
) : ScalarValue() {
    companion object {
        private const val tripleQuote = "\"\"\""
        private const val quote = "\""

        fun valueOf(input: String, location: AstLocation? = null): StringValue {
            return if (input.startsWith(tripleQuote) && input.endsWith(tripleQuote)) {
                StringValue(
                        value = input.removeSurrounding(tripleQuote).trimIndent(),
                        block = true,
                        blockRawValue = input,
                        location = location
                )
            } else {
                StringValue(value = input.removeSurrounding(quote), location = location)
            }
        }
    }
}

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
        val defaultValue: String?,
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
        override val directives: List<Directive> = emptyList(),
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

    abstract fun rename(newName: String): TypeDefinition
}

sealed class TypeExtension : TypeSystemExtension(), WithDirectives {
    abstract val name: String
}

data class ScalarTypeDefinition(
        override val name: String,
        override val directives: List<Directive> = emptyList(),
        override val description: StringValue? = null,
        override val location: AstLocation? = null
) : TypeDefinition() {
    override fun rename(newName: String) = copy(name = newName)
}

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
) : TypeDefinition() {
    override fun rename(newName: String) = copy(name = newName)
}

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
) : AstNode, WithDirectives, WithDescription {
    val deprecationReason: String? by lazy {
        directivesByName["deprecated"]?.let { args ->
            (args["reason"] as? StringValue)?.value
                    ?: "No longer supported"
        }
    }
}

data class InputValueDefinition(
        val name: String,
        val valueType: Type,
        val defaultValue: String?,
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
) : TypeDefinition() {
    override fun rename(newName: String) = copy(name = newName)
}

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
) : TypeDefinition() {
    override fun rename(newName: String) = copy(name = newName)
}

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
) : TypeDefinition() {
    override fun rename(newName: String) = copy(name = newName)
}

data class EnumValueDefinition(
        val name: String,
        override val directives: List<Directive> = emptyList(),
        override val description: StringValue? = null,
        override val location: AstLocation? = null
) : AstNode, WithDirectives, WithDescription {
    val deprecationReason: String? by lazy {
        directivesByName["deprecated"]?.let { args ->
            (args["reason"] as? StringValue)?.value
                    ?: "No longer supported"
        }
    }
}

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
) : TypeDefinition(), WithDescription {
    override fun rename(newName: String) = copy(name = newName)
}

data class InputObjectTypeExtension(
        override val name: String,
        val fields: List<InputValueDefinition>,
        override val directives: List<Directive> = emptyList(),
        override val location: AstLocation? = null
) : TypeExtension()

data class DirectiveDefinition(
        val name: String,
        val arguments: List<InputValueDefinition>,
        val locations: Set<DirectiveLocation>,
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
    INPUT_FIELD_DEFINITION;
}
