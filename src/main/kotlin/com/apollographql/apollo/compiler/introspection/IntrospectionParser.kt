package com.apollographql.apollo.compiler.introspection

import com.apollographql.apollo.compiler.introspection.TypeKind.ENUM
import com.apollographql.apollo.compiler.introspection.TypeKind.INPUT_OBJECT
import com.apollographql.apollo.compiler.introspection.TypeKind.INTERFACE
import com.apollographql.apollo.compiler.introspection.TypeKind.LIST
import com.apollographql.apollo.compiler.introspection.TypeKind.NON_NULL
import com.apollographql.apollo.compiler.introspection.TypeKind.OBJECT
import com.apollographql.apollo.compiler.introspection.TypeKind.SCALAR
import com.apollographql.apollo.compiler.introspection.TypeKind.UNION
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import okio.Buffer
import okio.Okio
import java.io.File
import java.io.InputStream

fun parseIntrospection(reader: JsonReader): IntrospectionSchema {
    val moshi = Moshi.Builder()
            .add(IntrospectionTypeAdapter)
            .add(IntrospectionTypeRefAdapter)
            .build()
    return IntrospectionResultJsonAdapter(moshi).fromJson(reader).data.__schema
}

fun String.parseIntrospectionSchema(): IntrospectionSchema =
        parseIntrospection(JsonReader.of(Buffer().writeUtf8(this)))

fun File.parseIntrospectionSchema(): IntrospectionSchema =
        parseIntrospection(JsonReader.of(Okio.buffer(Okio.source(this))))

fun InputStream.parseIntrospectionSchema(): IntrospectionSchema =
        parseIntrospection(JsonReader.of(Okio.buffer(Okio.source(this))))

@JsonClass(generateAdapter = true)
internal data class IntrospectionResult(val data: IntrospectionData)

@JsonClass(generateAdapter = true)
internal data class IntrospectionData(val __schema: IntrospectionSchema)

internal data class IntrospectionTypeJson(
        val kind: TypeKind,
        val name: String,
        val description: String?,
        val fields: List<IntrospectionField>?,
        val interfaces: List<IntrospectionNamedTypeRef>?,
        val inputFields: List<IntrospectionInputValue>?,
        val possibleTypes: List<IntrospectionNamedTypeRef>?,
        val enumValues: List<IntrospectionEnumValue>?
)

internal object IntrospectionTypeAdapter {
    @FromJson fun fromJson(json: IntrospectionTypeJson): IntrospectionType {
        return when (json.kind) {
            SCALAR -> IntrospectionScalarType(json.name, json.description)
            OBJECT -> IntrospectionObjectType(json.name, json.description, json.fields!!, json.interfaces!!)
            INTERFACE -> IntrospectionInterfaceType(json.name, json.description, json.fields!!, json.possibleTypes!!)
            UNION -> IntrospectionUnionType(json.name, json.description, json.possibleTypes!!)
            ENUM -> IntrospectionEnumType(json.name, json.description, json.enumValues!!)
            INPUT_OBJECT -> IntrospectionInputObjectType(json.name, json.description, json.inputFields!!)
            else -> throw UnsupportedOperationException("Unsupported type: '${json.kind}'")
        }
    }

    @Suppress("unused_parameter")
    @ToJson fun toJson(
            writer: JsonWriter,
            type: IntrospectionType,
            scalarAdapter: JsonAdapter<IntrospectionScalarType>,
            objectAdapter: JsonAdapter<IntrospectionObjectType>,
            interfaceAdapter: JsonAdapter<IntrospectionInterfaceType>,
            unionAdapter: JsonAdapter<IntrospectionUnionType>,
            enumAdapter: JsonAdapter<IntrospectionEnumType>,
            inputObjectAdapter: JsonAdapter<IntrospectionInputObjectType>
    ) {
        when (type) {
            is IntrospectionScalarType -> scalarAdapter.toJson(writer, type)
            is IntrospectionObjectType -> objectAdapter.toJson(writer, type)
            is IntrospectionInterfaceType -> interfaceAdapter.toJson(writer, type)
            is IntrospectionUnionType -> unionAdapter.toJson(writer, type)
            is IntrospectionEnumType -> enumAdapter.toJson(writer, type)
            is IntrospectionInputObjectType -> inputObjectAdapter.toJson(writer, type)
        }
    }
}

internal class IntrospectionTypeRefJson(
        val kind: TypeKind,
        val name: String?,
        val ofType: IntrospectionTypeRef?
)

internal object IntrospectionTypeRefAdapter {
    @FromJson fun fromJson(json: IntrospectionTypeRefJson): IntrospectionTypeRef {
        return when (json.kind) {
            LIST -> IntrospectionListTypeRef(json.ofType!!)
            NON_NULL -> IntrospectionNonNullTypeRef(json.ofType!!)
            else -> IntrospectionNamedTypeRef(json.kind, json.name!!)
        }
    }

    @ToJson fun toJson(
            writer: JsonWriter,
            typeRef: IntrospectionTypeRef,
            namedAdapter: JsonAdapter<IntrospectionNamedTypeRef>,
            listAdapter: JsonAdapter<IntrospectionListTypeRef>,
            nonNullAdapter: JsonAdapter<IntrospectionNonNullTypeRef>
    ) {
        when (typeRef) {
            is IntrospectionNamedTypeRef -> namedAdapter.toJson(writer, typeRef)
            is IntrospectionListTypeRef -> listAdapter.toJson(writer, typeRef)
            is IntrospectionNonNullTypeRef -> nonNullAdapter.toJson(writer, typeRef)
        }
    }
}