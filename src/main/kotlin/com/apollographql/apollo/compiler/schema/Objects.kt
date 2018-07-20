package com.apollographql.apollo.compiler.schema

import com.apollographql.apollo.compiler.ast.AstNode
import com.apollographql.apollo.compiler.ast.Directive

sealed class ObjectLikeType<C, V> : OutputType<V>, CompositeType<V>, NullableType, UnmodifiedType, Named, HasAstInfo {
    abstract val interfaces: List<InterfaceType<C, V>>
    abstract val fields: () -> List<Field<C, V>>
}

data class ObjectType<C, V>(
        override val name: String,
        override val description: String?,
        override val fields: () -> List<Field<C, V>>,
        override val interfaces: List<InterfaceType<C, V>>,
        // TODO val instanceCheck: (Any, KClass<*>, ObjectType<C, V>) -> Boolean,
        override val astDirectives: List<Directive>,
        override val astNodes: List<AstNode>
) : ObjectLikeType<C, V>() {
    override fun rename(newName: String): Named = copy(name = newName)
}

data class InterfaceType<C, V>(
        override val name: String,
        override val description: String?,
        override val fields: () -> List<Field<C, V>>,
        override val interfaces: List<InterfaceType<C, V>>,
        val manualPossibleTypes: () -> List<ObjectType<*, *>>,
        override val astDirectives: List<Directive>,
        override val astNodes: List<AstNode>
) : ObjectLikeType<C, V>(), AbstractType {
    override fun rename(newName: String): Named = copy(name = newName)
}

// TODO data class PossibleInterface<C, Concrete>(val interfaceType: InterfaceType<C, *>) {
//    companion object {
//        fun <C, Abstract, Concrete> forInterface(
//                interfaceType: InterfaceType<C, Abstract>
//        ): PossibleInterface<C, Concrete> = PossibleInterface(interfaceType)
//    }
//}

// TODO data class PossibleObject<C, A

// TODO interface PossibleType

data class UnionType<C>(
        override val name: String,
        override val description: String?,
        val types: List<ObjectType<C, *>>,
        override val astDirectives: List<Directive> = emptyList(),
        override val astNodes: List<AstNode> = emptyList()
) : OutputType<Any>, CompositeType<Any>, AbstractType, NullableType, UnmodifiedType, HasAstInfo {
    override fun rename(newName: String): Named = copy(name = newName)
}
