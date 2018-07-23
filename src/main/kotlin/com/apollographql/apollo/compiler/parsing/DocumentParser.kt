package com.apollographql.apollo.compiler.parsing

import com.apollographql.apollo.compiler.ast.*
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import com.apollographql.apollo.compiler.ast.StringValue as StringValueAst
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.TokenStream
import java.math.BigDecimal
import java.math.BigInteger
import java.util.UUID

class DocumentParser(
        private val source: DocumentSource,
        private val withAstLocation: Boolean = false
) : GraphqlParser(CommonTokenStream(GraphqlLexer(source.charStream()))) {

    private val ParserRuleContext.astLocation: AstLocation? get() {
        return if (withAstLocation) {
            AstLocation(source.id, start.startIndex, start.line, start.charPositionInLine)
        } else {
            null
        }
    }

    fun documentAst(): Document = document().toAst()
    fun valueAst(const: Boolean) = if (const) value_const().toAst() else value().toAst()

    fun DocumentContext.toAst() = Document(
            definitions = definition().map { it.toAst() },
            location = astLocation,
            sourceMapper = source.mapper
    )

    fun DefinitionContext.toAst(): Definition {
        return executableDefinition()?.toAst()
                ?: typeSystemDefinition()?.toAst()
                ?: typeSystemExtension().toAst()
    }

    fun ExecutableDefinitionContext.toAst(): Definition {
        return operationDefinition()?.toAst()
                ?: fragmentDefinition().toAst()
    }

    fun OperationDefinitionContext.toAst() = OperationDefinition(
            operationType = operationType()?.toAst()
                    ?: OperationType.Query,
            name = Name().text,
            variables = variableDefinitions()?.toAst() ?: emptyList(),
            directives = directives()?.toAst() ?: emptyList(),
            selections = selectionSet().toAst(),
            location = astLocation
    )

    fun OperationTypeContext.toAst(): OperationType {
        return when (text) {
            "query" -> OperationType.Query
            "mutation" -> OperationType.Mutation
            "subscription" -> OperationType.Subscription
            else -> throw ParseError("Unknown operation type: $this")
        }
    }

    fun SelectionSetContext.toAst() = selection().map { it.toAst() }
    fun SelectionContext.toAst(): Selection {
        return field()?.toAst()
                ?: fragmentSpread()?.toAst()
                ?: inlineFragment().toAst()
    }

    fun FieldContext.toAst() = Field(
            alias = alias()?.Name()?.text,
            name = Name().text,
            arguments = arguments()?.toAst() ?: emptyList(),
            directives = directives()?.toAst() ?: emptyList(),
            selections = selectionSet()?.toAst() ?: emptyList(),
            location = astLocation
    )

    fun ArgumentsContext.toAst() = argument().map { it.toAst() }
    fun Arguments_constContext.toAst() = argument_const().map { it.toAst() }

    fun ArgumentContext.toAst() = Argument(
            name = Name().text,
            value = value().toAst(),
            location = astLocation
    )

    fun Argument_constContext.toAst() = Argument(
            name = Name().text,
            value = value_const().toAst(),
            location = astLocation
    )

    fun FragmentSpreadContext.toAst() = FragmentSpread(
            name = fragmentName().Name().text,
            directives = directives()?.toAst() ?: emptyList(),
            location = astLocation
    )

    fun InlineFragmentContext.toAst() = InlineFragment(
            typeCondition = typeCondition()?.toAst(),
            directives = directives()?.toAst() ?: emptyList(),
            selections = selectionSet().toAst(),
            location = astLocation
    )

    fun FragmentDefinitionContext.toAst() = FragmentDefinition(
            name = fragmentName().Name().text,
            typeCondition = typeCondition().toAst(),
            directives = directives()?.toAst() ?: emptyList(),
            selections = selectionSet().toAst(),
            location = astLocation
    )

    fun TypeConditionContext.toAst(): NamedType = namedType().toAst()

    fun ValueContext.toAst(): Value {
        return variable()?.toAst()
                ?: IntValue()?.let { com.apollographql.apollo.compiler.ast.IntValue(BigInteger(it.text), astLocation) }
                ?: FloatValue()?.let { com.apollographql.apollo.compiler.ast.FloatValue(BigDecimal(it.text), astLocation) }
                ?: StringValue()?.let { StringValueAst.valueOf(it.text, astLocation) }
                ?: booleanValue()?.toAst()
                ?: nullValue()?.toAst()
                ?: enumValue()?.toAst()
                ?: listValue()?.toAst()
                ?: objectValue().toAst()
    }

    fun Value_constContext.toAst(): Value {
        return IntValue()?.let { com.apollographql.apollo.compiler.ast.IntValue(BigInteger(it.symbol.text), astLocation) }
                ?: FloatValue()?.let { com.apollographql.apollo.compiler.ast.FloatValue(BigDecimal(it.text), astLocation) }
                ?: StringValue()?.let { StringValueAst.valueOf(it.text, astLocation) }
                ?: booleanValue()?.toAst()
                ?: nullValue()?.toAst()
                ?: enumValue()?.toAst()
                ?: listValue_const()?.toAst()
                ?: objectValue_const().toAst()
    }

    fun BooleanValueContext.toAst() = BooleanValue(value = text!!.toBoolean(), location = astLocation)

    fun NullValueContext.toAst() = NullValue(location = astLocation)

    fun EnumValueContext.toAst() = EnumValue(value = Name().text, location = astLocation)

    fun ListValueContext.toAst() = ListValue(value = value().map { it.toAst() }, location = astLocation)
    fun ListValue_constContext.toAst() = ListValue(value = value_const().map { it.toAst() }, location = astLocation)

    fun ObjectValueContext.toAst() = ObjectValue(fields = objectField().map { it.toAst() }, location = astLocation)
    fun ObjectValue_constContext.toAst() =
            ObjectValue(fields = objectField_const().map { it.toAst() }, location = astLocation)

    fun ObjectFieldContext.toAst() = ObjectField(name = Name().text, value = value().toAst(), location = astLocation)
    fun ObjectField_constContext.toAst() =
            ObjectField(name = Name().text, value = value_const().toAst(), location = astLocation)

    fun VariableDefinitionsContext.toAst() = variableDefinition().map { it.toAst() }
    fun VariableDefinitionContext.toAst() = VariableDefinition(
            name = variable().Name().text,
            type = type().toAst(),
            defaultValue = defaultValue()?.toAst(),
            location = astLocation
    )

    fun VariableContext.toAst() = VariableValue(name = Name().text, location = astLocation)

    fun DefaultValueContext.toAst() = value_const().toAst()

    fun TypeContext.toAst(): Type {
        return namedType()?.toAst()
                ?: listType()?.toAst()
                ?: nonNullType().toAst()
    }

    fun NamedTypeContext.toAst() = NamedType(name = Name().text, location = astLocation)

    fun ListTypeContext.toAst() = ListType(ofType = type().toAst(), location = astLocation)

    fun NonNullTypeContext.toAst() = NonNullType(
            ofType = listType()?.toAst() ?: namedType().toAst(),
            location = astLocation
    )

    fun DirectivesContext.toAst() = directive().map { it.toAst() }
    fun Directives_constContext.toAst() = directive_const().map { it.toAst() }

    fun DirectiveContext.toAst() = Directive(
            name = Name().text,
            arguments = arguments()?.toAst() ?: emptyList(),
            location = astLocation
    )

    fun Directive_constContext.toAst() = Directive(
            name = Name().text,
            arguments = arguments_const()?.toAst() ?: emptyList(),
            location = astLocation
    )

    fun TypeSystemDefinitionContext.toAst(): TypeSystemDefinition {
        return schemaDefinition()?.toAst() as? TypeSystemDefinition
                ?: typeDefinition()?.toAst() as? TypeSystemDefinition
                ?: directiveDefinition().toAst() as TypeSystemDefinition
    }

    fun TypeSystemExtensionContext.toAst(): TypeSystemExtension {
        return schemaExtension()?.toAst() as? TypeSystemExtension
                ?: typeExtension().toAst() as TypeSystemExtension
    }

    fun SchemaDefinitionContext.toAst() = SchemaDefinition(
            operationTypes = operationTypeDefinition().map { it.toAst() },
            directives = directives_const()?.toAst() ?: emptyList(),
            location = astLocation
    )

    fun SchemaExtensionContext.toAst() = SchemaExtension(
            operationTypes = operationTypeDefinition()?.map { it.toAst() } ?: emptyList(),
            directives = directives_const()?.toAst() ?: emptyList(),
            location = astLocation
    )

    fun OperationTypeDefinitionContext.toAst() = OperationTypeDefinition(
            operation = operationType().toAst(),
            type = namedType().toAst(),
            location = astLocation
    )

    fun DescriptionContext.toAst() = StringValueAst.valueOf(text, astLocation)

    fun TypeDefinitionContext.toAst(): TypeDefinition {
        return scalarTypeDefinition()?.toAst()
                ?: objectTypeDefinition()?.toAst()
                ?: interfaceTypeDefinition()?.toAst()
                ?: unionTypeDefinition()?.toAst()
                ?: enumTypeDefinition()?.toAst()
                ?: inputObjectTypeDefinition().toAst()
    }

    fun TypeExtensionContext.toAst(): TypeExtension {
        return scalarTypeExtension()?.toAst()
                ?: objectTypeExtension()?.toAst()
                ?: interfaceTypeExtension()?.toAst()
                ?: unionTypeExtension()?.toAst()
                ?: enumTypeExtension()?.toAst()
                ?: inputObjectTypeExtension().toAst()
    }

    fun ScalarTypeDefinitionContext.toAst() = ScalarTypeDefinition(
            name = Name().text,
            directives = directives_const()?.toAst() ?: emptyList(),
            description = description()?.toAst(),
            location = astLocation
    )

    fun ScalarTypeExtensionContext.toAst() = ScalarTypeExtension(
            name = Name().text,
            directives = directives_const().toAst(),
            location = astLocation
    )

    fun ObjectTypeDefinitionContext.toAst() = ObjectTypeDefinition(
            name = Name().text,
            interfaces = implementsInterfaces()?.toAst() ?: emptyList(),
            fields = fieldsDefinition()?.toAst() ?: emptyList(),
            directives = directives_const()?.toAst() ?: emptyList(),
            description = description()?.toAst(),
            location = astLocation
    )

    fun ObjectTypeExtensionContext.toAst() = ObjectTypeExtension(
            name = Name().text,
            interfaces = implementsInterfaces()?.toAst() ?: emptyList(),
            fields = fieldsDefinition()?.toAst() ?: emptyList(),
            directives = directives_const()?.toAst() ?: emptyList(),
            location = astLocation
    )

    fun ImplementsInterfacesContext.toAst(): List<NamedType> {
        tailrec fun loop(ctx: ImplementsInterfacesContext, res: List<NamedType> = emptyList()): List<NamedType> {
            val newRes = res + ctx.namedType().toAst()
            val next = ctx.implementsInterfaces()
            return if (next == null) newRes else loop(next, newRes)
        }
        return loop(this)
    }

    fun FieldsDefinitionContext.toAst() = fieldDefinition().map { it.toAst() }
    fun FieldDefinitionContext.toAst() = FieldDefinition(
            name = Name().text,
            fieldType = type().toAst(),
            arguments = argumentsDefinition()?.toAst() ?: emptyList(),
            directives = directives_const()?.toAst() ?: emptyList(),
            description = description()?.toAst(),
            location = astLocation
    )

    fun ArgumentsDefinitionContext.toAst() = inputValueDefinition().map { it.toAst() }
    fun InputValueDefinitionContext.toAst() = InputValueDefinition(
            name = Name().text,
            valueType = type().toAst(),
            defaultValue = defaultValue()?.toAst(),
            directives = directives_const()?.toAst() ?: emptyList(),
            description = description()?.toAst(),
            location = astLocation
    )

    fun InterfaceTypeDefinitionContext.toAst() = InterfaceTypeDefinition(
            name = Name().text,
            fields = fieldsDefinition()?.toAst() ?: emptyList(),
            directives = directives_const()?.toAst() ?: emptyList(),
            description = description()?.toAst(),
            location = astLocation
    )

    fun InterfaceTypeExtensionContext.toAst() = InterfaceTypeExtension(
            name = Name().text,
            fields = fieldsDefinition()?.toAst() ?: emptyList(),
            directives = directives_const()?.toAst() ?: emptyList(),
            location = astLocation
    )

    fun UnionTypeDefinitionContext.toAst() = UnionTypeDefinition(
            name = Name().text,
            types = unionMemberTypes()?.toAst() ?: emptyList(),
            directives = directives_const()?.toAst() ?: emptyList(),
            description = description()?.toAst(),
            location = astLocation
    )

    fun UnionMemberTypesContext.toAst(): List<NamedType> {
        tailrec fun loop(ctx: UnionMemberTypesContext, res: List<NamedType> = emptyList()): List<NamedType> {
            val newRes = res + ctx.namedType().toAst()
            val next = ctx.unionMemberTypes()
            return if (next == null) newRes else loop(next, newRes)
        }
        return loop(this)
    }

    fun UnionTypeExtensionContext.toAst() = UnionTypeExtension(
            name = Name().text,
            types = unionMemberTypes()?.toAst() ?: emptyList(),
            directives = directives_const()?.toAst() ?: emptyList(),
            location = astLocation
    )

    fun EnumTypeDefinitionContext.toAst() = EnumTypeDefinition(
            name = Name().text,
            values = enumValuesDefinition()?.toAst() ?: emptyList(),
            directives = directives_const()?.toAst() ?: emptyList(),
            description = description()?.toAst(),
            location = astLocation
    )

    fun EnumValuesDefinitionContext.toAst() = enumValueDefinition().map { it.toAst() }
    fun EnumValueDefinitionContext.toAst() = EnumValueDefinition(
            name = enumValue().Name().text,
            directives = directives_const()?.toAst() ?: emptyList(),
            description = description()?.toAst(),
            location = astLocation
    )

    fun EnumTypeExtensionContext.toAst() = EnumTypeExtension(
            name = Name().text,
            values = enumValuesDefinition()?.toAst() ?: emptyList(),
            directives = directives_const()?.toAst() ?: emptyList(),
            location = astLocation
    )

    fun InputObjectTypeDefinitionContext.toAst() = InputObjectTypeDefinition(
            name = Name().text,
            fields = inputFieldsDefinition()?.toAst() ?: emptyList(),
            directives = directives_const()?.toAst() ?: emptyList(),
            description = description()?.toAst(),
            location = astLocation
    )

    fun InputFieldsDefinitionContext.toAst() = inputValueDefinition().map { it.toAst() }

    fun InputObjectTypeExtensionContext.toAst() = InputObjectTypeExtension(
            name = Name().text,
            fields = inputFieldsDefinition()?.toAst() ?: emptyList(),
            directives = directives_const()?.toAst() ?: emptyList(),
            location = astLocation
    )

    fun DirectiveDefinitionContext.toAst() = DirectiveDefinition(
            name = Name().text,
            arguments = argumentsDefinition()?.toAst() ?: emptyList(),
            locations = directiveLocations().toAst(),
            description = description()?.toAst(),
            location = astLocation
    )

    fun DirectiveLocationsContext.toAst(): List<DirectiveLocation> {
        tailrec fun loop(
                ctx: DirectiveLocationsContext,
                res: List<DirectiveLocation> = emptyList()
        ): List<DirectiveLocation> {
            val newRes = res + ctx.directiveLocation().toAst()
            val next = ctx.directiveLocations()
            return if (next == null) newRes else loop(next, newRes)
        }
        return loop(this)
    }

    fun DirectiveLocationContext.toAst(): DirectiveLocation {
        return executableDirectiveLocation()?.toAst()
                ?: typeSystemDirectiveLocation().toAst()
    }

    fun ExecutableDirectiveLocationContext.toAst() = DirectiveLocation.valueOf(text)

    fun TypeSystemDirectiveLocationContext.toAst() = DirectiveLocation.valueOf(text)

}