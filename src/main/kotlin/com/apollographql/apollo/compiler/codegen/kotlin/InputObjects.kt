package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.INPUT_FIELD_MARSHALLER
import com.apollographql.apollo.compiler.ir.InputObjectTypeSpec
import com.apollographql.apollo.compiler.ir.InputValueSpec
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

fun InputObjectTypeSpec.typeSpec(className: ClassName): TypeSpec {
    return TypeSpec.classBuilder(className)
            .addKdoc(values.kdoc())
            .addModifiers(KModifier.DATA)
            .primaryConstructor(FunSpec.constructorBuilder()
                    .addParameters(values.map { it.constructorParameterSpec() })
                    .build())
            .addProperties(values.map { it.propertySpec() })
            .addProperty(PropertySpec.builder(Selections.marshallerProperty, INPUT_FIELD_MARSHALLER)
                    .addModifiers(KModifier.INTERNAL)
                    .addAnnotation(AnnotationSpec.builder(Transient::class)
                            .useSiteTarget(AnnotationSpec.UseSiteTarget.DELEGATE)
                            .build())
                    .delegate(CodeBlock.of("""
                        lazy {
                        %>%T { %L ->
                        %>%L
                        %<}
                        %<}
                    """.trimIndent(), INPUT_FIELD_MARSHALLER, Types.defaultWriterParam,
                            values.map {
                                it.type.writeInputFieldValueCode(it.name, it.propertyName)
                            }.join("\n")))
                    .build())
            .build()
}

fun InputValueSpec.constructorParameterSpec(): ParameterSpec {
    val typeName = type.typeName()
    return with (ParameterSpec.builder(propertyName, typeName)) {
        // TODO support default values for custom types
        if (defaultValue != null && !type.isCustom) {
            defaultValue(type.initializerCode(defaultValue))
        } else if (type.isOptional) {
            defaultValue(typeName.defaultOptionalValue())
        }
        build()
    }
}

fun InputValueSpec.propertySpec(): PropertySpec {
    return PropertySpec.builder(propertyName, type.typeName())
            .initializer(propertyName)
            .build()
}

fun List<InputValueSpec>.kdoc(): CodeBlock {
    return CodeBlock.builder()
            .add("%L", mapNotNull { value ->
                value.doc.takeUnless { it.isEmpty() }
                        ?.let { CodeBlock.of("@param %L %L", value.name, it) }
            }.join("\n", suffix = "\n"))
            .build()
}