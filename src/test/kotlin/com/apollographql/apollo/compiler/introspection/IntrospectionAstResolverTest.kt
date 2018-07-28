package com.apollographql.apollo.compiler.introspection

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class IntrospectionAstResolverTest {

    @Test
    fun `resolves schema document`() {
        val source = javaClass.getResourceAsStream("/schema.json")
        val schema = source.parseIntrospectionSchema()
        val document = schema.resolveDocument()
        assertThat(document).isNotNull()
    }
}