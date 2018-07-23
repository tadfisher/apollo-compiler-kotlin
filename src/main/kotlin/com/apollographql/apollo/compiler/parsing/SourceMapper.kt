package com.apollographql.apollo.compiler.parsing

import com.apollographql.apollo.compiler.ast.AstLocation

interface SourceMapper {
    val id: String
    val source: String
    fun renderLocation(location: AstLocation): String
    fun renderLinePosition(location: AstLocation, prefix: String = ""): String
}

class DefaultSourceMapper(
        override val id: String,
        val documentSource: DocumentSource
) : SourceMapper {
    override val source by lazy { documentSource.fullText() }

    override fun renderLocation(location: AstLocation) =
            "line ${location.line}, column ${location.column}"

    override fun renderLinePosition(location: AstLocation, prefix: String): String {
        return documentSource.getLine(location.line)
                .replace("\r", "") + "\n" + prefix + (" ".repeat(location.column - 1)) + "^"
    }
}

class AggregateSourceMapper(
        override val id: String,
        val delegates: List<SourceMapper>
) : SourceMapper {
    override val source by lazy { delegates.joinToString("\n\n") { it.source.trim() } }

    val delegatesById by lazy { delegates.associateBy { it.id } }

    override fun renderLocation(location: AstLocation): String =
            delegatesById[location.sourceId]?.renderLocation(location) ?: ""

    override fun renderLinePosition(location: AstLocation, prefix: String): String =
            delegatesById[location.sourceId]?.renderLinePosition(location, prefix) ?: ""

    companion object {
        fun merge(mappers: List<SourceMapper>): AggregateSourceMapper {
            fun expand(sm: SourceMapper): List<SourceMapper> =
                (sm as? AggregateSourceMapper)?.delegates?.flatMap(::expand) ?: listOf(sm)

            return AggregateSourceMapper("merged", mappers.flatMap(::expand))
        }
    }
}