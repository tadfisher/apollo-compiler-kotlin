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
import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.INPUT_OPTIONAL
import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.OPERATION_DATA
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

fun OperationSpec.typeSpec(packageName: String): TypeSpec {
    val className = ClassName(packageName, name.capitalize())
    val dataType = className.nestedClass("Data")
    val variablesType = if (variables != null) {
        className.nestedClass("Variables")
    } else {
        Operation.Variables::class.asClassName()
    }

    return TypeSpec.classBuilder(className)
            .addAnnotation(AnnotationSpec.builder(Generated::class)
                    .addMember("%S", "Apollo GraphQL")
                    .build())
            .addSuperinterface(operation.typeName(dataType, dataType.asNullable(), variablesType))
            .primaryConstructor(FunSpec.constructorBuilder()
                    .addParameters(variables?.variables?.map {
                        it.operationParameterSpec()
                    } ?: emptyList())
                    .build())
            .apply { if (variables != null) addType(variables.typeSpec(variablesType)) }
            .build()
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
            .addCode("return %L", valueMapPropertySpec.name)
            .build()

    val marshallerLambdaCode = CodeBlock.of("%T { %L ->\n%>%L%<}",
            InputFieldMarshaller::class, Types.defaultWriterParam,
            CodeBlock.builder().add(
                variables.map { it.type.writeInputFieldValueCode(it.name) }
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
                    .addCode("return %L", Selections.marshallerProperty)
                    .build())
            .build()
}
