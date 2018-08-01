package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.RESPONSE_MAPPER
import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.RESPONSE_MARSHALLER
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

fun SelectionSetSpec.dataClassSpec(name: ClassName): TypeSpec {
    val optionalTypes: List<ClassName?> = fields.map {
        it.type.takeIf { it.isOptional }?.optionalType?.asClassName()
    }
    val hasOptionalTypes = optionalTypes.any { it != null }

    val primaryConstructor = FunSpec.constructorBuilder()
            .apply {
                fields.forEach {
                    addParameter(it.constructorParameter(maybeOptional = true))
                }
                if (hasOptionalTypes) {
                    addModifiers(KModifier.INTERNAL)
                }
            }
            .build()

    val secondaryConstructor = if (hasOptionalTypes) {
        val thisArgs = fields.mapIndexed { i, field ->
            optionalTypes[i]
                    ?.parameterizedBy(field.type.typeName(false))
                    ?.wrapOptionalValue(CodeBlock.of("%L", field.responseName))
                    ?: CodeBlock.of("%L", field.responseName)
        }.toTypedArray()

        FunSpec.constructorBuilder()
                .apply { fields.forEach {
                    addParameter(it.constructorParameter(maybeOptional = false))
                } }
                .callThisConstructor(*thisArgs)
                .build()
    } else null

    return TypeSpec.classBuilder(name)
            .apply {
                if (fields.any { it.doc.isNotEmpty() }) {
                    addKdoc(CodeBlock.builder()
                            .add("%L", fields.mapNotNull { field ->
                                field.doc.takeUnless { it.isEmpty() }
                                        ?.let { CodeBlock.of("@param %L %L", field.name, it) }
                            }.join("\n", suffix = "\n"))
                            .build())
                }
            }
            .addModifiers(KModifier.DATA)
            .primaryConstructor(primaryConstructor)
            .apply { secondaryConstructor?.let { addFunction(it) } }
            .addProperties(fields.map { it.propertySpec() })
            .addProperty(responseMarshallerPropertySpec())
            .addType(TypeSpec.companionObjectBuilder()
                    .addProperty(responseFieldsPropertySpec())
                    .addProperty(responseMapperPropertySpec(name))
                    .build())
            .build()
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
}