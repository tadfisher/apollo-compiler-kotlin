package com.apollographql.apollo.compiler.ir

import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.compiler.ast.OperationType
import com.apollographql.apollo.compiler.ast.Value
import com.apollographql.apollo.compiler.ast.VariableValue

data class OperationSpec(
        val id: String,
        val name: String,
        val operation: OperationType,
        val data: OperationDataSpec,
        val variables: OperationVariablesSpec?
)

data class SelectionSetSpec(
        val fields: List<ResponseFieldSpec>,
        val fragmentSpreads: List<FragmentSpreadSpec> = emptyList(),
        val inlineFragments: List<InlineFragmentSpec> = emptyList()
)

data class OperationDataSpec(
        val selections: SelectionSetSpec
)

data class OperationVariablesSpec(
        val variables: List<VariableSpec>
)

data class OperationTypesSpec(
        val types: List<OperationType>
)

data class VariableSpec(
        val name: String,
        val type: TypeRef,
        val defaultValue: Value?
)

/**
 * Resolved field; includes type and input values from the field definition.
 */
data class ResponseFieldSpec(
        val name: String,
        val responseName: String = name,
        val type: TypeRef,
        val responseType: ResponseField.Type,
        val arguments: List<ArgumentSpec> = emptyList(),
        val skipIf: List<VariableValue> = emptyList(),
        val includeIf: List<VariableValue> = emptyList(),
        val typeConditions: List<TypeRef> = emptyList(),
        val selections: SelectionSetSpec? = null
)

data class FragmentSpreadSpec(
        val fragmentName: String,
        val selections: SelectionSetSpec
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
        val jvmName: String,
        val kind: TypeKind,
        val isOptional: Boolean,
        val parameters: List<TypeRef>
)

enum class TypeKind(val readMethod: String, val writeMethod: String) {
    STRING("readString", "writeString"),
    INT("readInt", "writeInt"),
    LONG("readLong", "writeLong"),
    DOUBLE("readDouble", "writeDouble"),
    BOOLEAN("readBoolean", "writeBoolean"),
    ENUM("readString", "writeString"),
    OBJECT("readObject", "writeObject"),
    LIST("readList", "writeList"),
    CUSTOM("readCustomType", "writeCustom")
}