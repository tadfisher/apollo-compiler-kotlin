package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.api.InputFieldMarshaller
import com.apollographql.apollo.api.InputFieldWriter
import com.apollographql.apollo.api.Mutation
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.Subscription
import com.apollographql.apollo.compiler.ast.OperationType
import com.apollographql.apollo.compiler.ast.OperationType.MUTATION
import com.apollographql.apollo.compiler.ast.OperationType.QUERY
import com.apollographql.apollo.compiler.ast.OperationType.SUBSCRIPTION
import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.INPUT_OPTIONAL
import com.apollographql.apollo.compiler.ir.OperationDataSpec
import com.apollographql.apollo.compiler.ir.OperationSpec
import com.apollographql.apollo.compiler.ir.OperationVariablesSpec
import com.apollographql.apollo.compiler.ir.SelectionSetSpec
import com.apollographql.apollo.compiler.ir.TypeKind.BOOLEAN
import com.apollographql.apollo.compiler.ir.TypeKind.CUSTOM
import com.apollographql.apollo.compiler.ir.TypeKind.DOUBLE
import com.apollographql.apollo.compiler.ir.TypeKind.ENUM
import com.apollographql.apollo.compiler.ir.TypeKind.INT
import com.apollographql.apollo.compiler.ir.TypeKind.LIST
import com.apollographql.apollo.compiler.ir.TypeKind.LONG
import com.apollographql.apollo.compiler.ir.TypeKind.OBJECT
import com.apollographql.apollo.compiler.ir.TypeKind.STRING
import com.apollographql.apollo.compiler.ir.TypeRef
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
import javafx.beans.property.Property
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
                        it.operationParameterSpec(INPUT_OPTIONAL)
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
                    CodeBlock.builder().apply {
                        variables.dropLast(1).forEach { add("%L,\n", it.valueMapEntryCode()) }
                        variables.lastOrNull()?.let { add("%L", it.valueMapEntryCode()) }
                    }.build())
            .build()

    val valueMapFunSpec = FunSpec.builder("valueMap")
            .addModifiers(KModifier.OVERRIDE)
            .addCode("return %L", valueMapPropertySpec.name)
            .build()

    val marshallerLambdaCode = CodeBlock.of("%T { %L ->\n%>%L%<}",
            InputFieldMarshaller::class, writerParam,
            CodeBlock.builder().apply {
                variables.forEach { add(it.type.writeValueCode(it.name)) }
            }.build())

    val marshallerFunSpec = FunSpec.builder("marshaller")
            .addModifiers(KModifier.OVERRIDE)
            .addStatement("return %L", marshallerLambdaCode)
            .build()

    return TypeSpec.classBuilder(className)
            .addModifiers(KModifier.DATA)
            .primaryConstructor(FunSpec.constructorBuilder()
                    .addParameters(variables.map { it.variablesParameterSpec(INPUT_OPTIONAL) })
                    .build())
            .addSuperinterface(Operation.Variables::class.asClassName())
            .addProperties(variables.map { it.propertySpec(INPUT_OPTIONAL) })
            .addProperty(valueMapPropertySpec)
            .addFunction(valueMapFunSpec)
            .addFunction(marshallerFunSpec)
            .build()
}

fun TypeRef.writeValueCode(varName: String): CodeBlock {
    return when (kind) {
        STRING,
        INT,
        LONG,
        DOUBLE,
        BOOLEAN -> writeScalarCode(varName)
        ENUM -> writeEnumCode(varName)
        OBJECT -> writeObjectCode(varName)
        LIST -> writeListCode(varName)
        CUSTOM -> writeCustomCode(varName)
    }.let {
        if (isOptional) {
            CodeBlock.builder()
                    .beginControlFlow("if (%L.defined)", varName)
                    .add(it)
                    .endControlFlow()
                    .build()
        } else {
            it
        }
    }
}

fun TypeRef.writeScalarCode(varName: String): CodeBlock {
    val typeName = typeName(ClassNames.INPUT_OPTIONAL)
    val valueCode = typeName.unwrapOptionalValue(varName, false)
    return CodeBlock.of("%L.%L(%S, %L)\n", writerParam, kind.writeMethod, varName, valueCode)
}

fun TypeRef.writeEnumCode(varName: String): CodeBlock {
    val typeName = typeName(ClassNames.INPUT_OPTIONAL)
    val valueCode = typeName.unwrapOptionalValue(varName) {
        CodeBlock.of("%L.rawValue()", it)
    }
    return CodeBlock.of("%L.%L(%S, %L)\n", writerParam, kind.writeMethod, varName, valueCode)
}

fun TypeRef.writeObjectCode(varName: String): CodeBlock {
    val typeName = typeName(ClassNames.INPUT_OPTIONAL)
    val valueCode = typeName.unwrapOptionalValue(varName) {
        CodeBlock.of("%L.%L", it, marshallerParam)
    }
    return CodeBlock.of("%L.%L(%S, %L)\n", writerParam, kind.writeMethod, varName, valueCode)
}

fun TypeRef.writeListCode(varName: String): CodeBlock {
    val typeName = typeName(ClassNames.INPUT_OPTIONAL)
    val unwrappedListValue = typeName.unwrapOptionalValue(varName)
    val listParamType = if (kind == LIST) parameters.first() else this
    val listWriterCode = typeName.unwrapOptionalValue(name) {
        CodeBlock.of("%L", listParamType.listWriter(unwrappedListValue))
    }
    return CodeBlock.of("%L.%L(%S, %L)\n", writerParam, kind.writeMethod, varName, listWriterCode)
}

fun TypeRef.listWriter(listParam: CodeBlock): CodeBlock {
    return CodeBlock.of("""
        %T { %L ->
        %>%L.forEach {%L}
        %<}
    """.trimIndent(),
            InputFieldWriter.ListWriter::class, itemWriterParam, listParam, writeListItemCode())
}

fun TypeRef.writeListItemCode(): CodeBlock {
    fun writeList(): CodeBlock {
        val nestedListItemParamType = if (kind == LIST) parameters.first() else this
        val nestedListWriter = nestedListItemParamType.listWriter(CodeBlock.of("%L", "it"))
        return if (isOptional) {
            CodeBlock.of("\n%>%L.%L(%L?.let { %L })\n%<", itemWriterParam, kind.writeMethod, "it",
                    nestedListWriter)
        } else {
            CodeBlock.of("\n%>%L.%L(%L)\n%<", itemWriterParam, kind.writeMethod, nestedListWriter)
        }
    }

    fun writeCustom(): CodeBlock {
        return CodeBlock.of(" %L.%L(%T, it) ",
                itemWriterParam, kind.writeMethod, ClassName.bestGuess(name))
    }

    fun writeEnum(): CodeBlock {
        val valueCode = if (isOptional) "it?" else "it"
        return CodeBlock.of(" %L.%L(%L.rawValue()) ", itemWriterParam, kind.writeMethod, valueCode)
    }

    fun writeScalar() = CodeBlock.of(" %L.%L(it) ", itemWriterParam, kind.writeMethod)

    fun writeObject(): CodeBlock {
        val valueCode = if (isOptional) "it?" else "it"
        return CodeBlock.of(" %L.%L(%L.%L) ",
                itemWriterParam, kind.writeMethod, valueCode, marshallerParam)
    }

    return when (kind) {
        STRING,
        INT,
        LONG,
        DOUBLE,
        BOOLEAN -> writeScalar()
        ENUM -> writeEnum()
        OBJECT -> writeObject()
        LIST -> writeList()
        CUSTOM -> writeCustom()
    }
}

fun TypeRef.writeCustomCode(varName: String): CodeBlock {
    val typeName = typeName(ClassNames.INPUT_OPTIONAL)
    val valueCode = typeName.unwrapOptionalValue(varName, false)
    return CodeBlock.of("%L.%L(%S, %T, %L)\n",
            writerParam, kind.writeMethod, varName, ClassName.bestGuess(name), valueCode)
}

fun OperationDataSpec.typeSpec(): TypeSpec {
    return TypeSpec.classBuilder("Data")
            .addModifiers(KModifier.DATA)
            .addSuperinterface(Operation.Data::class.asClassName())
            .addType(TypeSpec.companionObjectBuilder()
                    .addProperty(selections.responseFieldsPropertySpec())
                    .build())
            .build()
}

private const val writerParam = "_writer"
private const val itemWriterParam = "_itemWriter"
private const val marshallerParam = "marshaller()"
