package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.compiler.ast.EnumValue
import com.apollographql.apollo.compiler.ast.OperationType
import com.apollographql.apollo.compiler.ast.VariableValue
import com.apollographql.apollo.compiler.ir.JavaTypeName
import com.apollographql.apollo.compiler.ir.OperationDataSpec
import com.apollographql.apollo.compiler.ir.OperationSpec
import com.apollographql.apollo.compiler.ir.OperationVariablesSpec
import com.apollographql.apollo.compiler.ir.OptionalType
import com.apollographql.apollo.compiler.ir.ResponseFieldSpec
import com.apollographql.apollo.compiler.ir.SelectionSetSpec
import com.apollographql.apollo.compiler.ir.VariableSpec
import com.google.common.truth.Truth.assertThat
import com.squareup.kotlinpoet.ClassName
import org.junit.Test

class OperationsTest {

    @Test
    fun `emits query operation type`() {
        val spec = OperationSpec(
            id = "1234",
            name = "TestQuery",
            javaType = JavaTypeName("com.example", "TestQuery"),
            operation = OperationType.QUERY,
            source = """
                    query TestQuery {
                        hero {
                          __typename
                          id
                          name
                        }
                    }
                """.trimIndent(),
            data = OperationDataSpec(selections = SelectionSetSpec(fields = listOf(
                ResponseFieldSpec(
                    name = "hero",
                    doc = "A hero.",
                    type = heroRef,
                    selections = SelectionSetSpec(fields = listOf(
                        ResponseFieldSpec(
                            name = "id",
                            doc = "ID of the hero.",
                            type = idRef
                        ),
                        ResponseFieldSpec(
                            name = "name",
                            doc = "Hero name.",
                            type = stringRef
                        )
                    )).withTypename()
                )))),
            variables = null)

        assertThat(spec.typeSpec().code("com.example")).isEqualTo("""
            @Generated("Apollo GraphQL")
            class TestQuery : Query<TestQuery.Data, TestQuery.Data, Operation.Variables> {
                override fun name(): OperationName = OPERATION_NAME

                override fun operationId(): String = OPERATION_ID

                override fun queryDocument(): String = QUERY_DOCUMENT

                override fun wrapData(data: Data): Data = data

                override fun variables(): Operation.Variables = Operations.EMPTY_VARIABLES

                override fun responseFieldMapper(): ResponseFieldMapper<Data> = Data.MAPPER

                companion object {
                    const val OPERATION_DEFINITION: String = ""${'"'}
                            |query TestQuery {
                            |    hero {
                            |      __typename
                            |      id
                            |      name
                            |    }
                            |}
                            ""${'"'}.trimMargin()

                    const val OPERATION_ID: String = "1234"

                    const val QUERY_DOCUMENT: String = OPERATION_DEFINITION

                    val OPERATION_NAME: OperationName = OperationName { "TestQuery" }
                }

                /**
                 * @param hero A hero.
                 */
                data class Data(val hero: Hero?) : Operation.Data {
                    @delegate:Transient
                    internal val _marshaller: ResponseFieldMarshaller by lazy {
                                ResponseFieldMarshaller { _writer ->
                                    _writer.writeObject("RESPONSE_FIELDS[0]", hero?._marshaller)
                                }
                            }

                    override fun marshaller() = _marshaller

                    companion object {
                        @JvmField
                        internal val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                                    ResponseField.forObject("hero", "hero", null, true, emptyList())
                                )

                        @JvmField
                        val MAPPER: ResponseFieldMapper<Data> = ResponseFieldMapper<Data> { _reader -> Data(
                                    _reader.readObject(RESPONSE_FIELDS[0], ResponseReader.ObjectReader<Hero> {
                                        Hero.MAPPER.map(it)
                                    })
                                )}
                    }
                }

                /**
                 * @param id ID of the hero.
                 * @param name Hero name.
                 */
                data class Hero(
                    val __typename: String,
                    val id: String,
                    val name: String?
                ) {
                    @delegate:Transient
                    internal val _marshaller: ResponseFieldMarshaller by lazy {
                                ResponseFieldMarshaller { _writer ->
                                    _writer.writeString("RESPONSE_FIELDS[0]", __typename)
                                    _writer.writeString("RESPONSE_FIELDS[1]", id)
                                    _writer.writeString("RESPONSE_FIELDS[2]", name)
                                }
                            }

                    companion object {
                        @JvmField
                        internal val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                                    ResponseField.forString("__typename", "__typename", null, false, emptyList()),
                                    ResponseField.forString("id", "id", null, false, emptyList()),
                                    ResponseField.forString("name", "name", null, true, emptyList())
                                )

                        @JvmField
                        val MAPPER: ResponseFieldMapper<Hero> = ResponseFieldMapper<Hero> { _reader -> Hero(
                                    Utils.checkNotNull(_reader.readString(RESPONSE_FIELDS[0]), "__typename == null"),
                                    Utils.checkNotNull(_reader.readString(RESPONSE_FIELDS[1]), "id == null"),
                                    _reader.readString(RESPONSE_FIELDS[2])
                                )}
                    }
                }
            }
        """.trimIndent())
    }

    @Test
    fun `emits query operation type with variables`() {
        val spec = OperationSpec(
            id = "1234",
            name = "TestQuery",
            javaType = JavaTypeName("com.example", "TestQuery"),
            operation = OperationType.QUERY,
            source = """
                    query TestQuery(${'$'}epsiode: Episode, ${'$'}IncludeName: Boolean!
                        hero(episode: ${'$'}episode) {
                          __typename
                          name @include(if: ${'$'}IncludeName)
                        }
                    }
                """.trimIndent(),
            data = OperationDataSpec(selections = SelectionSetSpec(fields = listOf(
                ResponseFieldSpec(
                    name = "hero",
                    doc = "A hero.",
                    type = heroRef,
                    selections = SelectionSetSpec(fields = listOf(
                        ResponseFieldSpec(
                            name = "name",
                            doc = "Hero name.",
                            type = stringRef,
                            includeIf = listOf(VariableValue("IncludeName"))
                        )
                    )).withTypename()
                )))),
            variables = OperationVariablesSpec(variables = listOf(
                VariableSpec(
                    name = "episode",
                    type = episodeRef.optional(OptionalType.INPUT)
                ),
                VariableSpec(
                    name = "IncludeName",
                    type = booleanRef.required()
                )
            )))

        assertThat(spec.typeSpec().code("com.example")).isEqualTo("""
            @Generated("Apollo GraphQL")
            class TestQuery internal constructor(private val variables: Variables) : Query<TestQuery.Data, TestQuery.Data, TestQuery.Variables> {
                constructor(episode: Input<Episode>, includeName: Boolean) : this(Variables(episode,%WIncludeName))

                override fun name(): OperationName = OPERATION_NAME

                override fun operationId(): String = OPERATION_ID

                override fun queryDocument(): String = QUERY_DOCUMENT

                override fun wrapData(data: Data): Data = data

                override fun variables(): Variables = variables

                override fun responseFieldMapper(): ResponseFieldMapper<Data> = Data.MAPPER

                companion object {
                    const val OPERATION_DEFINITION: String = ""${'"'}
                            |query TestQuery(${'$'}epsiode: Episode, ${'$'}IncludeName: Boolean!
                            |    hero(episode: ${'$'}episode) {
                            |      __typename
                            |      name @include(if: ${'$'}IncludeName)
                            |    }
                            |}
                            ""${'"'}.trimMargin()

                    const val OPERATION_ID: String = "1234"

                    const val QUERY_DOCUMENT: String = OPERATION_DEFINITION

                    val OPERATION_NAME: OperationName = OperationName { "TestQuery" }
                }

                data class Variables(val episode: Input<Episode>, val includeName: Boolean) : Operation.Variables {
                    @delegate:Transient
                    private val valueMap: Map<String, Any> by lazy {
                                listOfNotNull(
                                    ("episode" to episode).takeIf { episode.defined },
                                    "IncludeName" to includeName
                                ).toMap()
                            }

                    override fun valueMap() = valueMap

                    override fun marshaller() = InputFieldMarshaller { _writer ->
                        if (episode.defined) {
                            _writer.writeString("episode", episode.value?.rawValue)
                        }
                        _writer.writeBoolean("IncludeName", includeName)
                    }
                }

                /**
                 * @param hero A hero.
                 */
                data class Data(val hero: Hero?) : Operation.Data {
                    @delegate:Transient
                    internal val _marshaller: ResponseFieldMarshaller by lazy {
                                ResponseFieldMarshaller { _writer ->
                                    _writer.writeObject("RESPONSE_FIELDS[0]", hero?._marshaller)
                                }
                            }

                    override fun marshaller() = _marshaller

                    companion object {
                        @JvmField
                        internal val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                                    ResponseField.forObject("hero", "hero", null, true, emptyList())
                                )

                        @JvmField
                        val MAPPER: ResponseFieldMapper<Data> = ResponseFieldMapper<Data> { _reader -> Data(
                                    _reader.readObject(RESPONSE_FIELDS[0], ResponseReader.ObjectReader<Hero> {
                                        Hero.MAPPER.map(it)
                                    })
                                )}
                    }
                }

                /**
                 * @param name Hero name.
                 */
                data class Hero(val __typename: String, val name: String?) {
                    @delegate:Transient
                    internal val _marshaller: ResponseFieldMarshaller by lazy {
                                ResponseFieldMarshaller { _writer ->
                                    _writer.writeString("RESPONSE_FIELDS[0]", __typename)
                                    _writer.writeString("RESPONSE_FIELDS[1]", name)
                                }
                            }

                    companion object {
                        @JvmField
                        internal val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                                    ResponseField.forString("__typename", "__typename", null, false, emptyList()),
                                    ResponseField.forString("name", "name", null, true, listOf(
                                        ResponseField.Condition.booleanCondition("IncludeName", false)
                                    ))
                                )

                        @JvmField
                        val MAPPER: ResponseFieldMapper<Hero> = ResponseFieldMapper<Hero> { _reader -> Hero(
                                    Utils.checkNotNull(_reader.readString(RESPONSE_FIELDS[0]), "__typename == null"),
                                    _reader.readString(RESPONSE_FIELDS[1])
                                )}
                    }
                }
            }
        """.trimIndent())
    }

    @Test
    fun `emits query operation type with simple fragment`() {
        val spec = OperationSpec(
            id = "1234",
            name = "TestQuery",
            javaType = JavaTypeName("com.example", "TestQuery"),
            operation = OperationType.QUERY,
            source = """
                    query TestQuery {
                      hero {
                        ...HeroDetails
                      }
                    }
                """.trimIndent(),
            data = OperationDataSpec(selections = SelectionSetSpec(
                fields = listOf(ResponseFieldSpec(
                    name = "hero",
                    type = heroRef,
                    selections = SelectionSetSpec(
                        fields = listOf(ResponseFieldSpec("fragments",
                            type = heroDetailsWrapperRef,
                            typeConditions = heroDetailsSpec.possibleTypes)),
                        fragments = heroDetailsWrapperRef.spec).withTypename()
                ))
            ))
        )

        assertThat(spec.typeSpec().code("com.example")).isEqualTo("""
            @Generated("Apollo GraphQL")
            class TestQuery : Query<TestQuery.Data, TestQuery.Data, Operation.Variables> {
                override fun name(): OperationName = OPERATION_NAME

                override fun operationId(): String = OPERATION_ID

                override fun queryDocument(): String = QUERY_DOCUMENT

                override fun wrapData(data: Data): Data = data

                override fun variables(): Operation.Variables = Operations.EMPTY_VARIABLES

                override fun responseFieldMapper(): ResponseFieldMapper<Data> = Data.MAPPER

                companion object {
                    const val OPERATION_DEFINITION: String = ""${'"'}
                            |query TestQuery {
                            |  hero {
                            |    ...HeroDetails
                            |  }
                            |}
                            ""${'"'}.trimMargin()

                    const val OPERATION_ID: String = "1234"

                    const val QUERY_DOCUMENT: String = OPERATION_DEFINITION

                    val OPERATION_NAME: OperationName = OperationName { "TestQuery" }
                }

                data class Data(val hero: Hero?) : Operation.Data {
                    @delegate:Transient
                    internal val _marshaller: ResponseFieldMarshaller by lazy {
                                ResponseFieldMarshaller { _writer ->
                                    _writer.writeObject("RESPONSE_FIELDS[0]", hero?._marshaller)
                                }
                            }

                    override fun marshaller() = _marshaller

                    companion object {
                        @JvmField
                        internal val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                                    ResponseField.forObject("hero", "hero", null, true, emptyList())
                                )

                        @JvmField
                        val MAPPER: ResponseFieldMapper<Data> = ResponseFieldMapper<Data> { _reader -> Data(
                                    _reader.readObject(RESPONSE_FIELDS[0], ResponseReader.ObjectReader<Hero> {
                                        Hero.MAPPER.map(it)
                                    })
                                )}
                    }
                }

                data class Hero(val __typename: String, val fragments: Fragments) {
                    @delegate:Transient
                    internal val _marshaller: ResponseFieldMarshaller by lazy {
                                ResponseFieldMarshaller { _writer ->
                                    _writer.writeString("RESPONSE_FIELDS[0]", __typename)
                                    fragments._marshaller.marshal(_writer)
                                }
                            }

                    companion object {
                        @JvmField
                        internal val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                                    ResponseField.forString("__typename", "__typename", null, false, emptyList()),
                                    ResponseField.forFragment("__typename", "__typename", listOf("Human",
                                            "Droid"))
                                )

                        @JvmField
                        val MAPPER: ResponseFieldMapper<Hero> = ResponseFieldMapper<Hero> { _reader -> Hero(
                                    Utils.checkNotNull(_reader.readString(RESPONSE_FIELDS[0]), "__typename == null"),
                                    Utils.checkNotNull(_reader.readConditional(RESPONSE_FIELDS[1], ResponseReader.ConditionalTypeReader<Fragments> { _conditionalType, _reader ->
                                        Fragments.MAPPER.map(_reader, _conditionalType)
                                    }), "fragments == null")
                                )}
                    }

                    data class Fragments(val heroDetails: HeroDetails) {
                        @delegate:Transient
                        val _marshaller: ResponseFieldMarshaller by lazy {
                                    ResponseFieldMarshaller { _writer ->
                                        heroDetails._marshaller.marshal(_writer)
                                    }
                                }

                        companion object {
                            @JvmField
                            val MAPPER: FragmentResponseFieldMapper<Fragments> =
                                    FragmentResponseFieldMapper<Fragments> { _reader, _conditionalType -> Fragments(
                                        Utils.checkNotNull(HeroDetails.MAPPER.takeIf { _conditionalType in HeroDetails.POSSIBLE_TYPES }?.map(_reader), "heroDetails == null")
                                    )}
                        }
                    }
                }
            }
        """.trimIndent())
    }

    @Test
    fun `writes variables type`() {
        val spec = OperationVariablesSpec(listOf(
            VariableSpec("enum", type = episodeRef.required(), defaultValue = EnumValue("NEWHOPE")),
            VariableSpec("int", type = intRef.optional(OptionalType.INPUT)),
            VariableSpec("date", type = customDateRef.optional(OptionalType.INPUT))
        ))
        assertThat(spec.typeSpec(ClassName("", "Variables")).code()).isEqualTo("""
            data class Variables(
                val enum: Episode,
                val int: Input<Int>,
                val date: Input<ZonedDateTime>
            ) : Operation.Variables {
                @delegate:Transient
                private val valueMap: Map<String, Any> by lazy {
                            listOfNotNull(
                                "enum" to enum,
                                ("int" to int).takeIf { int.defined },
                                ("date" to date).takeIf { date.defined }
                            ).toMap()
                        }

                override fun valueMap() = valueMap

                override fun marshaller() = InputFieldMarshaller { _writer ->
                    _writer.writeString("enum", enum.rawValue)
                    if (int.defined) {
                        _writer.writeInt("int", int.value)
                    }
                    if (date.defined) {
                        _writer.writeCustom("date", CustomType.DATE, date.value)
                    }
                }
            }
        """.trimIndent())
    }

    @Test
    fun `writes data type`() {
        val spec = OperationDataSpec(
            selections = SelectionSetSpec(
                fields = listOf(
                    ResponseFieldSpec("number", type = floatRef.required()),
                    ResponseFieldSpec("optionalNumber", type = floatRef.optional(OptionalType.APOLLO)),
                    ResponseFieldSpec("string", type = stringRef.required())
                )
            )
        )

        assertThat(spec.typeSpec().code()).isEqualTo("""
            data class Data internal constructor(
                val number: Double,
                val optionalNumber: Optional<Double>,
                val string: String
            ) : Operation.Data {
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

                override fun marshaller() = _marshaller

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
}
