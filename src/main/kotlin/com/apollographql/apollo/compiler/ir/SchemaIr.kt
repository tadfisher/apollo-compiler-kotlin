package com.apollographql.apollo.compiler.ir

import com.apollographql.apollo.compiler.ast.Value
import java.util.Locale

sealed class TypeDefinitionSpec {
    abstract val name: String
    abstract val doc: String
}

data class InputObjectTypeSpec(
        override val name: String,
        override val doc: String = "",
        val values: List<InputValueSpec>
) : TypeDefinitionSpec()

data class InputValueSpec(
        override val name: String,
        val propertyName: String = name.decapitalize(),
        override val doc: String = "",
        val type: TypeRef,
        val defaultValue: Value? = null
) : TypeDefinitionSpec()

data class EnumTypeSpec(
        override val name: String,
        override val doc: String = "",
        val values: List<EnumValueSpec>
) : TypeDefinitionSpec()

data class EnumValueSpec(
        val name: String,
        val propertyName: String = name.toUpperCase(Locale.ENGLISH),
        val doc: String = "",
        val deprecationReason: String? = null
) {
    val isDeprecated: Boolean get() = deprecationReason != null
}

data class ScalarTypeSpec(
        override val name: String,
        override val doc: String = "",
        val type: TypeRef
) : TypeDefinitionSpec()