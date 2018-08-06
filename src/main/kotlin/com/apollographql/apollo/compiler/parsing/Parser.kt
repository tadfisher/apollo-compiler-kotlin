package com.apollographql.apollo.compiler.parsing

import com.apollographql.apollo.compiler.ast.Document
import com.apollographql.apollo.compiler.ast.Value
import java.io.File
import java.util.UUID

fun File.graphqlParser(
    sourceId: String = UUID.randomUUID().toString(),
    withAstLocation: Boolean = true
) = DocumentParser(FileDocumentSource(source = this), withAstLocation)

fun String.graphqlParser(
    sourceId: String = UUID.randomUUID().toString(),
    withAstLocation: Boolean = false
) = DocumentParser(StringDocumentSource(id = sourceId, source = this), withAstLocation)

fun File.parseGraphqlDocument(
    sourceId: String = UUID.randomUUID().toString(),
    withAstLocation: Boolean = true
): Document = graphqlParser(sourceId, withAstLocation).documentAst()
fun String.parseGraphqlDocument(
    sourceId: String = UUID.randomUUID().toString(),
    withAstLocation: Boolean = true
): Document = graphqlParser(sourceId, withAstLocation).documentAst()

fun String.parseGraphqlValue(const: Boolean = true, withAstLocation: Boolean = false): Value =
        graphqlParser("value-string", withAstLocation).valueAst(const)

class ParseError(message: String) : RuntimeException()
