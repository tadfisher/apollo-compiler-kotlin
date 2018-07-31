package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.ResponseFieldMapper
import com.apollographql.apollo.api.ResponseFieldMarshaller
import com.apollographql.apollo.api.ResponseReader
import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.RESPONSE_MAPPER
import com.apollographql.apollo.compiler.ir.SelectionSetSpec
import com.squareup.kotlinpoet.ARRAY
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName

fun SelectionSetSpec.responseFieldsPropertySpec(): PropertySpec {
    val initializerCode = fields.map { it.factoryCode() }
            .join(",\n", prefix = "arrayOf(\n%>", suffix = "\n%<)")

    val type = ARRAY.parameterizedBy(ResponseField::class.asClassName())

    return PropertySpec.builder(
            Selections.responseFieldsProperty, type, KModifier.INTERNAL)
            .addAnnotation(JvmField::class)
            .initializer(initializerCode)
            .build()
}

fun SelectionSetSpec.responseMapperPropertySpec(forType: TypeName): PropertySpec {
    val mapperType = RESPONSE_MAPPER.parameterizedBy(forType)

    val mapperCode = fields.mapIndexed { i, field ->
        val varName = "${Selections.responseFieldsProperty}[$i]"
        field.type.readResponseFieldValueCode(varName, field.responseName)
    }.join("\n")

    val builderCode = CodeBlock.of("%T(%L)", forType, fields.joinToString { it.responseName })

    val mapperLambda = CodeBlock.of("""
        %T { %L ->
        %>%L
        %L
        %<}
    """.trimIndent(), mapperType, Types.defaultReaderParam, mapperCode, builderCode)

    return PropertySpec.builder(Selections.mapperProperty, mapperType)
            .addAnnotation(JvmField::class)
            .initializer(mapperLambda)
            .build()
}

fun SelectionSetSpec.responseMarshallerFunSpec(): FunSpec {
    val code = fields.mapIndexed { i, field ->
        val varName = "${Selections.responseFieldsProperty}[$i]"
        field.type.writeResponseFieldValueCode(varName, field.responseName)
    }.join("\n")

    return FunSpec.builder("marshaller")
            .addModifiers(KModifier.OVERRIDE)
            .addCode("""
                return %T { %L ->
                %>%L
                %<}

            """.trimIndent(),
                    ResponseFieldMarshaller::class, Types.defaultWriterParam, code)
            .build()
}

object Selections {
    const val responseFieldsProperty = "RESPONSE_FIELDS"
    const val mapperProperty = "MAPPER"
}