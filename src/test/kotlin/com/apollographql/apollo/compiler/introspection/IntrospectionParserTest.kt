package com.apollographql.apollo.compiler.introspection

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class IntrospectionParserTest {
    @Test
    fun `parses introspection schema file`() {
        val source = javaClass.getResourceAsStream("/schema.json")
        val schema = source.parseIntrospectionSchema()
        assertThat(schema).isNotNull()
    }
}