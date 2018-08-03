package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.api.Input
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.ScalarType
import com.apollographql.apollo.api.internal.Optional
import com.apollographql.apollo.compiler.ast.EnumValue
import com.apollographql.apollo.compiler.ast.OperationType
import com.apollographql.apollo.compiler.ast.VariableValue
import com.apollographql.apollo.compiler.ir.FragmentSpec
import com.apollographql.apollo.compiler.ir.FragmentSpreadSpec
import com.apollographql.apollo.compiler.ir.OperationDataSpec
import com.apollographql.apollo.compiler.ir.OperationSpec
import com.apollographql.apollo.compiler.ir.OperationVariablesSpec
import com.apollographql.apollo.compiler.ir.ResponseFieldSpec
import com.apollographql.apollo.compiler.ir.SelectionSetSpec
import com.apollographql.apollo.compiler.ir.TypeKind
import com.apollographql.apollo.compiler.ir.TypeRef
import com.apollographql.apollo.compiler.ir.VariableSpec
import com.google.common.truth.Truth.assertThat
import com.squareup.kotlinpoet.ClassName
import org.junit.Test
import java.util.Date

class OperationsTest {

    @Test
    fun `emits query operation type`() {
        val spec = OperationSpec(
                id = "1234",
                name = "TestQuery",
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
                optionalType = null,
                data = OperationDataSpec(selections = SelectionSetSpec(fields = listOf(
                        ResponseFieldSpec(
                                name = "hero",
                                doc = "A hero.",
                                type = TypeRef(
                                        name = "Hero",
                                        kind = TypeKind.OBJECT
                                ),
                                selections = SelectionSetSpec(fields = listOf(
                                        typenameSpec,
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
                                ))
                )))),
                variables = null)

        assertThat(spec.typeSpec().code()).isEqualTo("""
            @Generated("Apollo GraphQL")
            class TestQuery : Query<TestQuery.Data, TestQuery.Data, Operation.Variables> {
                override fun name() = OPERATION_NAME

                override fun operationId() = OPERATION_ID

                override fun queryDocument() = QUERY_DOCUMENT

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
                        val MAPPER: ResponseFieldMapper<Data> = ResponseFieldMapper<Data> { _reader ->
                                    val hero: Hero? = _reader.readObject(RESPONSE_FIELDS[0], ResponseReader.ObjectReader<Hero> {
                                        Hero.MAPPER.map(it)
                                    })
                                    Data(hero)
                                }
                    }
                }

                /**
                 * @param id ID of the hero.
                 * @param name Hero name.
                 */
                data class Hero(
                    val __typename: String,
                    val id: String?,
                    val name: String?
                ) {
                    internal val _marshaller: ResponseFieldMarshaller by lazy {
                                ResponseFieldMarshaller { _writer ->
                                    _writer.writeString("RESPONSE_FIELDS[0]", __typename)
                                    _writer.writeCustom("RESPONSE_FIELDS[1]", CustomType.ID, id)
                                    _writer.writeString("RESPONSE_FIELDS[2]", name)
                                }
                            }

                    /**
                     * @param id ID of the hero.
                     * @param name Hero name.
                     */
                    constructor(id: String?, name: String?) : this("Hero", id, name)

                    companion object {
                        @JvmField
                        internal val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                                    ResponseField.forString("__typename", "__typename", null, false, emptyList()),
                                    ResponseField.forCustomType("id", "id", null, true, CustomType.ID, emptyList()),
                                    ResponseField.forString("name", "name", null, true, emptyList())
                                )

                        @JvmField
                        val MAPPER: ResponseFieldMapper<Hero> = ResponseFieldMapper<Hero> { _reader ->
                                    val __typename: String = Utils.checkNotNull(_reader.readString(RESPONSE_FIELDS[0]), "__typename == null")
                                    val id: String? = _reader.readCustomType(RESPONSE_FIELDS[1] as ResponseField.CustomTypeField)
                                    val name: String? = _reader.readString(RESPONSE_FIELDS[2])
                                    Hero(__typename, id, name)
                                }
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
                operation = OperationType.QUERY,
                source = """
                    query TestQuery(${'$'}epsiode: Episode, ${'$'}IncludeName: Boolean!
                        hero(episode: ${'$'}episode) {
                          __typename
                          name @include(if: ${'$'}IncludeName)
                        }
                    }
                """.trimIndent(),
                optionalType = null,
                data = OperationDataSpec(selections = SelectionSetSpec(fields = listOf(
                        ResponseFieldSpec(
                                name = "hero",
                                doc = "A hero.",
                                type = TypeRef(
                                        name = "Hero",
                                        kind = TypeKind.OBJECT
                                ),
                                selections = SelectionSetSpec(fields = listOf(
                                        typenameSpec,
                                        ResponseFieldSpec(
                                                name = "name",
                                                doc = "Hero name.",
                                                type = stringRef,
                                                includeIf = listOf(VariableValue("IncludeName"))
                                        )
                                ))
                        )))),
                variables = OperationVariablesSpec(variables = listOf(
                        VariableSpec(
                                name = "episode",
                                type = episodeRef.copy(optionalType = Input::class)
                        ),
                        VariableSpec(
                                name = "IncludeName",
                                type = booleanRef.copy(isOptional = false)
                        )
                )))

        assertThat(spec.typeSpec().code()).isEqualTo("""
            @Generated("Apollo GraphQL")
            class TestQuery internal constructor(private val variables: Variables) : Query<TestQuery.Data, TestQuery.Data, TestQuery.Variables> {
                constructor(episode: Input<Episode>, includeName: Boolean) : this(Variables(episode,%WIncludeName))

                override fun name() = OPERATION_NAME

                override fun operationId() = OPERATION_ID

                override fun queryDocument() = QUERY_DOCUMENT

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
                            _writer.writeString("episode", episode.value?.rawValue())
                        }
                        _writer.writeBoolean("IncludeName", includeName)
                    }
                }

                /**
                 * @param hero A hero.
                 */
                data class Data(val hero: Hero?) : Operation.Data {
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
                        val MAPPER: ResponseFieldMapper<Data> = ResponseFieldMapper<Data> { _reader ->
                                    val hero: Hero? = _reader.readObject(RESPONSE_FIELDS[0], ResponseReader.ObjectReader<Hero> {
                                        Hero.MAPPER.map(it)
                                    })
                                    Data(hero)
                                }
                    }
                }

                /**
                 * @param name Hero name.
                 */
                data class Hero(val __typename: String, val name: String?) {
                    internal val _marshaller: ResponseFieldMarshaller by lazy {
                                ResponseFieldMarshaller { _writer ->
                                    _writer.writeString("RESPONSE_FIELDS[0]", __typename)
                                    _writer.writeString("RESPONSE_FIELDS[1]", name)
                                }
                            }

                    /**
                     * @param name Hero name.
                     */
                    constructor(name: String?) : this("Hero", name)

                    companion object {
                        @JvmField
                        internal val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                                    ResponseField.forString("__typename", "__typename", null, false, emptyList()),
                                    ResponseField.forString("name", "name", null, true, listOf(
                                        ResponseField.Condition.booleanCondition("IncludeName", false)
                                    ))
                                )

                        @JvmField
                        val MAPPER: ResponseFieldMapper<Hero> = ResponseFieldMapper<Hero> { _reader ->
                                    val __typename: String = Utils.checkNotNull(_reader.readString(RESPONSE_FIELDS[0]), "__typename == null")
                                    val name: String? = _reader.readString(RESPONSE_FIELDS[1])
                                    Hero(__typename, name)
                                }
                    }
                }
            }
        """.trimIndent())
    }

    @Test
    fun `emits query operation type with simple fragment`() {
        val fragmentSpec = FragmentSpec(
                name = "HeroDetails",
                source = """
                    fragment HeroDetails on Character {
                      name
                    }
                """.trimIndent(),
                selections = SelectionSetSpec(fields = listOf(
                        ResponseFieldSpec(
                                name = "name",
                                doc = "The name of the character",
                                type = stringRef.copy(isOptional = false)
                        )
                ))
        )

        val spec = OperationSpec(
                id = "1234",
                name = "TestQuery",
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
                                        fragmentSpreads = listOf(
                                                FragmentSpreadSpec(fragmentSpec)
                                        )
                                )
                        ))
                ))
        )
    }

    @Test
    fun `writes variables type`() {
        val spec = OperationVariablesSpec(listOf(
                VariableSpec(
                        name = "enum",
                        type = TypeRef(
                                name = "Enum",
                                jvmName = Enum::class.qualifiedName!!,
                                kind = TypeKind.ENUM,
                                isOptional = false
                        ),
                        defaultValue = EnumValue("THREE")
                ),
                VariableSpec(
                        name = "int",
                        type = TypeRef(
                                name = "Int",
                                jvmName = Int::class.qualifiedName!!,
                                kind = TypeKind.INT,
                                isOptional = true,
                                optionalType = Input::class
                        ),
                        defaultValue = null
                ),
                VariableSpec(
                        name = "date",
                        type = TypeRef(
                                name = CustomType.DATE::class.qualifiedName!!,
                                jvmName = CustomType.DATE.javaType().canonicalName,
                                kind = TypeKind.CUSTOM,
                                isOptional = true,
                                optionalType = Input::class,
                                parameters = emptyList()
                        ),
                        defaultValue = null
                )
        ))
        assertThat(spec.typeSpec(ClassName("", "Variables")).code()).isEqualTo("""
            data class Variables(
                val enum: Enum,
                val int: Input<Int>,
                val date: Input<Date>
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
                    _writer.writeString("enum", enum.rawValue())
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
                                ResponseFieldSpec(
                                        name = "number",
                                        type = floatRef.copy(
                                                isOptional = false,
                                                optionalType = Optional::class
                                        )
                                ),
                                ResponseFieldSpec(
                                        name = "optionalNumber",
                                        type = floatRef.copy(
                                                isOptional = true,
                                                optionalType = Optional::class
                                        )
                                ),
                                ResponseFieldSpec(
                                        name = "string",
                                        type = stringRef.copy(isOptional = false)
                                )
                        )
                )
        )

        assertThat(spec.typeSpec().code()).isEqualTo("""
            data class Data internal constructor(
                val number: Double,
                val optionalNumber: Optional<Double>,
                val string: String
            ) : Operation.Data {
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
                    val MAPPER: ResponseFieldMapper<Data> = ResponseFieldMapper<Data> { _reader ->
                                val number: Double = Utils.checkNotNull(_reader.readDouble(RESPONSE_FIELDS[0]), "number == null")
                                val optionalNumber: Double? = _reader.readDouble(RESPONSE_FIELDS[1])
                                val string: String = Utils.checkNotNull(_reader.readString(RESPONSE_FIELDS[2]), "string == null")
                                Data(number, optionalNumber, string)
                            }
                }
            }
        """.trimIndent())
    }
}

private enum class Enum { ONE, TWO, THREE, FOUR }

private enum class CustomType : ScalarType {
    ID {
        override fun typeName() = "ID"
        override fun javaType() = String::class.java
    },
    DATE {
        override fun typeName() = "Date"
        override fun javaType() = Date::class.java
    }
}
