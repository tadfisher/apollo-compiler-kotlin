package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.ResponseReader
import com.apollographql.apollo.api.ResponseWriter
import com.apollographql.apollo.compiler.codegen.ResponseFields
import com.apollographql.apollo.compiler.ir.BooleanTypeRef
import com.apollographql.apollo.compiler.ir.CustomScalarTypeRef
import com.apollographql.apollo.compiler.ir.Field
import com.apollographql.apollo.compiler.ir.FloatTypeRef
import com.apollographql.apollo.compiler.ir.FragmentRef
import com.apollographql.apollo.compiler.ir.IdTypeRef
import com.apollographql.apollo.compiler.ir.IntTypeRef
import com.apollographql.apollo.compiler.ir.ObjectRef
import com.apollographql.apollo.compiler.ir.StringTypeRef
import com.apollographql.apollo.compiler.ir.VariableRef
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.asTypeName
import java.util.Locale

class ResponseFieldSpec(val field: Field) {
    private val responseFieldType = field.type.responseFieldType

    fun factoryCode(): CodeBlock {
        val factoryMethod = ResponseFields.FACTORY_METHODS[responseFieldType]!!
        return when (field.type) {
            is CustomScalarTypeRef -> customTypeFactoryCode(factoryMethod)
            is FragmentRef -> fragmentFactoryCode(factoryMethod, field.type)
            IdTypeRef -> TODO()
            StringTypeRef -> TODO()
            IntTypeRef -> TODO()
            BooleanTypeRef -> TODO()
            FloatTypeRef -> TODO()
            is ObjectRef -> TODO()
            is VariableRef -> TODO()
        }
    }

}

private fun ResponseFieldSpec.customTypeFactoryCode(factoryMethod: String): CodeBlock {
    val customScalarEnum = CustomTypes.className("TODO") // TODO
    val customScalarEnumConst = field.type.name.toUpperCase(Locale.ENGLISH)
    return CodeBlock.of(
            "%T.%L(%S, %S, %L, %L, %T.%L, %L)",
            ResponseField::class,
            factoryMethod,
            field.alias,
            field.name,
            field.argumentCode(),
            field.isOptional,
            customScalarEnum,
            customScalarEnumConst,
            field.conditionsCode()
    )
}

private fun ResponseFieldSpec.fragmentFactoryCode(
        factoryMethod: String,
        typeRef: FragmentRef
): CodeBlock {
    val conditions = CodeBlock.of("listOf(%L)", typeRef.typeConditions.joinToString { it.name })
    return CodeBlock.of("%T.%L(%S, %S, %L)",
            ResponseField::class,
            factoryMethod,
            field.alias,
            field.name,
            conditions
    )
}

private fun Field.argumentCode(): CodeBlock {
    TODO()
}

private fun Field.conditionsCode(): CodeBlock {
    TODO()
}


private val SCALAR_LIST_ITEM_READ_METHODS = mapOf(
        String::class.asTypeName() to "readString",
        INT to "readInt",
        LONG to "readLong",
        DOUBLE to "readDouble",
        BOOLEAN to "readBoolean"
)
private val SCALAR_LIST_ITEM_WRITE_METHODS = mapOf(
        String::class.asTypeName() to "writeString",
        INT to "writeInt",
        LONG to "writeLong",
        DOUBLE to "writeDouble",
        BOOLEAN to "writeBoolean"
)
private val RESPONSE_READER_PARAM =
        ParameterSpec.builder("reader", ResponseReader::class).build()
private val RESPONSE_LIST_ITEM_READER_PARAM =
        ParameterSpec.builder("listItemReader", ResponseReader.ListItemReader::class).build()
private val OBJECT_VALUE_PARAM = ParameterSpec.builder("value", ANY).build()
private val RESPONSE_LIST_ITEM_WRITER_PARAM =
        ParameterSpec.builder("listItemWriter", ResponseWriter.ListItemWriter::class).build()

private val FRAGMENTS_CLASS = ClassName("", "Fragments")
private const val CONDITIONAL_TYPE_VAR = "conditionalType"
