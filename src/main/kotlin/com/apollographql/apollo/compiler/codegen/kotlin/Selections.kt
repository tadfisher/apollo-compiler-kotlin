package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.RESPONSE_MAPPER
import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.RESPONSE_MARSHALLER
import com.apollographql.apollo.compiler.ir.ResponseFieldSpec
import com.apollographql.apollo.compiler.ir.SelectionSetSpec
import com.squareup.kotlinpoet.ARRAY
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName

fun SelectionSetSpec.typeSpecs(): List<TypeSpec> {
    return fields.mapNotNull { it.selections?.dataClassSpec(ClassName("", it.typeName)) } +
            fields.flatMap { it.selections?.typeSpecs() ?: emptyList() }
}

fun SelectionSetSpec.dataClassSpec(name: ClassName): TypeSpec {
    fun defaultTypenameConstructor(
            fields: List<ResponseFieldSpec>,
            maybeOptional: Boolean
    ): FunSpec {
        val otherFields = fields.filterNot { it.name == Selections.typenameField }
        return FunSpec.constructorBuilder()
                .addKdoc(otherFields.kdoc())
                .addParameters(otherFields.map { it.constructorParameter(maybeOptional) })
                .callThisConstructor(CodeBlock.of("%L",
                        fields.map {
                            if (it.name == Selections.typenameField) {
                                // TODO determine reasonable default __typename
                                CodeBlock.of("%S", name.simpleName)
                            } else {
                                CodeBlock.of("%L", it.responseName)
                            }
                        }.join()))
                .build()
    }


    val optionalTypes: List<ClassName?> = fields.map {
        it.type.takeIf { it.isOptional }?.optionalType?.asClassName()
    }
    val hasOptionalTypes = optionalTypes.any { it != null }
    val hasTypenameField = fields.any { it.name == Selections.typenameField }

    return with (TypeSpec.classBuilder(name)) {
        addModifiers(KModifier.DATA)
        addProperties(fields.map { it.propertySpec() })
        addProperty(responseMarshallerPropertySpec())
        addType(TypeSpec.companionObjectBuilder()
                .addProperty(responseFieldsPropertySpec())
                .addProperty(responseMapperPropertySpec(name))
                .build())

        if (fields.any { it.doc.isNotEmpty() }) {
            addKdoc(fields.kdoc())
        }

        if (hasOptionalTypes) {
            primaryConstructor(FunSpec.constructorBuilder()
                    .addModifiers(KModifier.INTERNAL)
                    .addParameters(fields.map { it.constructorParameter(maybeOptional = true) })
                    .build())

            addFunction(with(FunSpec.constructorBuilder()) {
                addParameters(fields.map { it.constructorParameter(maybeOptional = false) })

                callThisConstructor(fields.mapIndexed { i, field ->
                    optionalTypes[i]
                            ?.parameterizedBy(field.type.typeName(false))
                            ?.wrapOptionalValue(field.responseName)
                            ?: CodeBlock.of("%L", field.responseName)
                }.join())

                build()
            })
        } else {
            primaryConstructor(with (FunSpec.constructorBuilder()) {
                addParameters(fields.map { it.constructorParameter(maybeOptional = true) })
                build()
            })
        }

        if (hasTypenameField) {
            addFunction(defaultTypenameConstructor(fields, !hasOptionalTypes))
        }

        build()
    }
}

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

fun SelectionSetSpec.responseMarshallerPropertySpec(): PropertySpec {
    val code = fields.mapIndexed { i, field ->
        val varName = "${Selections.responseFieldsProperty}[$i]"
        field.type.writeResponseFieldValueCode(varName, field.responseName)
    }.join("\n")

    return PropertySpec.builder(
            Selections.marshallerProperty, RESPONSE_MARSHALLER, KModifier.INTERNAL)
            .delegate("""
                lazy {
                %>%T { %L ->
                %>%L
                %<}
                %<}
            """.trimIndent(), RESPONSE_MARSHALLER, Types.defaultWriterParam, code)
            .build()
}

object Selections {
    const val responseFieldsProperty = "RESPONSE_FIELDS"
    const val mapperProperty = "MAPPER"
    const val marshallerProperty = "_marshaller"
    const val typenameField = "__typename"
}