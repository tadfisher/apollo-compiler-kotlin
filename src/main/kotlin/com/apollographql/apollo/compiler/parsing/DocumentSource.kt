package com.apollographql.apollo.compiler.parsing

import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CharStreams
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.util.UUID

interface DocumentSource {
    val id: String
    val mapper: SourceMapper
    fun charStream(): CharStream
    fun inputStream(): InputStream
    fun getLine(line: Int): String
    fun fullText(): String
}

class FileDocumentSource(
        override val id: String = UUID.randomUUID().toString(),
        private val source: File
) : DocumentSource {
    override val mapper by lazy { DefaultSourceMapper(id, this) }

    override fun charStream(): CharStream = CharStreams.fromStream(source.inputStream())

    override fun inputStream() = source.inputStream().buffered()

    override fun getLine(line: Int) = source.useLines { lines ->
        lines.drop(line - 1).first()
    }

    override fun fullText(): String = source.readText()
}

class StringDocumentSource(
        override val id: String = UUID.randomUUID().toString(),
        private val source: String
) : DocumentSource {
    override val mapper by lazy { DefaultSourceMapper(id, this) }

    override fun charStream(): CharStream = CharStreams.fromString(source)

    override fun inputStream(): BufferedInputStream = source.byteInputStream().buffered()

    override fun getLine(line: Int) = source.lineSequence().drop(line - 1).first()

    override fun fullText(): String = source
}
