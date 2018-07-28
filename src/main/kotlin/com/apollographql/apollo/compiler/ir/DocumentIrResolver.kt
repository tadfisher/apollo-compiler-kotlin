package com.apollographql.apollo.compiler.ir

import com.apollographql.apollo.compiler.ast.Document
import com.apollographql.apollo.compiler.ast.FragmentDefinition
import com.apollographql.apollo.compiler.ast.FragmentSpread as AstFragmentSpread
import com.apollographql.apollo.compiler.ast.OperationDefinition
import com.apollographql.apollo.compiler.ast.Selection
import com.apollographql.apollo.compiler.ast.SelectionContainer
import com.apollographql.apollo.compiler.ast.TypeDefinition
import java.util.Stack

class DocumentIrResolver(val document: Document) {

    private val fragmentSpreadsCache = linkedMapOf<SelectionContainer, List<AstFragmentSpread>>()

    private val recursivelyReferencedFragmentsCache =
            linkedMapOf<OperationDefinition, List<FragmentDefinition>>()

    private val recursivelyReferencedTypesCache =
            linkedMapOf<OperationDefinition, List<TypeDefinition>>()


    fun resolveExecutableIr(): ExecutableIr {
        // Shake the tree
        val operations = document.operations
        val fragments = operations.values.map { it.collectFragments() }
        TODO()
    }

    fun SelectionContainer.collectFragmentSpreads(): List<AstFragmentSpread> {
        return fragmentSpreadsCache.getOrPut(this) {
            val spreads = mutableListOf<AstFragmentSpread>()
            val setsToVisit = Stack<Selection>()

            selections.forEach { setsToVisit.push(it) }

            while (setsToVisit.isNotEmpty()) {
                val set = setsToVisit.pop()

                when (set) {
                    is AstFragmentSpread -> spreads += set
                    is SelectionContainer -> set.selections.forEach { setsToVisit.push(it) }
                }
            }

            spreads.toList()
        }
    }

    fun OperationDefinition.collectFragments(): List<FragmentDefinition> {
        val frags = mutableListOf<FragmentDefinition>()
        val collectedNames = mutableSetOf<String>()
        val nodesToVisit = Stack<SelectionContainer>()

        nodesToVisit.push(this)

        while (nodesToVisit.isNotEmpty()) {
            val node = nodesToVisit.pop()
            val spreads = node.collectFragmentSpreads()

            spreads.forEach { spread ->
                val fragName = spread.name

                if (!collectedNames.contains(fragName)) {
                    collectedNames += fragName

                    document.fragments[fragName]?.let { frag ->
                        frags += frag
                        nodesToVisit.push(frag)
                    }
                }
            }
        }

        return frags.toList()
    }



//    val separateOperations: Map<String?, Document>
//        get() = document.operations.mapValues { (_, definition) -> separateOperation(definition) }
//
//    fun separateOperation(definition: OperationDefinition): Document {
//        val definitions = listOf(definition) +
//                definition.collectFragments().sortedBy { it.location?.line ?: 0 }
//        return document.copy(definitions = definitions)
//    }
//
//    fun separateOperation(operationName: String?): Document? {
//        return if (operationName.isNullOrEmpty() && document.operations.size == 1) {
//            separateOperation(document.operations.values.first())
//        } else {
//            document.operations[operationName]?.let { separateOperation(it) }
//        }
//    }
//
}