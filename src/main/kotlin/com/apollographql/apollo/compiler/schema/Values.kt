package com.apollographql.apollo.compiler.schema

import com.apollographql.apollo.compiler.ast.AstNode
import com.apollographql.apollo.compiler.ast.Directive

data class EnumType<T>(
        override val name: String,
        override val description: String? = null,
        val values: List<EnumValue<T>>,
        override val astDirectives: List<Directive> = emptyList(),
        override val astNodes: List<AstNode> = emptyList()
) : InputType<T>, OutputType<T>, LeafType, NullableType, UnmodifiedType, Named, HasAstInfo {

    val byName: Map<String, EnumValue<T>> by lazy { values.associateBy { it.name } }
    val byValue: Map<T, EnumValue<T>> by lazy { values.associateBy { it.value } }

    override fun rename(newName: String): EnumType<T> = copy(name = newName)
}

data class EnumValue<out T>(
        override val name: String,
        override val description: String?,
        val value: T,
        override val deprecationReason: String? = null,
        override val astDirectives: List<Directive> = emptyList(),
        override val astNodes: List<AstNode> = emptyList()
) : Named, HasDeprecation, HasAstInfo {
    override fun rename(newName: String): EnumValue<T> = copy(name = newName)
}

data class InputObjectType<T>(
        override val name: String,
        override val description: String? = null,
        // TODO val fields: () -> List<InputField<*>>,
        override val astDirectives: List<Directive> = emptyList(),
        override val astNodes: List<AstNode> = emptyList()
) : InputType<T>, NullableType, UnmodifiedType, Named, HasAstInfo {
    override fun rename(newName: String): InputObjectType<T> = copy(name = newName)
}

//data class InputField<T>(
//
//) : InputValue<T>, Named, HasAstInfo