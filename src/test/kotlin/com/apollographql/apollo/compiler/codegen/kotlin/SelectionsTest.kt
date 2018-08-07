package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.compiler.ast.EnumValue
import com.apollographql.apollo.compiler.ast.IntValue
import com.apollographql.apollo.compiler.ast.ObjectField
import com.apollographql.apollo.compiler.ast.ObjectValue
import com.apollographql.apollo.compiler.ast.VariableValue
import com.apollographql.apollo.compiler.ir.ArgumentSpec
import com.apollographql.apollo.compiler.ir.FragmentSpec
import com.apollographql.apollo.compiler.ir.FragmentTypeRef
import com.apollographql.apollo.compiler.ir.FragmentsWrapperSpec
import com.apollographql.apollo.compiler.ir.JavaTypeName
import com.apollographql.apollo.compiler.ir.OptionalType
import com.apollographql.apollo.compiler.ir.ResponseFieldSpec
import com.apollographql.apollo.compiler.ir.SelectionSetSpec
import com.google.common.truth.Truth.assertThat
import com.squareup.kotlinpoet.ClassName
import org.junit.Test
import java.math.BigInteger

class SelectionsTest {

    @Test
    fun `emits data class`() {
        val spec = SelectionSetSpec(
            fields = listOf(
                ResponseFieldSpec("number", doc = "A number.", type = floatRef),
                ResponseFieldSpec("string", doc = "A string.", type = stringRef.required())
            )
        )

        assertThat(spec.dataClassSpec(ClassName("", "Data")).code()).isEqualTo("""
            /**
             * @param number A number.
             * @param string A string.
             */
            data class Data(val number: Double?, val string: String) {
                @delegate:Transient
                internal val _marshaller: ResponseFieldMarshaller by lazy {
                            ResponseFieldMarshaller { _writer ->
                                _writer.writeDouble("RESPONSE_FIELDS[0]", number)
                                _writer.writeString("RESPONSE_FIELDS[1]", string)
                            }
                        }

                companion object {
                    @JvmField
                    internal val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                                ResponseField.forDouble("number", "number", null, true, emptyList()),
                                ResponseField.forString("string", "string", null, false, emptyList())
                            )

                    @JvmField
                    val MAPPER: ResponseFieldMapper<Data> = ResponseFieldMapper<Data> { _reader -> Data(
                                _reader.readDouble(RESPONSE_FIELDS[0]),
                                Utils.checkNotNull(_reader.readString(RESPONSE_FIELDS[1]), "string == null")
                            )}
                }
            }
        """.trimIndent())
    }

    @Test
    fun `emits data class with optionals`() {
        val spec = SelectionSetSpec(
            fields = listOf(
                ResponseFieldSpec("number", type = floatRef.required()),
                ResponseFieldSpec("optionalNumber", type = floatRef.optional(OptionalType.APOLLO)),
                ResponseFieldSpec("string", type = stringRef.required())
            )
        )

        assertThat(spec.dataClassSpec(ClassName("", "Data")).code()).isEqualTo("""
            data class Data internal constructor(
                val number: Double,
                val optionalNumber: Optional<Double>,
                val string: String
            ) {
                @delegate:Transient
                internal val _marshaller: ResponseFieldMarshaller by lazy {
                            ResponseFieldMarshaller { _writer ->
                                _writer.writeDouble("RESPONSE_FIELDS[0]", number)
                                _writer.writeDouble("RESPONSE_FIELDS[1]", optionalNumber.get())
                                _writer.writeString("RESPONSE_FIELDS[2]", string)
                            }
                        }

                constructor(
                    number: Double,
                    optionalNumber: Double?,
                    string: String
                ) : this(number, Optional.fromNullable(optionalNumber), string)

                companion object {
                    @JvmField
                    internal val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                                ResponseField.forDouble("number", "number", null, false, emptyList()),
                                ResponseField.forDouble("optionalNumber", "optionalNumber", null, true, emptyList()),
                                ResponseField.forString("string", "string", null, false, emptyList())
                            )

                    @JvmField
                    val MAPPER: ResponseFieldMapper<Data> = ResponseFieldMapper<Data> { _reader -> Data(
                                Utils.checkNotNull(_reader.readDouble(RESPONSE_FIELDS[0]), "number == null"),
                                _reader.readDouble(RESPONSE_FIELDS[1]),
                                Utils.checkNotNull(_reader.readString(RESPONSE_FIELDS[2]), "string == null")
                            )}
                }
            }
        """.trimIndent())
    }

    @Test
    fun `emits response fields`() {
        val spec = SelectionSetSpec(
            fields = listOf(
                ResponseFieldSpec(
                    name = "heroWithReview",
                    type = heroRef,
                    arguments = listOf(
                        ArgumentSpec(
                            name = "episode",
                            value = VariableValue("episode"),
                            type = episodeRef
                        ),
                        ArgumentSpec(
                            name = "review",
                            value = ObjectValue(listOf(
                                ObjectField("stars", VariableValue("stars")),
                                ObjectField("favoriteColor", ObjectValue(listOf(
                                    ObjectField("red", IntValue(BigInteger.valueOf(0))),
                                    ObjectField("green", VariableValue("greenValue")),
                                    ObjectField("blue", IntValue(BigInteger.valueOf(0)))
                                )))
                            )),
                            type = reviewRef
                        )
                    ),
                    selections = SelectionSetSpec(
                        fields = listOf(
                            ResponseFieldSpec(
                                name = "name",
                                type = stringRef
                            ),
                            ResponseFieldSpec(
                                name = "height",
                                type = floatRef,
                                arguments = listOf(
                                    ArgumentSpec(
                                        name = "unit",
                                        value = EnumValue("FOOT"),
                                        type = unitRef
                                    ))))))))

        assertThat(spec.responseFieldsPropertySpec().code()).isEqualTo("""
            @JvmField
            internal val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                        ResponseField.forObject("heroWithReview", "heroWithReview", mapOf(
                            "episode" to mapOf(
                                "kind" to "Variable",
                                "variableName" to "episode"
                            ),
                            "review" to mapOf(
                                "stars" to mapOf(
                                    "kind" to "Variable",
                                    "variableName" to "stars"
                                ),
                                "favoriteColor" to mapOf(
                                    "red" to 0,
                                    "green" to mapOf(
                                        "kind" to "Variable",
                                        "variableName" to "greenValue"
                                    ),
                                    "blue" to 0
                                )
                            )
                        ), true, emptyList())
                    )
            """.trimIndent())
    }

    @Test
    fun `emits response mapper`() {
        val spec = SelectionSetSpec(
            fields = listOf(
                ResponseFieldSpec("number", type = floatRef),
                ResponseFieldSpec("string", type = stringRef.required()),
                ResponseFieldSpec("unit", type = unitRef),
                ResponseFieldSpec("heroWithReview", type = heroRef),
                ResponseFieldSpec("list", type = listRef.of(stringRef)),
                ResponseFieldSpec("custom", type = customUrlRef),
                ResponseFieldSpec("fragments", type = heroDetailsWrapperRef)
            )
        )

        assertThat(spec.responseMapperPropertySpec(ClassName("com.example", "Data")).code("com.example"))
            .isEqualTo("""
                @JvmField
                val MAPPER: ResponseFieldMapper<Data> = ResponseFieldMapper<Data> { _reader -> Data(
                            _reader.readDouble(RESPONSE_FIELDS[0]),
                            Utils.checkNotNull(_reader.readString(RESPONSE_FIELDS[1]), "string == null"),
                            _reader.readString(RESPONSE_FIELDS[2])?.let {
                                Unit.safeValueOf(it)
                            },
                            _reader.readObject(RESPONSE_FIELDS[3], ResponseReader.ObjectReader<TestQuery.Hero> {
                                TestQuery.Hero.MAPPER.map(it)
                            }),
                            _reader.readList(RESPONSE_FIELDS[4], ResponseReader.ListReader<List<String?>> { _itemReader ->
                                _itemReader.readString()
                            }),
                            _reader.readCustomType(RESPONSE_FIELDS[5] as ResponseField.CustomTypeField),
                            Utils.checkNotNull(_reader.readConditional(RESPONSE_FIELDS[6], ResponseReader.ConditionalTypeReader<Fragments> { _conditionalType, _reader ->
                                Fragments.MAPPER.map(_reader, _conditionalType)
                            }), "fragments == null")
                        )}
        """.trimIndent())
    }

    @Test
    fun `emits response marshaller`() {
        val spec = SelectionSetSpec(
            fields = listOf(
                ResponseFieldSpec("number", type = floatRef),
                ResponseFieldSpec("unit", type = unitRef),
                ResponseFieldSpec("heroWithReview", type = heroRef),
                ResponseFieldSpec("list", type = listRef.of(stringRef)),
                ResponseFieldSpec("custom", type = customUrlRef),
                ResponseFieldSpec("fragments", type = heroDetailsWrapperRef)
            )
        )

        assertThat(spec.responseMarshallerPropertySpec().code()).isEqualTo("""
            @delegate:Transient
            internal val _marshaller: ResponseFieldMarshaller by lazy {
                        ResponseFieldMarshaller { _writer ->
                            _writer.writeDouble("RESPONSE_FIELDS[0]", number)
                            _writer.writeString("RESPONSE_FIELDS[1]", unit?.rawValue)
                            _writer.writeObject("RESPONSE_FIELDS[2]", heroWithReview?._marshaller)
                            _writer.writeList("RESPONSE_FIELDS[3]", ResponseWriter.ListWriter { _itemWriter ->
                                list?.forEach { _itemWriter.writeString(it) }
                            })
                            _writer.writeCustom("RESPONSE_FIELDS[4]", CustomType.URL, custom)
                            fragments._marshaller.marshal(_writer)
                        }
                    }
        """.trimIndent())
    }

    @Test
    fun `emits fragments type`() {
        val humanFragmentSpec = FragmentSpec(
            name = "HumanDetails",
            javaType = JavaTypeName("com.example.fragments", "HumanDetails"),
            source = """
                    fragment HumanDetails on Human {
                      name
                      height
                    }
                """.trimIndent(),
            typeCondition = "Human",
            selections = SelectionSetSpec(fields = listOf(
                ResponseFieldSpec("name", type = stringRef),
                ResponseFieldSpec("height", type = floatRef)
            ))
        )

        val droidFragmentSpec = FragmentSpec(
            name = "DroidDetails",
            javaType = JavaTypeName("com.example.fragments", "DroidDetails"),
            source = """
                    fragment DroidDetails on Droid {
                      name
                      primaryFunction
                    }
                """.trimIndent(),
            typeCondition = "Droid",
            selections = SelectionSetSpec(fields = listOf(
                ResponseFieldSpec("name", type = stringRef),
                ResponseFieldSpec("primaryFunction", type = stringRef)
            ))
        )

        val spec = FragmentsWrapperSpec(listOf(
            ResponseFieldSpec("humanDetails", type = FragmentTypeRef(humanFragmentSpec)),
            ResponseFieldSpec("droidDetails", type = FragmentTypeRef(droidFragmentSpec))
        ))

        assertThat(spec.typeSpec().code("com.example.fragments")).isEqualTo("""
            data class Fragments(val humanDetails: HumanDetails?, val droidDetails: DroidDetails?) {
                @delegate:Transient
                val _marshaller: ResponseFieldMarshaller by lazy {
                            ResponseFieldMarshaller { _writer ->
                                humanDetails?._marshaller.marshal(_writer)
                                droidDetails?._marshaller.marshal(_writer)
                            }
                        }

                companion object {
                    @JvmField
                    val MAPPER: FragmentResponseFieldMapper<Fragments> =
                            FragmentResponseFieldMapper<Fragments> { _reader, _conditionalType -> Fragments(
                                HumanDetails.MAPPER.takeIf { _conditionalType in HumanDetails.POSSIBLE_TYPES }?.map(_reader),
                                DroidDetails.MAPPER.takeIf { _conditionalType in DroidDetails.POSSIBLE_TYPES }?.map(_reader)
                            )}
                }
            }
        """.trimIndent())
    }
}