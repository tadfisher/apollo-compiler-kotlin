package com.apollographql.apollo.compiler.parsing

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import java.io.File
import java.io.InputStream

fun File.queryLexer(): GraphqlQueryLexer = inputStream().queryLexer()

fun File.queryParser(): GraphqlQueryParser = inputStream().queryParser()

fun InputStream.queryLexer(): GraphqlQueryLexer = GraphqlQueryLexer(CharStreams.fromStream(this))

fun InputStream.queryParser(): GraphqlQueryParser = GraphqlQueryParser(CommonTokenStream(queryLexer()))

fun String.queryLexer(): GraphqlQueryLexer = GraphqlQueryLexer(CharStreams.fromString(this))

fun String.queryParser(): GraphqlQueryParser = GraphqlQueryParser(CommonTokenStream(queryLexer()))
