package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.api.GraphqlFragment
import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.LIST
import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.STRING
import com.apollographql.apollo.compiler.ir.FragmentSpec
import com.apollographql.apollo.compiler.util.update
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

fun FragmentSpec.typeSpec(className: ClassName): TypeSpec {

    val fragmentDefinition =
            PropertySpec.builder(Fragments.fragmentDefinitionProp, STRING)
                    .addModifiers(KModifier.CONST)
                    .initializer("%S", source)
                    .build()

    val possibleTypes =
            PropertySpec.builder(Fragments.possibleTypesProp, LIST.parameterizedBy(STRING))
                    .initializer("listOf(%L)", possibleTypes.joinToCodeBlock {
                        CodeBlock.of("%S", it)
                    })
                    .build()

    return with (selections.dataClassSpec(className).toBuilder()) {
        addGeneratedAnnotation()

        addSuperinterface(GraphqlFragment::class)

        typeSpecs.update({ it.isCompanion }) { it.toBuilder()
                .addProperty(fragmentDefinition)
                .addProperty(possibleTypes)
                .build()
        }

        addFunction(FunSpec.builder(Fragments.typenameFun)
                .addModifiers(KModifier.OVERRIDE)
                .returns(STRING)
                .addCode("return %L\n", Selections.typenameField)
                .build())

        build()
    }
}

object Fragments {
    val fragmentDefinitionProp = "FRAGMENT_DEFINITION"
    val possibleTypesProp = "POSSIBLE_TYPES"
    val typenameFun = "__typename"
}