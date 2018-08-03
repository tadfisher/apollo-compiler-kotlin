package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.api.ScalarType
import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.CLASS
import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.INPUT_FIELD_MARSHALLER
import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.STRING
import com.apollographql.apollo.compiler.ir.CustomTypesSpec
import com.apollographql.apollo.compiler.ir.EnumTypeSpec
import com.apollographql.apollo.compiler.ir.EnumTypeSpec.Companion.unknownValue
import com.apollographql.apollo.compiler.ir.EnumValueSpec
import com.apollographql.apollo.compiler.ir.InputObjectTypeSpec
import com.apollographql.apollo.compiler.ir.InputValueSpec
import com.apollographql.apollo.compiler.ir.ScalarTypeSpec
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.WildcardTypeName

fun InputObjectTypeSpec.typeSpec(className: ClassName): TypeSpec {
    val marshallerPropertySpec =
            PropertySpec.builder(Selections.marshallerProperty, INPUT_FIELD_MARSHALLER)
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
                    .build()

    return TypeSpec.classBuilder(className)
            .addGeneratedAnnotation()
            .addParameterKdoc(values)
            .addModifiers(KModifier.DATA)
            .primaryConstructor(FunSpec.constructorBuilder()
                    .addParameters(values.map { it.constructorParameterSpec() })
                    .build())
            .addProperties(values.map { it.propertySpec() })
            .addProperty(marshallerPropertySpec)
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

fun EnumTypeSpec.typeSpec(className: ClassName): TypeSpec {
    val safeValueOf = FunSpec.builder(InputTypes.enumSafeValueOfFun)
            .addParameter(InputTypes.enumRawValueProp, STRING)
            .addCode(CodeBlock.of("return %T.values().find { it.%L == %L } ?: %T.%L\n",
                    className, InputTypes.enumRawValueProp, InputTypes.enumRawValueProp,
                    className, unknownValue.propertyName))
            .build()

    return with(TypeSpec.enumBuilder(className)) {
        if (doc.isNotEmpty()) {
            addKdoc("%L\n", doc)
        }

        addGeneratedAnnotation()

        for (value in values) {
            addEnumConstant(value.propertyName, value.enumConstantSpec())
        }

        addEnumConstant(unknownValue.propertyName, unknownValue.enumConstantSpec())

        primaryConstructor(FunSpec.constructorBuilder()
                .addParameter(InputTypes.enumRawValueProp, STRING)
                .build())

        addProperty(PropertySpec.builder(InputTypes.enumRawValueProp, STRING)
                .initializer(InputTypes.enumRawValueProp)
                .build())

        addType(TypeSpec.companionObjectBuilder()
                .addFunction(safeValueOf)
                .build())

        build()
    }
}

fun EnumValueSpec.enumConstantSpec(): TypeSpec {
    return with (TypeSpec.anonymousClassBuilder()) {
        if (doc.isNotEmpty()) addKdoc("%L\n", doc)

        if (deprecationReason != null) {
            addDeprecatedAnnotation(deprecationReason)
        }

        addSuperclassConstructorParameter("%S", name)

        build()
    }
}

fun CustomTypesSpec.typeSpec(): TypeSpec {
    return with (TypeSpec.enumBuilder(InputTypes.customTypesEnum)) {
        addGeneratedAnnotation()

        addSuperinterface(ScalarType::class)

        for (type in types) {
            addEnumConstant(type.name, type.enumConstantSpec())
        }

        build()
    }
}

fun ScalarTypeSpec.enumConstantSpec(): TypeSpec {
    return TypeSpec.anonymousClassBuilder()
            .addFunction(FunSpec.builder("typeName")
                    .addModifiers(KModifier.OVERRIDE)
                    .returns(STRING)
                    .addCode("return %S\n", type.name)
                    .build())
            .addFunction(FunSpec.builder("javaType")
                    .addModifiers(KModifier.OVERRIDE)
                    .returns(CLASS.parameterizedBy(WildcardTypeName.STAR))
                    .addCode("return %T::class.java\n", type.typeName(false))
                    .build())
            .build()
}

object InputTypes {
    const val enumRawValueProp = "rawValue"
    const val enumSafeValueOfFun = "safeValueOf"
    const val customTypesEnum = "CustomType"
}