package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.FRAGMENT_RESPONSE_MAPPER
import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.RESPONSE_MAPPER
import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.RESPONSE_MARSHALLER
import com.apollographql.apollo.compiler.ir.FragmentSpreadSpec
import com.apollographql.apollo.compiler.ir.ResponseFieldSpec
import com.apollographql.apollo.compiler.ir.SelectionSetSpec
import com.squareup.kotlinpoet.ARRAY
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
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
                .addParameterKdoc(otherFields)
                .addParameters(otherFields.map { it.constructorParameterSpec(maybeOptional) })
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

    return with(TypeSpec.classBuilder(name)) {
        addModifiers(KModifier.DATA)
        addProperties(fields.map { it.propertySpec() })
        addProperty(responseMarshallerPropertySpec())
        addType(TypeSpec.companionObjectBuilder()
                .addProperty(responseFieldsPropertySpec())
                .addProperty(responseMapperPropertySpec(name))
                .build())
        addParameterKdoc(fields)

        if (hasOptionalTypes) {
            primaryConstructor(FunSpec.constructorBuilder()
                    .addModifiers(KModifier.INTERNAL)
                    .addParameters(fields.map { it.constructorParameterSpec(maybeOptional = true) })
                    .build())

            addFunction(with(FunSpec.constructorBuilder()) {
                addParameters(fields.map { it.constructorParameterSpec(maybeOptional = false) })

                callThisConstructor(fields.mapIndexed { i, field ->
                    optionalTypes[i]
                            ?.parameterizedBy(field.type.typeName(false))
                            ?.wrapOptionalValue(field.responseName)
                            ?: CodeBlock.of("%L", field.responseName)
                }.join())

                build()
            })
        } else {
            primaryConstructor(with(FunSpec.constructorBuilder()) {
                addParameters(fields.map { it.constructorParameterSpec(maybeOptional = true) })
                build()
            })
        }

        if (hasTypenameField) {
            addFunction(defaultTypenameConstructor(fields, !hasOptionalTypes))
        }

        if (fragmentSpreads.isNotEmpty()) {
            addType(fragmentsTypeSpec())
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
    }.join(",\n")

    val mapperLambda = CodeBlock.of("""
        %T { %L -> %T(
        %>%L
        %<)}
    """.trimIndent(), mapperType, Types.defaultReaderParam, forType, mapperCode)

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
            .addTransientAnnotation(AnnotationSpec.UseSiteTarget.DELEGATE)
            .delegate("""
                lazy {
                %>%T { %L ->
                %>%L
                %<}
                %<}
            """.trimIndent(), RESPONSE_MARSHALLER, Types.defaultWriterParam, code)
            .build()
}

fun SelectionSetSpec.fragmentsTypeSpec(): TypeSpec {
    val className = ClassName("", Selections.fragmentsType)

    val hasOptionalTypes = fragmentSpreads.any { it.optionalType != null }

    val marshallerLambda = CodeBlock.of("""
        %T { %L ->
        %>%L
        %<}
    """.trimIndent(),
            RESPONSE_MARSHALLER, Types.defaultWriterParam,
            fragmentSpreads.joinToCodeBlock("\n") { it.writeValueCode() }
    )

    val mapperLambda = CodeBlock.of("""
        %T { %L, %L -> %T(
        %>%L
        %<)}
    """.trimIndent(),
            FRAGMENT_RESPONSE_MAPPER.parameterizedBy(className),
            Types.defaultReaderParam, Types.conditionalTypeParam,
            className,
            fragmentSpreads.joinToCodeBlock(",\n") { it.readValueCode() }
    )

    return with(TypeSpec.classBuilder(className)) {
        addModifiers(KModifier.DATA)

        if (hasOptionalTypes) {
            primaryConstructor(FunSpec.constructorBuilder()
                    .addModifiers(KModifier.INTERNAL)
                    .addParameters(fragmentSpreads.map { it.constructorParameterSpec(true) })
                    .build())

            addFunction(FunSpec.constructorBuilder()
                    .addParameters(fragmentSpreads.map { it.constructorParameterSpec(false) })
                    .callThisConstructor(fragmentSpreads.joinToCodeBlock {
                        it.optionalType?.asClassName()?.wrapOptionalValue(it.propertyName)
                                ?: CodeBlock.of(it.propertyName)
                    })
                    .build())
        } else {
            primaryConstructor(FunSpec.constructorBuilder()
                    .addParameters(fragmentSpreads.map { it.constructorParameterSpec(true) })
                    .build())
        }

        addProperties(fragmentSpreads.map { it.propertySpec() })

        addProperty(PropertySpec.builder(Selections.marshallerProperty, RESPONSE_MARSHALLER)
                .addTransientAnnotation(AnnotationSpec.UseSiteTarget.DELEGATE)
                .delegate("""
                    lazy {
                    %>%L
                    %<}
                """.trimIndent(), marshallerLambda)
                .build())

        addType(TypeSpec.companionObjectBuilder()
                .addProperty(PropertySpec.builder(
                        Selections.mapperProperty,
                        FRAGMENT_RESPONSE_MAPPER.parameterizedBy(className))
                        .addAnnotation(JvmField::class)
                        .initializer(mapperLambda)
                        .build())
                .build())

        build()
    }
}

fun FragmentSpreadSpec.constructorParameterSpec(maybeOptional: Boolean): ParameterSpec {
    val fragmentType = fragmentTypeName(maybeOptional).let {
        if (isOptional && !maybeOptional) it.asNullable() else it
    }
    return ParameterSpec.builder(propertyName, fragmentType).build()
}

fun FragmentSpreadSpec.propertySpec(): PropertySpec {
    return PropertySpec.builder(propertyName, fragmentTypeName(true))
            .initializer(propertyName)
            .build()
}

fun FragmentSpreadSpec.fragmentTypeName(maybeOptional: Boolean): TypeName {
    return if (maybeOptional && isOptional) {
        optionalType?.asClassName()?.parameterizedBy(fragment.className())
                ?: fragment.className().asNullable()
    } else {
        fragment.className()
    }
}

fun FragmentSpreadSpec.readValueCode(): CodeBlock {
    val fragmentType = fragment.className()

    return CodeBlock.of("%T.%L.takeIf (%L in %T.%L)?.map(%L)",
            fragmentType, Selections.mapperProperty,
            Types.conditionalTypeParam, fragmentType, Fragments.possibleTypesProp,
            Types.defaultReaderParam)
}

fun FragmentSpreadSpec.writeValueCode(): CodeBlock {
    return CodeBlock.of("%L.%L.marshal(%L)",
            fragmentTypeName(true).unwrapOptionalValue(propertyName, true),
            Selections.marshallerProperty, Types.defaultWriterParam)
}

object Selections {
    const val responseFieldsProperty = "RESPONSE_FIELDS"
    const val mapperProperty = "MAPPER"
    const val marshallerProperty = "_marshaller"
    const val typenameField = "__typename"
    const val fragmentsType = "Fragments"
}