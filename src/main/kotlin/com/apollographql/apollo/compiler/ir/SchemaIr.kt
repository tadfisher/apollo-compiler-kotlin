package com.apollographql.apollo.compiler.ir

import com.apollographql.apollo.compiler.ast.Value
import java.util.Locale

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
) : PropertyWithDoc {
    val isDeprecated: Boolean get() = deprecationReason != null
}

data class CustomTypesSpec(
        val types: List<ScalarTypeSpec>
)

data class ScalarTypeSpec(
        val name: String,
        val type: TypeRef
) : TypeDefinitionSpec()