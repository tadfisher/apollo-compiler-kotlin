package com.apollographql.apollo.compiler.ir

import com.apollographql.apollo.compiler.ast.OperationType
import com.apollographql.apollo.compiler.ast.Value
import com.apollographql.apollo.compiler.ast.VariableValue
import com.apollographql.apollo.compiler.util.lazyPlus
import java.util.Locale
import kotlin.reflect.KClass

interface WithDoc {
    val doc: String
}

interface PropertyWithDoc : WithDoc {
    val propertyName: String
}

data class OperationSpec(
    val id: String,
    val name: String,
    val operation: OperationType,
    val source: String,
    val optionalType: KClass<*>? = null,
    val variables: OperationVariablesSpec? = null,
    val data: OperationDataSpec
)

data class SelectionSetSpec(
    val fields: List<ResponseFieldSpec> = emptyList(),
    val fragmentSpreads: List<FragmentSpreadSpec> = emptyList(),
    val inlineFragments: List<InlineFragmentSpec> = emptyList()
)

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

data class ResponseFieldSpec(
    val name: String,
    val responseName: String = name,
    val typeName: String = name.capitalize(),
    override val doc: String = "",
    val type: TypeRef,
    val arguments: List<ArgumentSpec> = emptyList(),
    val skipIf: List<VariableValue> = emptyList(),
    val includeIf: List<VariableValue> = emptyList(),
    val typeConditions: List<TypeRef> = emptyList(),
    val selections: SelectionSetSpec? = null
) : PropertyWithDoc {
    override val propertyName: String = responseName
}

data class FragmentSpreadSpec(
    val fragment: FragmentSpec,
    val propertyName: String = fragment.name.decapitalize(),
    val isOptional: Boolean = false,
    val optionalType: KClass<*>? = null
)

data class InlineFragmentSpec(
    val typeCondition: TypeRef?,
    val selections: SelectionSetSpec
)

data class ArgumentSpec(
    val name: String,
    val value: Value,
    val type: TypeRef
)

/**
 * @param name GraphQL type name
 * @param jvmName Fully-qualified JVM type name or primitive
 */
data class TypeRef(
    val name: String,
    val jvmName: String = name,
    val kind: TypeKind,
    val isOptional: Boolean = true,
    val optionalType: KClass<*>? = null,
    val parameters: List<TypeRef> = emptyList()
) {
    /**
     * Returns a depth-first sequence of nested types.
     */
    fun nestedTypes(): Sequence<TypeRef> {
        return sequenceOf(this) lazyPlus {
            parameters.asSequence().flatMap { it.nestedTypes() }
        }
    }

    /**
     * True if this or any nested type reference refers to a custom type.
     */
    val isCustom: Boolean by lazy {
        nestedTypes().any { it.kind == TypeKind.CUSTOM }
    }
}

enum class TypeKind(val readMethod: String, val writeMethod: String, val factoryMethod: String) {
    STRING("readString", "writeString", "forString"),
    INT("readInt", "writeInt", "forInt"),
    LONG("readLong", "writeLong", "forLong"),
    DOUBLE("readDouble", "writeDouble", "forDouble"),
    BOOLEAN("readBoolean", "writeBoolean", "forBoolean"),
    ENUM("readString", "writeString", "forString"),
    OBJECT("readObject", "writeObject", "forObject"),
    LIST("readList", "writeList", "forList"),
    CUSTOM("readCustomType", "writeCustom", "forCustomType"),
    FRAGMENT("readConditional", "", "forFragment"),
    INLINE_FRAGMENT("readConditional", "", "forInlineFragment")
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
    val doc: String = "",
    val values: List<EnumValueSpec>
) : TypeDefinitionSpec() {
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
    val types: List<ScalarTypeSpec>
)

data class ScalarTypeSpec(
    val name: String,
    val type: TypeRef
) : TypeDefinitionSpec()

data class FragmentSpec(
    val name: String,
    val jvmName: String = name.capitalize(),
    val source: String,
    val selections: SelectionSetSpec,
    val typeCondition: TypeRef? = null,
    val possibleTypes: List<String> = emptyList()
)
