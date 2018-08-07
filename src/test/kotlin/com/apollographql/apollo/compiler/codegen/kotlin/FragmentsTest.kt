package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.compiler.ir.FragmentSpec
import com.apollographql.apollo.compiler.ir.FragmentTypeRef
import com.apollographql.apollo.compiler.ir.FragmentsWrapperSpec
import com.apollographql.apollo.compiler.ir.FragmentsWrapperTypeRef
import com.apollographql.apollo.compiler.ir.JavaTypeName
import com.apollographql.apollo.compiler.ir.ListTypeRef
import com.apollographql.apollo.compiler.ir.ObjectTypeRef
import com.apollographql.apollo.compiler.ir.ResponseFieldSpec
import com.apollographql.apollo.compiler.ir.SelectionSetSpec
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FragmentsTest {
    @Test
    fun `emits simple fragment type`() {
        assertThat(heroDetailsSpec.typeSpec().code("com.example.fragments")).isEqualTo("""
            /**
             * @param name The name of the character
             */
            @Generated("Apollo GraphQL")
            data class HeroDetails(val __typename: String, val name: String) : GraphqlFragment {
                @delegate:Transient
                internal val _marshaller: ResponseFieldMarshaller by lazy {
                            ResponseFieldMarshaller { _writer ->
                                _writer.writeString("RESPONSE_FIELDS[0]", __typename)
                                _writer.writeString("RESPONSE_FIELDS[1]", name)
                            }
                        }

                /**
                 * @param name The name of the character
                 */
                constructor(name: String) : this("HeroDetails", name)

                override fun __typename(): String = __typename

                companion object {
                    @JvmField
                    internal val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                                ResponseField.forString("__typename", "__typename", null, false, emptyList()),
                                ResponseField.forString("name", "name", null, false, emptyList())
                            )

                    @JvmField
                    val MAPPER: ResponseFieldMapper<HeroDetails> =
                            ResponseFieldMapper<HeroDetails> { _reader -> HeroDetails(
                                Utils.checkNotNull(_reader.readString(RESPONSE_FIELDS[0]), "__typename == null"),
                                Utils.checkNotNull(_reader.readString(RESPONSE_FIELDS[1]), "name == null")
                            )}

                    const val FRAGMENT_DEFINITION: String = ""${'"'}
                            |fragment HeroDetails on Character {
                            |  name
                            |}
                            ""${'"'}.trimMargin()

                    val POSSIBLE_TYPES: List<String> = listOf("Human", "Droid")
                }
            }
        """.trimIndent())
    }

    @Test
    fun `emits fragment type with nested fragment`() {
        val pilotSpec = FragmentSpec(
            name = "pilotFragment",
            javaType = JavaTypeName("com.example.fragments", "PilotFragment"),
            source = """
                    fragment pilotFragment on Person {
                      __typename
                      name
                      homeworld {
                        __typename
                        name
                      }
                    }
                """.trimIndent(),
            typeCondition = "Person",
            possibleTypes = listOf("Person"),
            selections = SelectionSetSpec(fields = listOf(
                ResponseFieldSpec("name", type = stringRef),
                ResponseFieldSpec("homeworld",
                    type = ObjectTypeRef("Homeworld", JavaTypeName("com.example.fragments", "HomeWorld")),
                    selections = SelectionSetSpec(listOf(
                        ResponseFieldSpec("name", type = stringRef)
                    )).withTypename())
            )).withTypename()
        )

        val wrapperSpec = FragmentsWrapperSpec(listOf(
            ResponseFieldSpec("pilotFragment",
                type = FragmentTypeRef(pilotSpec).required())
        ))

        val spec = FragmentSpec(
            name = "starshipFragment",
            javaType = JavaTypeName("com.example.fragments", "StarshipFragment"),
            source = """
                    fragment starshipFragment on Starship {
                      __typename
                      id
                      name
                      pilotConnection {
                        __typename
                        edges {
                          __typename
                          node {
                            __typename
                            ...pilotFragment
                          }
                        }
                      }
                    }
                """.trimIndent(),
            typeCondition = "Starship",
            possibleTypes = listOf("Starship"),
            selections = SelectionSetSpec(fields = listOf(
                ResponseFieldSpec("id", type = idRef),
                ResponseFieldSpec("name", type = stringRef),
                ResponseFieldSpec("pilotConnection",
                    type = ObjectTypeRef("PilotConnection",
                        JavaTypeName("com.example.fragments", "StarshipFragment.PilotConnection")),
                    selections = SelectionSetSpec(fields = listOf(
                        ResponseFieldSpec("edges",
                            type = ListTypeRef(ObjectTypeRef("Edge",
                                JavaTypeName("com.example.fragments", "StarshipFragment.Edge"))),
                            selections = SelectionSetSpec(fields = listOf(
                                ResponseFieldSpec("node",
                                    type = ObjectTypeRef("Node",
                                        JavaTypeName("com.example.fragments", "StarshipFragment.Node")),
                                    selections = SelectionSetSpec(
                                        fields = listOf(ResponseFieldSpec("fragments",
                                            type = FragmentsWrapperTypeRef(wrapperSpec),
                                            typeConditions = pilotSpec.possibleTypes)),
                                        fragments = wrapperSpec
                                    ).withTypename()
                                ))).withTypename()
                        ))
                    ).withTypename())
            )).withTypename()
        )

        assertThat(spec.typeSpec().code("com.example.fragments")).isEqualTo("""
            @Generated("Apollo GraphQL")
            data class StarshipFragment(
                val __typename: String,
                val id: String,
                val name: String?,
                val pilotConnection: PilotConnection?
            ) : GraphqlFragment {
                @delegate:Transient
                internal val _marshaller: ResponseFieldMarshaller by lazy {
                            ResponseFieldMarshaller { _writer ->
                                _writer.writeString("RESPONSE_FIELDS[0]", __typename)
                                _writer.writeString("RESPONSE_FIELDS[1]", id)
                                _writer.writeString("RESPONSE_FIELDS[2]", name)
                                _writer.writeObject("RESPONSE_FIELDS[3]", pilotConnection?._marshaller)
                            }
                        }

                constructor(
                    id: String,
                    name: String?,
                    pilotConnection: PilotConnection?
                ) : this("StarshipFragment", id, name, pilotConnection)

                override fun __typename(): String = __typename

                companion object {
                    @JvmField
                    internal val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                                ResponseField.forString("__typename", "__typename", null, false, emptyList()),
                                ResponseField.forString("id", "id", null, false, emptyList()),
                                ResponseField.forString("name", "name", null, true, emptyList()),
                                ResponseField.forObject("pilotConnection", "pilotConnection", null, true, emptyList())
                            )

                    @JvmField
                    val MAPPER: ResponseFieldMapper<StarshipFragment> =
                            ResponseFieldMapper<StarshipFragment> { _reader -> StarshipFragment(
                                Utils.checkNotNull(_reader.readString(RESPONSE_FIELDS[0]), "__typename == null"),
                                Utils.checkNotNull(_reader.readString(RESPONSE_FIELDS[1]), "id == null"),
                                _reader.readString(RESPONSE_FIELDS[2]),
                                _reader.readObject(RESPONSE_FIELDS[3], ResponseReader.ObjectReader<PilotConnection> {
                                    PilotConnection.MAPPER.map(it)
                                })
                            )}

                    const val FRAGMENT_DEFINITION: String = ""${'"'}
                            |fragment starshipFragment on Starship {
                            |  __typename
                            |  id
                            |  name
                            |  pilotConnection {
                            |    __typename
                            |    edges {
                            |      __typename
                            |      node {
                            |        __typename
                            |        ...pilotFragment
                            |      }
                            |    }
                            |  }
                            |}
                            ""${'"'}.trimMargin()

                    val POSSIBLE_TYPES: List<String> = listOf("Starship")
                }

                data class PilotConnection(val __typename: String, val edges: List<Edge?>?) {
                    @delegate:Transient
                    internal val _marshaller: ResponseFieldMarshaller by lazy {
                                ResponseFieldMarshaller { _writer ->
                                    _writer.writeString("RESPONSE_FIELDS[0]", __typename)
                                    _writer.writeList("RESPONSE_FIELDS[1]", ResponseWriter.ListWriter { _itemWriter ->
                                        edges?.forEach { _itemWriter.writeObject(it?._marshaller) }
                                    })
                                }
                            }

                    constructor(edges: List<Edge?>?) : this("PilotConnection", edges)

                    companion object {
                        @JvmField
                        internal val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                                    ResponseField.forString("__typename", "__typename", null, false, emptyList()),
                                    ResponseField.forList("edges", "edges", null, true, emptyList())
                                )

                        @JvmField
                        val MAPPER: ResponseFieldMapper<PilotConnection> =
                                ResponseFieldMapper<PilotConnection> { _reader -> PilotConnection(
                                    Utils.checkNotNull(_reader.readString(RESPONSE_FIELDS[0]), "__typename == null"),
                                    _reader.readList(RESPONSE_FIELDS[1], ResponseReader.ListReader<List<Edge?>> { _itemReader ->
                                        _itemReader.readObject(ResponseReader.ObjectReader<Edge> {
                                            Edge.MAPPER.map(it)
                                        })
                                    })
                                )}
                    }
                }

                data class Edge(val __typename: String, val node: Node?) {
                    @delegate:Transient
                    internal val _marshaller: ResponseFieldMarshaller by lazy {
                                ResponseFieldMarshaller { _writer ->
                                    _writer.writeString("RESPONSE_FIELDS[0]", __typename)
                                    _writer.writeObject("RESPONSE_FIELDS[1]", node?._marshaller)
                                }
                            }

                    constructor(node: Node?) : this("Edge", node)

                    companion object {
                        @JvmField
                        internal val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                                    ResponseField.forString("__typename", "__typename", null, false, emptyList()),
                                    ResponseField.forObject("node", "node", null, true, emptyList())
                                )

                        @JvmField
                        val MAPPER: ResponseFieldMapper<Edge> = ResponseFieldMapper<Edge> { _reader -> Edge(
                                    Utils.checkNotNull(_reader.readString(RESPONSE_FIELDS[0]), "__typename == null"),
                                    _reader.readObject(RESPONSE_FIELDS[1], ResponseReader.ObjectReader<Node> {
                                        Node.MAPPER.map(it)
                                    })
                                )}
                    }
                }

                data class Node(val __typename: String, val fragments: Fragments) {
                    @delegate:Transient
                    internal val _marshaller: ResponseFieldMarshaller by lazy {
                                ResponseFieldMarshaller { _writer ->
                                    _writer.writeString("RESPONSE_FIELDS[0]", __typename)
                                    fragments._marshaller.marshal(_writer)
                                }
                            }

                    constructor(fragments: Fragments) : this("Node", fragments)

                    companion object {
                        @JvmField
                        internal val RESPONSE_FIELDS: Array<ResponseField> = arrayOf(
                                    ResponseField.forString("__typename", "__typename", null, false, emptyList()),
                                    ResponseField.forFragment("__typename", "__typename", listOf("Person"))
                                )

                        @JvmField
                        val MAPPER: ResponseFieldMapper<Node> = ResponseFieldMapper<Node> { _reader -> Node(
                                    Utils.checkNotNull(_reader.readString(RESPONSE_FIELDS[0]), "__typename == null"),
                                    Utils.checkNotNull(_reader.readConditional(RESPONSE_FIELDS[1], ResponseReader.ConditionalTypeReader<Fragments> { _conditionalType, _reader ->
                                        Fragments.MAPPER.map(_reader, _conditionalType)
                                    }), "fragments == null")
                                )}
                    }

                    data class Fragments(val pilotFragment: PilotFragment) {
                        @delegate:Transient
                        val _marshaller: ResponseFieldMarshaller by lazy {
                                    ResponseFieldMarshaller { _writer ->
                                        pilotFragment._marshaller.marshal(_writer)
                                    }
                                }

                        companion object {
                            @JvmField
                            val MAPPER: FragmentResponseFieldMapper<Fragments> =
                                    FragmentResponseFieldMapper<Fragments> { _reader, _conditionalType -> Fragments(
                                        Utils.checkNotNull(PilotFragment.MAPPER.takeIf { _conditionalType in PilotFragment.POSSIBLE_TYPES }?.map(_reader), "pilotFragment == null")
                                    )}
                        }
                    }
                }
            }
        """.trimIndent())
    }
}