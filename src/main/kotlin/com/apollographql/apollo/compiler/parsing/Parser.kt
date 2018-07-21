package com.apollographql.apollo.compiler.parsing

import com.apollographql.apollo.compiler.ast.AstLocation
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.tree.TerminalNode
import java.io.File
import java.io.InputStream

fun File.graphqlLexer(): GraphqlLexer = inputStream().graphqlLexer()

fun File.graphqlParser(): GraphqlParser = inputStream().graphqlParser()

fun InputStream.graphqlLexer() = GraphqlLexer(CharStreams.fromStream(this))

fun InputStream.graphqlParser() = GraphqlParser(CommonTokenStream(graphqlLexer()))

fun String.graphqlLexer() = GraphqlLexer(CharStreams.fromString(this))

fun String.graphqlParser() = GraphqlParser(CommonTokenStream(graphqlLexer()))

data class Point(val line: Int, val char: Int)
data class Location(val start: Point, val end: Point)

val Token.startPoint get() = Point(line, charPositionInLine)
val Token.endPoint get() = Point(line, charPositionInLine + text.length)
val Token.location get() = Location(startPoint, endPoint)

val ParserRuleContext.startPoint get() = start.startPoint
val ParserRuleContext.endPoint get() = stop.endPoint
val ParserRuleContext.location get() = Location(startPoint, endPoint)
val ParserRuleContext.astLocation get() = AstLocation(start.startIndex, start.line, start.charPositionInLine)

class ParseError(message: String) : RuntimeException()

val TerminalNode.intValue: Int get() = symbol.text.toInt()
val TerminalNode.floatValue: Double get() = symbol.text.toDouble()
val TerminalNode.stringValue: String get() = symbol.text
val TerminalNode.booleanValue: Boolean get() = symbol.text.toBoolean()
