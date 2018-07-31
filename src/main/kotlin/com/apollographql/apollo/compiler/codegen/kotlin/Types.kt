package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.compiler.ast.EnumValue
import com.apollographql.apollo.compiler.ast.ObjectValue
import com.apollographql.apollo.compiler.ast.Value
import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.APOLLO_OPTIONAL
import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.GUAVA_OPTIONAL
import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.INPUT_FIELD_LIST_WRITER
import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.INPUT_OPTIONAL
import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.JAVA_OPTIONAL
import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.RESPONSE_LIST_WRITER
import com.apollographql.apollo.compiler.ir.TypeKind
import com.apollographql.apollo.compiler.ir.TypeRef
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName

fun TypeRef.typeName(): TypeName {
    val rawType = ClassName.bestGuess(jvmName)
    val typeName = if (parameters.isNotEmpty()) {
        rawType.parameterizedBy(
                *(parameters.map { it.typeName() }.toTypedArray())
        )
    } else {
        rawType
    }

    return if (isOptional) {
        optionalType?.asClassName()?.parameterizedBy(typeName) ?: typeName.asNullable()
    } else {
        typeName
    }
}

fun TypeRef.initializerCode(initialValue: Value): CodeBlock {
    val valueCode = when (initialValue) {
        is EnumValue -> CodeBlock.of("%T.%L", typeName().asNonNullable(), initialValue.valueCode())
        is ObjectValue ->
            CodeBlock.of("%[%T(%L)%]", typeName().asNonNullable(), initialValue.valueCode())
        else -> initialValue.valueCode()
    }
    return if (isOptional && optionalType != null) {
        optionalType.asClassName().wrapOptionalValue(valueCode)
    } else {
        valueCode
    }
}

fun TypeRef.writeInputFieldValueCode(varName: String): CodeBlock {
    return writeValueCode(varName = varName, listWriterType = INPUT_FIELD_LIST_WRITER
    ).let {
        if (isOptional) {
            CodeBlock.of("""
                if (%L.defined) {
                %>%L
                %<}
            """.trimIndent(), varName, it)
        } else {
            it
        }
    }
}

fun TypeRef.writeResponseFieldValueCode(
        varName: String,
        propertyName: String
): CodeBlock {
    return writeValueCode(
            varName = varName,
            propertyName = propertyName,
            listWriterType = RESPONSE_LIST_WRITER
    )
}

fun TypeRef.writeValueCode(
        varName: String,
        propertyName: String = varName,
        writerParam: String = Types.defaultWriterParam,
        itemWriterParam: String = Types.defaultItemWriterParam,
        marshallerParam: String = Types.defaultMarshallerParam,
        listWriterType: ClassName
): CodeBlock {
    return when (kind) {
        TypeKind.STRING,
        TypeKind.INT,
        TypeKind.LONG,
        TypeKind.DOUBLE,
        TypeKind.BOOLEAN -> writeScalarCode(varName, propertyName, writerParam)
        TypeKind.ENUM -> writeEnumCode(varName, propertyName, writerParam)
        TypeKind.OBJECT -> writeObjectCode(varName, propertyName, writerParam, marshallerParam)
        TypeKind.LIST ->
            writeListCode(varName, propertyName, writerParam, itemWriterParam, marshallerParam, listWriterType)
        TypeKind.CUSTOM -> writeCustomCode(varName, propertyName, writerParam)
    }}

fun TypeRef.writeScalarCode(
        varName: String,
        propertyName: String,
        writerParam: String
): CodeBlock {
    val typeName = typeName()
    val valueCode = typeName.unwrapOptionalValue(propertyName, false)
    return CodeBlock.of("%L.%L(%S, %L)", writerParam, kind.writeMethod, varName, valueCode)
}

fun TypeRef.writeEnumCode(
        varName: String,
        propertyName: String,
        writerParam: String
): CodeBlock {
    val typeName = typeName()
    val valueCode = typeName.unwrapOptionalValue(propertyName) {
        CodeBlock.of("%L.rawValue()", it)
    }
    return CodeBlock.of("%L.%L(%S, %L)", writerParam, kind.writeMethod, varName, valueCode)
}

fun TypeRef.writeObjectCode(
        varName: String,
        propertyName: String,
        writerParam: String,
        marshallerParam: String
): CodeBlock {
    val typeName = typeName()
    val valueCode = typeName.unwrapOptionalValue(propertyName) {
        CodeBlock.of("%L.%L", it, marshallerParam)
    }
    return CodeBlock.of("%L.%L(%S, %L)", writerParam, kind.writeMethod, varName, valueCode)
}

fun TypeRef.writeListCode(
        varName: String,
        propertyName: String,
        writerParam: String,
        itemWriterParam: String,
        marshallerParam: String,
        listWriterType: ClassName
): CodeBlock {
    val typeName = typeName()
    val unwrappedListValue = typeName.unwrapOptionalValue(propertyName)
    val listParamType = if (kind == TypeKind.LIST) parameters.first() else this
    val listWriterCode = typeName.unwrapOptionalValue(propertyName) {
        CodeBlock.of("%L", listParamType.listWriter(unwrappedListValue, itemWriterParam,
                marshallerParam, listWriterType))
    }
    return CodeBlock.of("%L.%L(%S, %L)", writerParam, kind.writeMethod, varName, listWriterCode)
}

fun TypeRef.listWriter(
        listParam: CodeBlock,
        itemWriterParam: String,
        marshallerParam: String,
        listWriterType: ClassName
): CodeBlock {
    return CodeBlock.of("""
        %T { %L ->
        %>%L.forEach {%L}
        %<}
    """.trimIndent(),
            listWriterType, itemWriterParam, listParam,
            writeListItemCode(itemWriterParam, marshallerParam, listWriterType)
    )
}

fun TypeRef.writeListItemCode(
        itemWriterParam: String,
        marshallerParam: String,
        listWriterType: ClassName
): CodeBlock {
    fun writeList(): CodeBlock {
        val nestedListItemParamType = if (kind == TypeKind.LIST) parameters.first() else this
        val nestedListWriter = nestedListItemParamType.listWriter(CodeBlock.of("%L", "it"),
                itemWriterParam, marshallerParam, listWriterType)
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
        TypeKind.STRING,
        TypeKind.INT,
        TypeKind.LONG,
        TypeKind.DOUBLE,
        TypeKind.BOOLEAN -> writeScalar()
        TypeKind.ENUM -> writeEnum()
        TypeKind.OBJECT -> writeObject()
        TypeKind.LIST -> writeList()
        TypeKind.CUSTOM -> writeCustom()
    }
}

fun TypeRef.writeCustomCode(
        varName: String,
        propertyName: String,
        writerParam: String
): CodeBlock {
    val typeName = typeName()
    val valueCode = typeName.unwrapOptionalValue(propertyName, false)
    return CodeBlock.of("%L.%L(%S, %T, %L)",
            writerParam, kind.writeMethod, varName, ClassName.bestGuess(name), valueCode)
}

fun TypeName.isOptional(expectedOptionalType: ClassName? = null): Boolean {
    val rawType = (this as? ParameterizedTypeName)?.rawType ?: this
    return if (expectedOptionalType == null) {
        return (rawType in setOf(APOLLO_OPTIONAL, GUAVA_OPTIONAL, JAVA_OPTIONAL, INPUT_OPTIONAL))
    } else {
        rawType == expectedOptionalType
    }
}

fun TypeName.unwrapOptionalType(): TypeName {
    return if (this is ParameterizedTypeName && isOptional()) {
        typeArguments.first().asNonNullable()
    } else {
        this.asNonNullable()
    }
}

fun TypeName.unwrapOptionalValue(
        varName: String,
        checkIfPresent : Boolean = true,
        transformation: ((CodeBlock) -> CodeBlock) = { it }
): CodeBlock {
    return if (isOptional() && this is ParameterizedTypeName) {
        if (rawType == INPUT_OPTIONAL) {
            val valueCode = if (checkIfPresent) {
                CodeBlock.of("%L.value?", varName)
            } else {
                CodeBlock.of("%L.value", varName)
            }
            transformation(valueCode)
        } else {
            if (checkIfPresent) {
                transformation(CodeBlock.of("%L.takeIf { it.isPresent() }?.get()", varName))
            } else {
                transformation(CodeBlock.of("%L.get()"))
            }
        }
    } else {
        val valueCode = CodeBlock.of("%L", varName)
        if (nullable && checkIfPresent) {
            transformation(CodeBlock.of("%L?", valueCode))
        } else {
            transformation(valueCode)
        }
    }
}

fun TypeName.wrapOptionalValue(value: CodeBlock): CodeBlock {
    return if (isOptional() && this is ParameterizedTypeName) {
        CodeBlock.of("%T.fromNullable(%L)", rawType, value)
    } else {
        value
    }
}

fun TypeName.defaultOptionalValue(): CodeBlock {
    return if (isOptional() && this is ParameterizedTypeName) {
        CodeBlock.of("%T.absent", rawType)
    } else {
        CodeBlock.of("")
    }
}

object Types {
    const val defaultWriterParam = "_writer"
    const val defaultItemWriterParam = "_itemWriter"
    const val defaultMarshallerParam = "marshaller()"
}
