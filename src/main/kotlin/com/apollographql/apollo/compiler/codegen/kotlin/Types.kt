package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.compiler.ast.EnumValue
import com.apollographql.apollo.compiler.ast.ObjectValue
import com.apollographql.apollo.compiler.ast.Value
import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.APOLLO_OPTIONAL
import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.APOLLO_UTILS
import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.GUAVA_OPTIONAL
import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.INPUT_FIELD_LIST_WRITER
import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.INPUT_OPTIONAL
import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.JAVA_OPTIONAL
import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.RESPONSE_CONDITIONAL_READER
import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.RESPONSE_LIST_READER
import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.RESPONSE_LIST_WRITER
import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.RESPONSE_OBJECT_READER
import com.apollographql.apollo.compiler.ir.TypeKind
import com.apollographql.apollo.compiler.ir.TypeRef
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName

fun TypeRef.typeName(maybeOptional: Boolean = true): TypeName {
    val rawType = ClassName.bestGuess(jvmName)
    val typeName = if (parameters.isNotEmpty()) {
        rawType.parameterizedBy(
                *(parameters.map { it.typeName() }.toTypedArray())
        )
    } else {
        rawType
    }

    return if (maybeOptional && isOptional) {
        optionalType?.asClassName()?.parameterizedBy(typeName) ?: typeName.asNullable()
    } else {
        typeName
    }
}

fun TypeRef.initializerCode(initialValue: Value): CodeBlock {
    val valueCode = when (initialValue) {
        is EnumValue -> CodeBlock.of("%T.%L", typeName(false), initialValue.valueCode())
        is ObjectValue ->
            CodeBlock.of("%T(%L)", typeName(false), initialValue.valueCode())
        else -> initialValue.valueCode()
    }
    return if (isOptional && optionalType != null) {
        typeName(true).wrapOptionalValue(valueCode)
    } else {
        valueCode
    }
}

fun TypeRef.readResponseFieldValueCode(
        varName: String,
        propertyName: String
): CodeBlock {
    val typeName = typeName(false).let { if (isOptional) it.asNullable() else it }
    return CodeBlock.of("val %L: %T = %L",
            propertyName, typeName, wrapNullCheck(readValueCode(varName), propertyName))
}

fun TypeRef.readValueCode(
        varName: String,
        readerParam: String = Types.defaultReaderParam
): CodeBlock {
    return when (kind) {
        TypeKind.STRING,
        TypeKind.INT,
        TypeKind.LONG,
        TypeKind.DOUBLE,
        TypeKind.BOOLEAN -> readScalarCode(varName, readerParam)
        TypeKind.ENUM -> readEnumCode(varName, readerParam)
        TypeKind.OBJECT -> readObjectCode(varName, readerParam)
        TypeKind.LIST -> readListCode(varName, readerParam)
        TypeKind.CUSTOM -> readCustomCode(varName, readerParam)
        TypeKind.FRAGMENT -> readFragmentsCode(varName, readerParam)
        TypeKind.INLINE_FRAGMENT -> TODO()
    }
}

fun TypeRef.readScalarCode(varName: String, readerParam: String): CodeBlock {
    return CodeBlock.of("%L.%L(%L)", readerParam, kind.readMethod, varName)
}

fun TypeRef.readEnumCode(
        varName: String,
        readerParam: String
): CodeBlock {
    return CodeBlock.of("""
        %L.%L(%L)?.let {
        %>%T.%L(it)
        %<}
    """.trimIndent(), readerParam, TypeKind.STRING.readMethod, varName, typeName(false),
            Types.enumSafeValueOfFun)
}

fun TypeRef.readObjectCode(
        varName: String,
        readerParam: String
): CodeBlock {
    val typeName = typeName(false)
    val readerLambda = CodeBlock.of("""
        %T {
        %>%T.%L.map(it)
        %<}
    """.trimIndent(),
            RESPONSE_OBJECT_READER.parameterizedBy(typeName), typeName, Selections.mapperProperty)

    return CodeBlock.of("%L.%L(%L, %L)", readerParam, kind.readMethod, varName, readerLambda)
}

fun TypeRef.readListCode(
        varName: String,
        readerParam: String
): CodeBlock {
    val typeName = typeName(false)
    val fieldType = parameters[0]
    val readerLambda = CodeBlock.of("""
        %T { %L ->
        %>%L
        %<}
    """.trimIndent(),
            RESPONSE_LIST_READER.parameterizedBy(typeName), Types.defaultItemReaderParam,
            fieldType.readListItemStatement(Types.defaultItemReaderParam))

    return CodeBlock.of("%L.%L(%L, %L)", readerParam, kind.readMethod, varName, readerLambda)
}

fun TypeRef.readListItemStatement(itemReaderParam: String): CodeBlock {
    fun readScalar() = CodeBlock.of("%L.%L()", itemReaderParam, kind.readMethod)

    fun readEnum(): CodeBlock {
        return CodeBlock.of("%T.%L(%L.%L())", typeName(false), Types.enumSafeValueOfFun,
                itemReaderParam, kind.readMethod)
    }

    fun readObject(): CodeBlock {
        val typeName = typeName(false)
        val readerLambda = CodeBlock.of("""
            %T {
            %>%T.%L.map(it)
            %<}
        """.trimIndent(), RESPONSE_OBJECT_READER.parameterizedBy(typeName), typeName,
                Selections.mapperProperty)

        return CodeBlock.of("%L.%L(%L)", itemReaderParam, kind.readMethod, readerLambda)
    }

    fun readList(): CodeBlock {
        val fieldType = parameters[0]
        val readItemCode = fieldType.readListItemStatement("it")
        val readerLambda = CodeBlock.of("""
            %T {
            %>%L
            %<}
        """.trimIndent(), RESPONSE_LIST_READER, readItemCode)
        return CodeBlock.of("%L.%L(%L)", itemReaderParam, kind.readMethod, readerLambda)
    }

    fun readCustom() =
            CodeBlock.of("%L.%L(%T)", itemReaderParam, kind.readMethod, ClassName.bestGuess(name))

    return when (kind) {
        TypeKind.STRING,
        TypeKind.INT,
        TypeKind.LONG,
        TypeKind.DOUBLE,
        TypeKind.BOOLEAN -> readScalar()
        TypeKind.ENUM -> readEnum()
        TypeKind.OBJECT -> readObject()
        TypeKind.LIST -> readList()
        TypeKind.CUSTOM -> readCustom()
        TypeKind.FRAGMENT,
        TypeKind.INLINE_FRAGMENT -> throw UnsupportedOperationException(
                "Lists of fragments are not allowed."
        )
    }
}

fun TypeRef.readCustomCode(varName: String, readerParam: String): CodeBlock {
    return CodeBlock.of("%L.%L(%L as %T)",
            readerParam, kind.readMethod, varName, ResponseField.CustomTypeField::class)
}

fun TypeRef.readFragmentsCode(varName: String, readerParam: String): CodeBlock {
    val fragmentsType = ClassName("", Selections.fragmentsType)

    val readerLambda = CodeBlock.of("""
        %T { %L, %L ->
        %>%T.%L.map(%L, %L)
        %<}
    """.trimIndent(),
            RESPONSE_CONDITIONAL_READER.parameterizedBy(fragmentsType),
            Types.conditionalTypeParam, Types.defaultReaderParam,
            fragmentsType, Selections.mapperProperty, Types.defaultReaderParam,
            Types.conditionalTypeParam
    )

    return CodeBlock.of("%L.%L(%L, %L)",
            readerParam, kind.readMethod, varName, readerLambda)
}

fun TypeRef.writeInputFieldValueCode(
        varName: String,
        propertyName: String = varName
): CodeBlock {
    return writeValueCode(varName, propertyName, listWriterType = INPUT_FIELD_LIST_WRITER).let {
        if (isOptional) {
            CodeBlock.of("""
                if (%L.defined) {
                %>%L
                %<}
            """.trimIndent(), propertyName, it)
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
        listWriterType: ClassName
): CodeBlock {
    return when (kind) {
        TypeKind.STRING,
        TypeKind.INT,
        TypeKind.LONG,
        TypeKind.DOUBLE,
        TypeKind.BOOLEAN -> writeScalarCode(varName, propertyName, writerParam)
        TypeKind.ENUM -> writeEnumCode(varName, propertyName, writerParam)
        TypeKind.OBJECT -> writeObjectCode(varName, propertyName, writerParam)
        TypeKind.LIST ->
            writeListCode(varName, propertyName, writerParam, itemWriterParam, listWriterType)
        TypeKind.CUSTOM -> writeCustomCode(varName, propertyName, writerParam)
        TypeKind.FRAGMENT -> writeFragmentsCode(propertyName, writerParam)
        TypeKind.INLINE_FRAGMENT -> TODO()
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
        writerParam: String
): CodeBlock {
    val typeName = typeName()
    val valueCode = typeName.unwrapOptionalValue(propertyName) {
        CodeBlock.of("%L.%L", it, Selections.marshallerProperty)
    }
    return CodeBlock.of("%L.%L(%S, %L)", writerParam, kind.writeMethod, varName, valueCode)
}

fun TypeRef.writeListCode(
        varName: String,
        propertyName: String,
        writerParam: String,
        itemWriterParam: String,
        listWriterType: ClassName
): CodeBlock {
    val typeName = typeName()
    val unwrappedListValue = typeName.unwrapOptionalValue(propertyName)
    val listParamType = if (kind == TypeKind.LIST) parameters.first() else this
    val listWriterCode = typeName.unwrapOptionalValue(propertyName) {
        CodeBlock.of("%L", listParamType.listWriter(unwrappedListValue, itemWriterParam,
                listWriterType))
    }
    return CodeBlock.of("%L.%L(%S, %L)", writerParam, kind.writeMethod, varName, listWriterCode)
}

fun TypeRef.listWriter(
        listParam: CodeBlock,
        itemWriterParam: String,
        listWriterType: ClassName
): CodeBlock {
    return CodeBlock.of("""
        %T { %L ->
        %>%L.forEach {%L}
        %<}
    """.trimIndent(),
            listWriterType, itemWriterParam, listParam,
            writeListItemCode(itemWriterParam, listWriterType)
    )
}

fun TypeRef.writeListItemCode(
        itemWriterParam: String,
        listWriterType: ClassName
): CodeBlock {
    fun writeList(): CodeBlock {
        val nestedListItemParamType = if (kind == TypeKind.LIST) parameters.first() else this
        val nestedListWriter = nestedListItemParamType.listWriter(CodeBlock.of("%L", "it"),
                itemWriterParam, listWriterType)
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
                itemWriterParam, kind.writeMethod, valueCode, Selections.marshallerProperty)
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
        TypeKind.FRAGMENT,
        TypeKind.INLINE_FRAGMENT -> throw UnsupportedOperationException()
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

fun TypeRef.writeFragmentsCode(
        propertyName: String,
        writerParam: String
): CodeBlock {
    return CodeBlock.of("%L.%L.marshal(%L)",
            propertyName, Selections.marshallerProperty, writerParam)
}

fun TypeName.isOptional(expectedOptionalType: ClassName? = null): Boolean {
    val rawType = (this as? ParameterizedTypeName)?.rawType ?: this
    return if (expectedOptionalType == null) {
        return (rawType in setOf(APOLLO_OPTIONAL, GUAVA_OPTIONAL, JAVA_OPTIONAL, INPUT_OPTIONAL))
    } else {
        rawType == expectedOptionalType
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
                transformation(CodeBlock.of("%L.get()", varName))
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

fun TypeName.wrapOptionalValue(value: String): CodeBlock {
    return wrapOptionalValue(CodeBlock.of("%L", value))
}


fun TypeName.defaultOptionalValue(): CodeBlock {
    return if (isOptional() && this is ParameterizedTypeName) {
        CodeBlock.of("%T.absent", rawType)
    } else if (nullable) {
        CodeBlock.of("%L", "null")
    } else {
        CodeBlock.of("")
    }
}

fun TypeRef.wrapNullCheck(code: CodeBlock, propertyName: String): CodeBlock {
    return if (!isOptional) {
        CodeBlock.of("%T.%L(%L, %S)",
                APOLLO_UTILS, Types.checkNotNullFun, code, "$propertyName == null")
    } else {
        code
    }
}

object Types {
    const val defaultReaderParam = "_reader"
    const val defaultItemReaderParam = "_itemReader"
    const val defaultWriterParam = "_writer"
    const val defaultItemWriterParam = "_itemWriter"
    const val conditionalTypeParam = "_conditionalType"
    const val enumSafeValueOfFun = "safeValueOf"
    const val checkNotNullFun = "checkNotNull"
}
