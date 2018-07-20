package com.apollographql.apollo.compiler.schema

import com.apollographql.apollo.compiler.ast.AstNode
import com.apollographql.apollo.compiler.ast.Directive as AstDirective

data class Field<C, V>(
        override val name: String,
        val type: OutputType<*>,
        override val description: String?,
        override val arguments: List<Argument<*>>,
        //val resolve: (Context<C, V>) -> Action<C, *>,
        override val deprecationReason: String?,
        //val tags: List<FieldTag>,
        // val complexity: ((C, Args, Double) -> Double)?,
        // val manualPossibleTypes: () -> List<ObjectType<*, *>>,
        override val astDirectives: List<AstDirective>,
        override val astNodes: List<AstNode>
) : Named, HasArguments, HasDeprecation, HasAstInfo {
    override fun rename(newName: String): Named = copy(name = newName)

    // TODO fun withPossibleTypes
    // TODO fun toAst
}

data class Argument<T>(
        val name: String,
        val argumentType: InputType<*>,
        val description: String?,
        // TODO val defaultValue: ToInput<*, *>?
        // TODO val fromInput: FromInput<*>,
        override val astDirectives: List<AstDirective>,
        override val astNodes: List<AstNode>
) : HasAstInfo