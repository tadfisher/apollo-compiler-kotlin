package com.apollographql.apollo.compiler.ir

import com.apollographql.apollo.compiler.ast.OperationType
import com.apollographql.apollo.compiler.ast.Value
import com.apollographql.apollo.compiler.ast.VariableValue
import java.util.Locale

interface WithDoc {
    val doc: String
}

interface PropertyWithDoc : WithDoc {
    val propertyName: String
}

interface WithJavaType {
    val javaType: JavaTypeName
}

data class OperationSpec(
    val id: String,
    val name: String,
    override val javaType: JavaTypeName,
    val operation: OperationType,
    val source: String,
    val optional: OptionalType = OptionalType.NULLABLE,
    val variables: OperationVariablesSpec? = null,
    val data: OperationDataSpec
) : WithJavaType

data class SelectionSetSpec(
    val fields: List<ResponseFieldSpec<out SchemaTypeRef>> = emptyList(),
    val fragmentSpreads: List<ResponseFieldSpec<FragmentTypeRef>> = emptyList(),
    val inlineFragments: List<ResponseFieldSpec<InlineFragmentTypeRef>> = emptyList()
) {
    fun withTypename() = copy(fields = listOf(ResponseFieldSpec.typenameField) + fields)
}

data class OperationDataSpec(
    val selections: SelectionSetSpec
)

data class OperationVariablesSpec(
    val variables: List<VariableSpec>
)

data class VariableSpec(
    val name: String,
    val propertyName: String = name.decapitalize(),
    val type: TypeRef,
    val defaultValue: Value? = null
)

data class ResponseFieldSpec<T : TypeRef>(
    val name: String,
    val responseName: String = name,
    override val doc: String = "",
    val type: T,
    val arguments: List<ArgumentSpec> = emptyList(),
    val skipIf: List<VariableValue> = emptyList(),
    val includeIf: List<VariableValue> = emptyList(),
    val typeConditions: List<String> = emptyList(),
    val selections: SelectionSetSpec? = null
) : PropertyWithDoc {
    override val propertyName: String = responseName

    companion object {
        val typenameField = ResponseFieldSpec(
            name = "__typename",
            type = BuiltinTypeRef(BuiltinType.STRING).required()
        )
    }
}

data class InlineFragmentSpec(
    val typeCondition: TypeRef,
    val selections: SelectionSetSpec
)

data class ArgumentSpec(
    val name: String,
    val value: Value,
    val type: TypeRef
)

class JavaTypeName(val packageName: String, val typeName: String)

sealed class TypeRef {
    abstract val name: String
    abstract val optional: OptionalType
    abstract fun required(): TypeRef
    abstract fun nullable(): TypeRef
    abstract fun optional(type: OptionalType): TypeRef

    val isOptional: Boolean by lazy {
        optional != OptionalType.NONNULL
    }

    val unwrapped: TypeRef by lazy {
        if (this is ListTypeRef) ofType.unwrapped else this
    }
}

sealed class SchemaTypeRef : TypeRef()

data class BuiltinTypeRef(
    val kind: BuiltinType,
    override val optional: OptionalType = OptionalType.NULLABLE
) : SchemaTypeRef() {
    override val name: String = kind.graphqlName
    override fun required() = copy(optional = OptionalType.NONNULL)
    override fun nullable() = copy(optional = OptionalType.NULLABLE)
    override fun optional(type: OptionalType) = copy(optional = type)
}

enum class BuiltinType(val graphqlName: String) {
    INT("Int"),
    FLOAT("Float"),
    STRING("String"),
    BOOLEAN("Boolean"),
    ID("ID")
}

data class EnumTypeRef(
    val spec: EnumTypeSpec,
    override val optional: OptionalType = OptionalType.NULLABLE
) : SchemaTypeRef() {
    override val name = spec.name
    override fun required() = copy(optional = OptionalType.NONNULL)
    override fun nullable() = copy(optional = OptionalType.NULLABLE)
    override fun optional(type: OptionalType) = copy(optional = type)
}

data class ObjectTypeRef(
    override val name: String,
    override val javaType: JavaTypeName,
    override val optional: OptionalType = OptionalType.NULLABLE
) : SchemaTypeRef(), WithJavaType {
    override fun required() = copy(optional = OptionalType.NONNULL)
    override fun nullable() = copy(optional = OptionalType.NULLABLE)
    override fun optional(type: OptionalType) = copy(optional = type)
}

data class ListTypeRef(
    val ofType: TypeRef,
    override val optional: OptionalType = OptionalType.NULLABLE
) : SchemaTypeRef() {
    fun of(ofType: TypeRef) = this.copy(ofType = ofType)
    override val name = "[${ofType.name}]"
    override fun required() = copy(optional = OptionalType.NONNULL)
    override fun nullable() = copy(optional = OptionalType.NULLABLE)
    override fun optional(type: OptionalType) = copy(optional = type)
}

data class CustomTypeRef(
    val spec: CustomScalarTypeSpec,
    override val optional: OptionalType = OptionalType.NULLABLE
) : SchemaTypeRef(), WithJavaType {
    override val name = spec.name
    override val javaType = spec.javaType
    override fun required() = copy(optional = OptionalType.NONNULL)
    override fun nullable() = copy(optional = OptionalType.NULLABLE)
    override fun optional(type: OptionalType) = copy(optional = type)
}

object FragmentsWrapperTypeRef : SchemaTypeRef(), WithJavaType {
    override val name = "Fragments"
    override val javaType = JavaTypeName("", name)
    override val optional = OptionalType.NONNULL
    override fun required() = this
    override fun nullable() = throw UnsupportedOperationException()
    override fun optional(type: OptionalType) = throw UnsupportedOperationException()
}

data class FragmentTypeRef(
    val spec: FragmentSpec,
    override val optional: OptionalType = OptionalType.NULLABLE
) : TypeRef(), WithJavaType {
    override val name = spec.name
    override val javaType = spec.javaType
    override fun required() = copy(optional = OptionalType.NONNULL)
    override fun nullable() = copy(optional = OptionalType.NULLABLE)
    override fun optional(type: OptionalType) = copy(optional = type)
}

data class InlineFragmentTypeRef(
    override val name: String,
    val spec: InlineFragmentSpec,
    override val optional: OptionalType = OptionalType.NULLABLE
) : TypeRef() {
    override fun required() = copy(optional = OptionalType.NONNULL)
    override fun nullable() = copy(optional = OptionalType.NULLABLE)
    override fun optional(type: OptionalType) = copy(optional = type)
}

enum class OptionalType(val isWrapperType: Boolean) {
    NONNULL(false),
    NULLABLE(false),
    INPUT(true),
    APOLLO(true),
    JAVA(true),
    GUAVA(true)
}

sealed class TypeDefinitionSpec

data class InputObjectTypeSpec(
    val name: String,
    val doc: String = "",
    val values: List<InputValueSpec>
) : TypeDefinitionSpec()

data class InputValueSpec(
    val name: String,
    override val propertyName: String = name.decapitalize(),
    override val doc: String = "",
    val type: TypeRef,
    val defaultValue: Value? = null
) : PropertyWithDoc

data class EnumTypeSpec(
    val name: String,
    override val javaType: JavaTypeName,
    val doc: String = "",
    val values: List<EnumValueSpec>
) : TypeDefinitionSpec(), WithJavaType {
    companion object {
        val unknownValue = EnumValueSpec(
                name = "_UNKNOWN",
                propertyName = "_UNKNOWN",
                doc = "Auto-generated constant for unknown enum values.")
    }
}

data class EnumValueSpec(
    val name: String,
    override val propertyName: String = name.toUpperCase(Locale.ENGLISH),
    override val doc: String = "",
    val deprecationReason: String? = null
) : PropertyWithDoc

data class CustomTypesSpec(
    val types: List<CustomScalarTypeSpec>
)

data class CustomScalarTypeSpec(
    val name: String,
    val propertyName: String = name.toUpperCase(Locale.ENGLISH),
    override val javaType: JavaTypeName,
    val customTypeName: JavaTypeName
) : TypeDefinitionSpec(), WithJavaType

data class FragmentSpec(
    val name: String,
    override val javaType: JavaTypeName,
    val source: String,
    val typeCondition: String,
    val selections: SelectionSetSpec,
    val possibleTypes: List<String> = emptyList()
) : WithJavaType
