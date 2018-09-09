// package com.apollographql.apollo.compiler.codegen.kotlin
//
// import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.RESPONSE_MAPPER
// import com.apollographql.apollo.compiler.ir.InlineFragmentsWrapperSpec
// import com.squareup.kotlinpoet.CodeBlock
// import com.squareup.kotlinpoet.KModifier
// import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
// import com.squareup.kotlinpoet.PropertySpec
// import com.squareup.kotlinpoet.TypeName
//
// fun InlineFragmentsWrapperSpec.mapperPropertySpec(forType: TypeName): PropertySpec {
//    val type = RESPONSE_MAPPER.parameterizedBy(forType)
//
//    val statements = inlineFragments.mapIndexed { i, field ->
//        val varName = "${Selections.responseFieldsProperty}[$i]"
//        field.type.readInlineFragmentCode(varName, Types.defaultReaderParam)
//    } + CodeBlock.of("%L.%L.map(%L)", surrogateType, Selections.mapperProperty, Types.defaultReaderParam)
//
//    return PropertySpec.builder(Selections.mapperProperty, type, KModifier.INTERNAL)
//        .addAnnotation(JvmField::class)
//        .initializer("""
//            %T { %L ->
//            %>%L
//            %<}
//        """.trimIndent(), type, Types.defaultReaderParam, statements.join("\n?: "))
//        .build()
// }
