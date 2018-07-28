package com.apollographql.apollo.compiler.ir

import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.compiler.ast.OperationType

data class OperationSpec(
        val id: String,
        val name: String,
        val operation: OperationType,
        val data: SelectionSetSpec,
        val variables: List<VariableSpec>
)

sealed class SelectionSetSpec(
        val fields: List<FieldSpec>,
        val fragmentSpreads: List<FragmentSpreadSpec>,
        val inlineFragments: List<InlineFragmentSpec>
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
        val defaultValue: String?
)

/**
 * Resolved field; includes type and input values from the field definition.
 */
data class FieldSpec(
        val name: String,
        val responseName: String,
        val type: TypeRef,
        val responseType: ResponseField.Type,
        val selections: SelectionSetSpec?
)

data class FragmentSpreadSpec(
        val fragmentName: String,
        val selections: SelectionSetSpec
)

data class InlineFragmentSpec(
        val typeCondition: TypeRef?,
        val selections: SelectionSetSpec
)

/**
 * @param name GraphQL type name
 * @param jvmName Fully-qualified JVM type name or primitive
 */
data class TypeRef(
        val name: String,
        val jvmName: String
)