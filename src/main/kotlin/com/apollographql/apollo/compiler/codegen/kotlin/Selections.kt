package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.ResponseFieldMarshaller
import com.apollographql.apollo.compiler.ir.SelectionSetSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec

fun SelectionSetSpec.responseFieldsPropertySpec(): PropertySpec {
    val initializerCode = fields.map { it.factoryCode() }
            .join(",\n", prefix = "arrayOf(\n%>", suffix = "\n%<)")

    return PropertySpec.builder("_responseFields", Array<ResponseField>::class, KModifier.INTERNAL)
            .addAnnotation(JvmField::class)
            .initializer(initializerCode)
            .build()
}

fun SelectionSetSpec.responseMarshallerFunSpec(
        writerParam: String = Types.defaultWriterParam
): FunSpec {
    val code = fields.mapIndexed { i, field ->
        field.type.writeResponseFieldValueCode("responseFields[$i]", field.responseName)
    }.join("\n")

    return FunSpec.builder("marshaller")
            .addModifiers(KModifier.OVERRIDE)
            .addCode("""
                return %T { %L ->
                %>%L
                %<}

            """.trimIndent(),
                    ResponseFieldMarshaller::class, writerParam, code)
            .build()
}
