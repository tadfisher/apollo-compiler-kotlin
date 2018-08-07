package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.compiler.ast.EnumValue
import com.apollographql.apollo.compiler.ast.ObjectValue
import com.apollographql.apollo.compiler.ast.Value
import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.INPUT_FIELD_LIST_WRITER
import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.RESPONSE_CONDITIONAL_READER
import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.RESPONSE_LIST_READER
import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.RESPONSE_LIST_WRITER
import com.apollographql.apollo.compiler.codegen.kotlin.ClassNames.RESPONSE_OBJECT_READER
import com.apollographql.apollo.compiler.ir.BuiltinType
import com.apollographql.apollo.compiler.ir.BuiltinTypeRef
import com.apollographql.apollo.compiler.ir.CustomTypeRef
import com.apollographql.apollo.compiler.ir.EnumTypeRef
import com.apollographql.apollo.compiler.ir.FragmentTypeRef
import com.apollographql.apollo.compiler.ir.FragmentsWrapperTypeRef
import com.apollographql.apollo.compiler.ir.InlineFragmentTypeRef
import com.apollographql.apollo.compiler.ir.JavaTypeName
import com.apollographql.apollo.compiler.ir.ListTypeRef
import com.apollographql.apollo.compiler.ir.ObjectTypeRef
import com.apollographql.apollo.compiler.ir.OptionalType
import com.apollographql.apollo.compiler.ir.TypeRef
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName

fun JavaTypeName.kotlin(): ClassName {
    val simpleNames = typeName.split(".")
    return ClassName(packageName, simpleNames[0], *simpleNames.drop(1).toTypedArray())
}

fun TypeRef.kotlin(): TypeName {
    return when (this) {
        is BuiltinTypeRef -> when (kind) {
            BuiltinType.INT -> ClassNames.INT
            BuiltinType.FLOAT -> ClassNames.DOUBLE
            BuiltinType.STRING -> ClassNames.STRING
            BuiltinType.BOOLEAN -> ClassNames.BOOLEAN
            BuiltinType.ID -> ClassNames.STRING
        }
        is EnumTypeRef -> spec.javaType.kotlin()
        is ObjectTypeRef -> javaType.kotlin()
        is ListTypeRef -> ClassNames.LIST.parameterizedBy(ofType.kotlin())
        is CustomTypeRef -> javaType.kotlin()
        is FragmentsWrapperTypeRef -> ClassName("", name)
        is FragmentTypeRef -> spec.javaType.kotlin()
        is InlineFragmentTypeRef -> ClassName("", name)
    }.let {
        optional.wrap(it)
    }
}

fun OptionalType.kotlin(): ClassName {
    return when (this) {
        OptionalType.NONNULL -> throw UnsupportedOperationException()
        OptionalType.NULLABLE -> throw UnsupportedOperationException()
        OptionalType.INPUT -> ClassNames.INPUT_OPTIONAL
        OptionalType.APOLLO -> ClassNames.APOLLO_OPTIONAL
        OptionalType.JAVA -> ClassNames.JAVA_OPTIONAL
        OptionalType.GUAVA -> ClassNames.GUAVA_OPTIONAL
    }
}

fun OptionalType.wrap(typeName: TypeName): TypeName {
    return when (this) {
        OptionalType.NONNULL -> typeName.asNonNullable()
        OptionalType.NULLABLE -> typeName.asNullable()
        OptionalType.INPUT,
        OptionalType.APOLLO,
        OptionalType.JAVA,
        OptionalType.GUAVA -> kotlin().parameterizedBy(typeName)
    }
}

fun OptionalType.fromValue(code: CodeBlock): CodeBlock {
    return when (this) {
        OptionalType.NONNULL,
        OptionalType.NULLABLE -> code
        OptionalType.INPUT,
        OptionalType.APOLLO,
        OptionalType.JAVA,
        OptionalType.GUAVA -> CodeBlock.of("%T.fromNullable(%L)", kotlin(), code)
    }
}

fun OptionalType.toValue(code: CodeBlock): CodeBlock {
    return when (this) {
        OptionalType.NONNULL,
        OptionalType.NULLABLE -> code
        OptionalType.INPUT -> CodeBlock.of("%L.value", code)
        OptionalType.APOLLO,
        OptionalType.JAVA,
        OptionalType.GUAVA -> CodeBlock.of("%L.get()", code)
    }
}

fun OptionalType.toReceiver(code: CodeBlock): CodeBlock {
    return when (this) {
        OptionalType.NONNULL -> code
        OptionalType.NULLABLE,
        OptionalType.INPUT -> CodeBlock.of("%L?", toValue(code))
        OptionalType.APOLLO,
        OptionalType.JAVA,
        OptionalType.GUAVA -> CodeBlock.of("%L.takeIf { it.isPresent() }?.get()?", code)
    }
}

fun OptionalType.emptyValue(): CodeBlock {
    return when (this) {
        OptionalType.NONNULL -> CodeBlock.of("")
        OptionalType.NULLABLE -> CodeBlock.of("null")
        OptionalType.INPUT,
        OptionalType.APOLLO,
        OptionalType.GUAVA -> CodeBlock.of("%T.absent()", kotlin())
        OptionalType.JAVA -> CodeBlock.of("%T.empty()", kotlin())
    }
}

fun OptionalType.checkNotNull(code: CodeBlock, propertyName: String): CodeBlock {
    return when (this) {
        OptionalType.NONNULL ->
            CodeBlock.of("%T.checkNotNull(%L, %S)", ClassNames.APOLLO_UTILS, code, "$propertyName == null")
        else -> code
    }
}

fun TypeRef.initializerCode(initialValue: Value): CodeBlock {
    return when (initialValue) {
        is EnumValue -> CodeBlock.of("%T.%L", required().kotlin(), initialValue.valueCode())
        is ObjectValue -> CodeBlock.of("%T(%L)", required().kotlin(), initialValue.valueCode())
        else -> initialValue.valueCode()
    }.let { optional.fromValue(it) }
}

val TypeRef.readMethod get() = when (this) {
    is BuiltinTypeRef -> when (kind) {
        BuiltinType.INT -> "readInt"
        BuiltinType.FLOAT -> "readDouble"
        BuiltinType.STRING -> "readString"
        BuiltinType.BOOLEAN -> "readBoolean"
        BuiltinType.ID -> "readString"
    }
    is EnumTypeRef -> "readString"
    is ObjectTypeRef -> "readObject"
    is ListTypeRef -> "readList"
    is CustomTypeRef -> "readCustomType"
    is FragmentsWrapperTypeRef -> "readConditional"
    is FragmentTypeRef -> "readConditional"
    is InlineFragmentTypeRef -> "readConditional"
}

fun TypeRef.readResponseFieldValueCode(varName: String, propertyName: String) =
    optional.checkNotNull(readValueCode(varName), propertyName)

fun TypeRef.readValueCode(varName: String, readerParam: String = Types.defaultReaderParam): CodeBlock {
    return when (this) {
        is BuiltinTypeRef -> readValueCode(varName, readerParam)
        is EnumTypeRef -> readValueCode(varName, readerParam)
        is ObjectTypeRef -> readValueCode(varName, readerParam)
        is ListTypeRef -> readValueCode(varName, readerParam)
        is CustomTypeRef -> readCustomCode(varName, readerParam)
        is FragmentsWrapperTypeRef -> readFragmentsWrapperCode(varName, readerParam)
        is FragmentTypeRef -> readFragmentCode(readerParam)
        is InlineFragmentTypeRef -> readInlineFragmentCode(varName, readerParam)
    }
}

fun BuiltinTypeRef.readValueCode(varName: String, readerParam: String) =
    CodeBlock.of("%L.%L(%L)", readerParam, readMethod, varName)

fun EnumTypeRef.readValueCode(varName: String, readerParam: String): CodeBlock {
    return CodeBlock.of("""
        %L.%L(%L)?.let {
        %>%T.%L(it)
        %<}
    """.trimIndent(), readerParam, readMethod, varName, required().kotlin(), Types.enumSafeValueOfFun)
}

fun ObjectTypeRef.readValueCode(varName: String, readerParam: String): CodeBlock {
    val typeName = required().kotlin()
    val readerLambda = CodeBlock.of("""
        %T {
        %>%T.%L.map(it)
        %<}
    """.trimIndent(),
            RESPONSE_OBJECT_READER.parameterizedBy(typeName), typeName, Selections.mapperProperty)

    return CodeBlock.of("%L.%L(%L, %L)", readerParam, "readObject", varName, readerLambda)
}

fun ListTypeRef.readValueCode(varName: String, readerParam: String): CodeBlock {
    val typeName = required().kotlin()
    val fieldType = ofType
    val readerLambda = CodeBlock.of("""
        %T { %L ->
        %>%L
        %<}
    """.trimIndent(),
            RESPONSE_LIST_READER.parameterizedBy(typeName), Types.defaultItemReaderParam,
            fieldType.readListItemCode(Types.defaultItemReaderParam))

    return CodeBlock.of("%L.%L(%L, %L)", readerParam, readMethod, varName, readerLambda)
}

fun TypeRef.readListItemCode(itemReaderParam: String): CodeBlock {
    fun readBuiltin() = CodeBlock.of("%L.%L()", itemReaderParam, readMethod)

    fun readEnum() =
        CodeBlock.of("%T.%L(%L.%L())", required().kotlin(), Types.enumSafeValueOfFun, itemReaderParam, readMethod)

    fun readObject(): CodeBlock {
        val typeName = required().kotlin()
        val readerLambda = CodeBlock.of("""
            %T {
            %>%T.%L.map(it)
            %<}
        """.trimIndent(), RESPONSE_OBJECT_READER.parameterizedBy(typeName), typeName, Selections.mapperProperty)

        return CodeBlock.of("%L.%L(%L)", itemReaderParam, readMethod, readerLambda)
    }

    fun ListTypeRef.readList(): CodeBlock {
        val readItemCode = ofType.readListItemCode("it")
        val readerLambda = CodeBlock.of("""
            %T {
            %>%L
            %<}
        """.trimIndent(), RESPONSE_LIST_READER, readItemCode)
        return CodeBlock.of("%L.%L(%L)", itemReaderParam, readMethod, readerLambda)
    }

    fun readCustom() =
        CodeBlock.of("%L.%L(%T)", itemReaderParam, readMethod, ClassName.bestGuess(name))

    return when (this) {
        is BuiltinTypeRef -> readBuiltin()
        is EnumTypeRef -> readEnum()
        is ObjectTypeRef -> readObject()
        is ListTypeRef -> readList()
        is CustomTypeRef -> readCustom()
        is FragmentsWrapperTypeRef,
        is FragmentTypeRef,
        is InlineFragmentTypeRef -> throw UnsupportedOperationException("Lists of fragments are not allowed.")
    }
}

fun CustomTypeRef.readCustomCode(varName: String, readerParam: String): CodeBlock {
    return CodeBlock.of("%L.%L(%L as %T)",
            readerParam, readMethod, varName, ResponseField.CustomTypeField::class)
}

fun FragmentsWrapperTypeRef.readFragmentsWrapperCode(varName: String, readerParam: String): CodeBlock {
    val typeName = kotlin()
    val readerLambda = CodeBlock.of("""
        %T { %L, %L ->
        %>%T.%L.map(%L, %L)
        %<}
    """.trimIndent(),
            RESPONSE_CONDITIONAL_READER.parameterizedBy(typeName),
            Types.conditionalTypeParam, Types.defaultReaderParam,
            typeName, Selections.mapperProperty, Types.defaultReaderParam,
            Types.conditionalTypeParam
    )

    return CodeBlock.of("%L.%L(%L, %L)", readerParam, readMethod, varName, readerLambda)
}

fun FragmentTypeRef.readFragmentCode(readerParam: String): CodeBlock {
    val typeName = required().kotlin()
    return CodeBlock.of("%T.%L.takeIf { %L in %T.%L }?.map(%L)",
        typeName, Selections.mapperProperty,
        Types.conditionalTypeParam, typeName, Fragments.possibleTypesProp,
        readerParam)
}

// TODO this isn't right
fun InlineFragmentTypeRef.readInlineFragmentCode(varName: String, readerParam: String): CodeBlock {
    val concreteType = ClassName.bestGuess(name)

    val readerLambda = CodeBlock.of("""
        %T { %L, %L ->
        %>%T.%L.map(%L)
        %<}
    """.trimIndent(),
            RESPONSE_CONDITIONAL_READER.parameterizedBy(concreteType),
            Types.conditionalTypeParam, Types.defaultReaderParam,
            concreteType, Selections.mapperProperty, Types.defaultReaderParam)

    return CodeBlock.of("%L.%L(%L, %L)", readerParam, readMethod, varName, readerLambda)
}

val TypeRef.writeMethod get() = when (this) {
    is BuiltinTypeRef -> when (kind) {
        BuiltinType.INT -> "writeInt"
        BuiltinType.FLOAT -> "writeDouble"
        BuiltinType.STRING -> "writeString"
        BuiltinType.BOOLEAN -> "writeBoolean"
        BuiltinType.ID -> "writeString"
    }
    is EnumTypeRef -> "writeString"
    is ObjectTypeRef -> "writeObject"
    is ListTypeRef -> "writeList"
    is CustomTypeRef -> "writeCustom"
    is FragmentsWrapperTypeRef,
    is FragmentTypeRef,
    is InlineFragmentTypeRef -> throw UnsupportedOperationException()
}

fun TypeRef.writeInputFieldValueCode(varName: String, propertyName: String = varName): CodeBlock {
    return writeValueCode(varName, propertyName, listWriterType = INPUT_FIELD_LIST_WRITER)
        .let {
        if (optional == OptionalType.INPUT) {
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

fun TypeRef.writeResponseFieldValueCode(varName: String, propertyName: String) =
    writeValueCode(varName, propertyName, listWriterType = RESPONSE_LIST_WRITER)

fun TypeRef.writeValueCode(
    varName: String,
    propertyName: String = varName,
    writerParam: String = Types.defaultWriterParam,
    itemWriterParam: String = Types.defaultItemWriterParam,
    listWriterType: ClassName
): CodeBlock {
    return when (this) {
        is BuiltinTypeRef -> writeValueCode(varName, propertyName, writerParam)
        is EnumTypeRef -> writeValueCode(varName, propertyName, writerParam)
        is ObjectTypeRef -> writeValueCode(varName, propertyName, writerParam)
        is ListTypeRef -> writeValueCode(varName, propertyName, writerParam, itemWriterParam, listWriterType)
        is CustomTypeRef -> writeCustomCode(varName, propertyName, writerParam)
        is FragmentsWrapperTypeRef,
        is FragmentTypeRef,
        is InlineFragmentTypeRef -> writeFragmentCode(propertyName, writerParam)
    }
}

fun BuiltinTypeRef.writeValueCode(varName: String, propertyName: String, writerParam: String): CodeBlock {
    val valueCode = optional.toValue(propertyName.code())
    return CodeBlock.of("%L.%L(%S, %L)", writerParam, writeMethod, varName, valueCode)
}

fun EnumTypeRef.writeValueCode(varName: String, propertyName: String, writerParam: String): CodeBlock {
    return CodeBlock.of("%L.%L(%S, %L.%L)",
        writerParam, writeMethod, varName, optional.toReceiver(propertyName.code()), InputTypes.enumRawValueProp)
}

fun ObjectTypeRef.writeValueCode(varName: String, propertyName: String, writerParam: String): CodeBlock {
    return CodeBlock.of("%L.%L(%S, %L.%L)",
        writerParam, writeMethod, varName, optional.toReceiver(propertyName.code()), Selections.marshallerProperty)
}

fun ListTypeRef.writeValueCode(
    varName: String,
    propertyName: String,
    writerParam: String,
    itemWriterParam: String,
    listWriterType: ClassName
): CodeBlock {
    return CodeBlock.of("%L.%L(%S, %L)", writerParam, writeMethod, varName,
        ofType.listWriter(optional.toReceiver(propertyName.code()), itemWriterParam, listWriterType))
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
    fun writeBuiltin() = CodeBlock.of(" %L.%L(it) ", itemWriterParam, writeMethod)

    fun writeEnum() =
        CodeBlock.of(" %L.%L(%L.rawValue) ", itemWriterParam, writeMethod, optional.toReceiver("it".code()))

    fun writeObject(): CodeBlock {
        return CodeBlock.of(" %L.%L(%L.%L) ",
            itemWriterParam, writeMethod, optional.toReceiver("it".code()), Selections.marshallerProperty)
    }

    fun ListTypeRef.writeList(): CodeBlock {
        val nestedListWriter = ofType.listWriter("it".code(), itemWriterParam, listWriterType)
        val valueCode = if (optional == OptionalType.NONNULL) {
            nestedListWriter
        } else {
            CodeBlock.of("%L.let { %L }", optional.toReceiver("it".code()), nestedListWriter)
        }
        return CodeBlock.of("\n%>%L.%L(%L)\n%<", itemWriterParam, writeMethod, valueCode)
    }

    fun CustomTypeRef.writeCustom() =
        CodeBlock.of(" %L.%L(%T, it) ", itemWriterParam, writeMethod, spec.customTypeName.kotlin())

    return when (this) {
        is BuiltinTypeRef -> writeBuiltin()
        is EnumTypeRef -> writeEnum()
        is ObjectTypeRef -> writeObject()
        is ListTypeRef -> writeList()
        is CustomTypeRef -> writeCustom()
        is FragmentsWrapperTypeRef,
        is FragmentTypeRef,
        is InlineFragmentTypeRef -> throw UnsupportedOperationException("Lists of fragments are not allowed.")
    }
}

fun CustomTypeRef.writeCustomCode(varName: String, propertyName: String, writerParam: String): CodeBlock {
    return CodeBlock.of("%L.%L(%S, %T, %L)",
            writerParam, writeMethod, varName, spec.customTypeName.kotlin(), optional.toValue(propertyName.code()))
}

fun TypeRef.writeFragmentCode(propertyName: String, writerParam: String): CodeBlock {
    return CodeBlock.of(
        "%L.%L.marshal(%L)",
        optional.toReceiver(propertyName.code()), Selections.marshallerProperty, writerParam)
}

object Types {
    const val defaultReaderParam = "_reader"
    const val defaultItemReaderParam = "_itemReader"
    const val defaultWriterParam = "_writer"
    const val defaultItemWriterParam = "_itemWriter"
    const val conditionalTypeParam = "_conditionalType"
    const val enumSafeValueOfFun = "safeValueOf"
}
