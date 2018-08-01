package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.api.InputFieldMarshaller
import com.apollographql.apollo.api.Mutation
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.Subscription
import com.apollographql.apollo.compiler.ast.OperationType
import com.apollographql.apollo.compiler.ast.OperationType.MUTATION
import com.apollographql.apollo.compiler.ast.OperationType.QUERY
import com.apollographql.apollo.compiler.ast.OperationType.SUBSCRIPTION
import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.OPERATION_DATA
import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.OPERATION_NAME
import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.OPERATION_VARIABLES
import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.RESPONSE_MAPPER
import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.STRING
import com.apollographql.apollo.compiler.ir.OperationDataSpec
import com.apollographql.apollo.compiler.ir.OperationSpec
import com.apollographql.apollo.compiler.ir.OperationVariablesSpec
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import javax.annotation.Generated

fun OperationSpec.typeSpec(): TypeSpec {
    val className = ClassName("", name)
    val dataType = className.nestedClass("Data")
    val optionalDataType = optionalType?.asClassName()?.parameterizedBy(dataType) ?: dataType
    val variablesType = if (variables != null) {
        className.nestedClass("Variables")
    } else {
        OPERATION_VARIABLES
    }

    return with (TypeSpec.classBuilder(className)) {
        addAnnotation(AnnotationSpec.builder(Generated::class)
                .addMember("%S", "Apollo GraphQL")
                .build())

        addSuperinterface(operation.typeName(dataType, optionalDataType, variablesType))

        addType(TypeSpec.companionObjectBuilder()
                .addProperty(
                        PropertySpec.builder(Operations.definitionProperty, STRING, KModifier.CONST)
                                .initializer("%S", definition)
                                .build())
                .addProperty(PropertySpec.builder(Operations.idProperty, STRING, KModifier.CONST)
                        .initializer("%S", id)
                        .build())
                .addProperty(
                        PropertySpec.builder(
                                Operations.queryDocumentProperty, STRING, KModifier.CONST)
                                .initializer("%L", Operations.definitionProperty)
                                .build())
                .addProperty(
                        PropertySpec.builder(Operations.operationNameProperty, OPERATION_NAME)
                                .initializer(CodeBlock.of("%T { %S }", OPERATION_NAME, name))
                                .build()
                )
                .build())

        if (variables != null) {
            primaryConstructor(FunSpec.constructorBuilder()
                    .addModifiers(KModifier.INTERNAL)
                    .addParameter(Operations.variablesProperty, variablesType)
                    .build())

            addProperty(PropertySpec.builder(Operations.variablesProperty, variablesType)
                    .addModifiers(KModifier.PRIVATE)
                    .initializer(Operations.variablesProperty)
                    .build())

            addFunction(FunSpec.constructorBuilder()
                    .addParameters(variables.variables.map { it.operationParameterSpec() })
                    .callThisConstructor(CodeBlock.of("%T(%L)", variablesType,
                            variables.variables.joinToString(",%W") { it.name }))
                    .build())

            addType(variables.typeSpec(variablesType))
        }

        addFunction(FunSpec.builder(Operations.nameFun)
                .addModifiers(KModifier.OVERRIDE)
                .addCode("return %L\n", Operations.operationNameProperty)
                .build())

        addFunction(FunSpec.builder(Operations.operationIdFun)
                .addModifiers(KModifier.OVERRIDE)
                .addCode("return %L\n", Operations.idProperty)
                .build())

        addFunction(FunSpec.builder(Operations.queryDocumentFun)
                .addModifiers(KModifier.OVERRIDE)
                .addCode("return %L\n", Operations.queryDocumentProperty)
                .build())

        addFunction(FunSpec.builder(Operations.wrapDataFun)
                .addModifiers(KModifier.OVERRIDE)
                .addParameter("data", dataType)
                .returns(optionalDataType)
                .addCode("return %L\n", optionalDataType.wrapOptionalValue("data"))
                .build())

        addFunction(FunSpec.builder(Operations.variablesFun)
                .addModifiers(KModifier.OVERRIDE)
                .returns(variablesType)
                .addCode("return %L\n", if (variables != null) {
                    Operations.variablesProperty
                } else {
                    CodeBlock.of("%T.%L", Operations::class, "EMPTY_VARIABLES")
                })
                .build())

        addFunction(FunSpec.builder(Operations.responseFieldMapperFun)
                .addModifiers(KModifier.OVERRIDE)
                .returns(RESPONSE_MAPPER.parameterizedBy(dataType))
                .addCode("return %T.%L\n", dataType, Selections.mapperProperty)
                .build())

        addType(data.typeSpec())

        addTypes(data.selections.typeSpecs())

        build()
    }
}

fun OperationType.typeName(
        data: TypeName,
        wrapped: TypeName,
        variables: TypeName
): TypeName = when (this) {
    QUERY -> Query::class.asClassName()
    MUTATION -> Mutation::class.asClassName()
    SUBSCRIPTION -> Subscription::class.asClassName()
}.parameterizedBy(data, wrapped, variables)

fun OperationVariablesSpec.typeSpec(className: ClassName): TypeSpec {
    val valueMapPropertySpec = PropertySpec.builder("valueMap",
            Map::class.asClassName().parameterizedBy(String::class.asClassName(), ANY))
            .addAnnotation(AnnotationSpec.builder(Transient::class)
                    .useSiteTarget(AnnotationSpec.UseSiteTarget.DELEGATE)
                    .build())
            .addModifiers(KModifier.PRIVATE)
            // TODO This generates too much indentation; see https://github.com/square/kotlinpoet/issues/259
            .delegate("""
                lazy {
                %>listOfNotNull(
                %>%L
                %<).toMap()
                %<}
            """.trimIndent(),
                    variables.map { it.valueMapEntryCode() }.join(",\n"))
            .build()

    val valueMapFunSpec = FunSpec.builder("valueMap")
            .addModifiers(KModifier.OVERRIDE)
            .addCode("return %L\n", valueMapPropertySpec.name)
            .build()

    val marshallerLambdaCode = CodeBlock.of("%T { %L ->\n%>%L%<}",
            InputFieldMarshaller::class, Types.defaultWriterParam,
            CodeBlock.builder().add(
                variables.map { it.type.writeInputFieldValueCode(it.name, it.propertyName) }
                        .join("\n", suffix = "\n")
            ).build())

    val marshallerFunSpec = FunSpec.builder("marshaller")
            .addModifiers(KModifier.OVERRIDE)
            .addStatement("return %L", marshallerLambdaCode)
            .build()

    return TypeSpec.classBuilder(className)
            .addModifiers(KModifier.DATA)
            .primaryConstructor(FunSpec.constructorBuilder()
                    .addParameters(variables.map { it.variablesParameterSpec() })
                    .build())
            .addSuperinterface(Operation.Variables::class.asClassName())
            .addProperties(variables.map { it.propertySpec() })
            .addProperty(valueMapPropertySpec)
            .addFunction(valueMapFunSpec)
            .addFunction(marshallerFunSpec)
            .build()
}

fun OperationDataSpec.typeSpec(): TypeSpec {
    return selections.dataClassSpec(ClassName("", "Data"))
            .toBuilder()
            .addSuperinterface(OPERATION_DATA)
            .addFunction(FunSpec.builder("marshaller")
                    .addModifiers(KModifier.OVERRIDE)
                    .addCode("return %L\n", Selections.marshallerProperty)
                    .build())
            .build()
}

object Operations {
    const val definitionProperty = "OPERATION_DEFINITION"
    const val idProperty = "OPERATION_ID"
    const val queryDocumentProperty = "QUERY_DOCUMENT"
    const val operationNameProperty = "OPERATION_NAME"
    const val variablesProperty = "variables"
    const val operationIdFun = "operationId"
    const val queryDocumentFun = "queryDocument"
    const val wrapDataFun = "wrapData"
    const val variablesFun = "variables"
    const val responseFieldMapperFun = "responseFieldMapper"
    const val nameFun = "name"
}