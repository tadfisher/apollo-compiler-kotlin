package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.api.InputFieldMarshaller
import com.apollographql.apollo.api.InputFieldWriter
import com.apollographql.apollo.api.Mutation
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.Subscription
import com.apollographql.apollo.compiler.ast.OperationType
import com.apollographql.apollo.compiler.ast.OperationType.MUTATION
import com.apollographql.apollo.compiler.ast.OperationType.QUERY
import com.apollographql.apollo.compiler.ast.OperationType.SUBSCRIPTION
import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.INPUT_OPTIONAL
import com.apollographql.apollo.compiler.ir.OperationSpec
import com.apollographql.apollo.compiler.ir.OperationVariablesSpec
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
import com.apollographql.apollo.compiler.ir.VariableSpec
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName

fun OperationSpec.typeSpec(packageName: String): TypeSpec {
    val className = ClassName(packageName, name.capitalize())
    val dataType = className.nestedClass("Data")
    val variablesType = if (variables != null) {
        className.nestedClass("Variables")
    } else {
        Operation.Variables::class.asClassName()
    }

    return TypeSpec.classBuilder(className)
            .addSuperinterface(operation.typeName(dataType, dataType.asNullable(), variablesType))
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
            .addAnnotation(Transient::class)
            .addModifiers(KModifier.PRIVATE)
            .initializer("""
                    listOfNotNull(
                        %L
                    ).toMap()
                """.trimIndent(), variables.joinToString(",\n") { it.valueMapEntry() }
            )
            .build()

    val valueMapFunSpec = FunSpec.builder("valueMap")
            .addModifiers(KModifier.OVERRIDE)
            .returns(Map::class.parameterizedBy(String::class, Any::class))
            .addCode(valueMapPropertySpec.name)
            .build()

    val marshallerLambdaCode = CodeBlock.of("""
        %T { writer ->
            %L
        }
    """.trimIndent(),
            InputFieldMarshaller::class,
            variables.map { it.type.writeValueCode(it.name) }
                    .fold(CodeBlock.builder(), CodeBlock.Builder::add)
                    .build()
    )

    val marshallerFunSpec = FunSpec.builder("marshaller")
            .addModifiers(KModifier.OVERRIDE)
            .returns(InputFieldMarshaller::class)
            .addStatement("return %L", marshallerLambdaCode)
            .build()

    return TypeSpec.classBuilder(className)
            .addSuperinterface(Operation.Variables::class.asClassName())
            .primaryConstructor(FunSpec.constructorBuilder()
                    .addModifiers(KModifier.INTERNAL)
                    .apply { variables.forEach {
                        addParameter(it.name, it.type.typeName(INPUT_OPTIONAL)) }
                    }
                    .build())
            .apply {
                variables.forEach {
                    addProperty(PropertySpec.builder(it.name, it.type.typeName())
                            .initializer(it.name)
                            .build())
                }
            }
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
                itemWriterParam, kind.writeMethod, typeName().unwrapOptionalType())
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
            writerParam, kind.writeMethod, varName, typeName.unwrapOptionalType(), valueCode)
}

fun VariableSpec.valueMapEntry(): String {
    return if (type.isOptional) {
        "(\"$name\" to $name).takeIf { $name.defined }"
    } else {
        "\"$name\" to $name"
    }
}

private const val writerParam = "_writer"
private const val itemWriterParam = "_itemWriter"
private const val marshallerParam = "marshaller()"
