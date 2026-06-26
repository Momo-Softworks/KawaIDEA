package com.momosoftworks.kawaidea

import com.intellij.formatting.Alignment
import com.intellij.formatting.Block
import com.intellij.formatting.ChildAttributes
import com.intellij.formatting.Indent
import com.intellij.formatting.Spacing
import com.intellij.formatting.Wrap
import com.intellij.lang.ASTNode
import com.intellij.psi.TokenType
import com.intellij.psi.formatter.common.AbstractBlock
import com.momosoftworks.kawaidea.psi.KawaTypes

/**
 * One node in the formatting tree. The interesting logic lives on list/vector
 * nodes, which pick one of three indentation modes (see [SeqMode]); every other
 * node is a thin pass-through with no extra indent.
 *
 * Spacing is intentionally always null: we never join or split lines, we only
 * correct the leading indent — which is exactly how Lisp code should be formatted.
 *
 * Body indent is a literal 2 spaces (`getSpaceIndent(2)`) rather than the
 * configurable `getNormalIndent()`. Lisp indentation is 2 by convention, and this
 * makes the formatter immune to the IDE's indent-size setting and to "Detect and
 * use existing file indents" — which otherwise makes reformat preserve a file's
 * existing 4-space indent and appear to do nothing.
 */
class KawaBlock(
    node: ASTNode,
    wrap: Wrap?,
    alignment: Alignment?,
    private val indent: Indent,
) : AbstractBlock(node, wrap, alignment) {

    private enum class SeqMode { SPECIAL, CALL, DATA }

    private val isSequence: Boolean =
        node.elementType == KawaTypes.LIST || node.elementType == KawaTypes.VECTOR

    /** Indent mode for this list/vector, computed once from its head. */
    private val mode: SeqMode by lazy { computeMode() }

    /**
     * Shared alignment for CALL/DATA modes. Null when [ALIGN_ARGUMENTS] is off,
     * which collapses every mode to a uniform +2 indent (the Emacs
     * `lisp-indent-offset` / fixed-two-spaces style).
     */
    private val seqAlignment: Alignment? by lazy {
        if (ALIGN_ARGUMENTS && mode != SeqMode.SPECIAL) Alignment.createAlignment() else null
    }

    override fun getIndent(): Indent = indent

    override fun getSpacing(child1: Block?, child2: Block): Spacing? = null

    override fun isLeaf(): Boolean = node.firstChildNode == null

    override fun buildChildren(): List<Block> =
        if (isSequence) buildSequenceChildren() else buildPassThroughChildren()

    /** File / form / quoted / atom: children carry no extra indent of their own. */
    private fun buildPassThroughChildren(): List<Block> {
        val blocks = ArrayList<Block>()
        var child = node.firstChildNode
        while (child != null) {
            if (isSignificant(child)) {
                blocks.add(KawaBlock(child, null, null, Indent.getNoneIndent()))
            }
            child = child.treeNext
        }
        return blocks
    }

    private fun buildSequenceChildren(): List<Block> {
        val blocks = ArrayList<Block>()
        var elementIndex = 0 // 0 = head, 1 = first arg/element, ...
        var child = node.firstChildNode
        while (child != null) {
            if (isSignificant(child)) {
                when {
                    isDelimiter(child) ->
                        blocks.add(KawaBlock(child, null, null, Indent.getNoneIndent()))

                    isComment(child) ->
                        // Comments ride with the body but don't count as elements.
                        blocks.add(KawaBlock(child, null, seqAlignment, Indent.getSpaceIndent(2)))

                    else -> {
                        blocks.add(elementBlock(child, elementIndex))
                        elementIndex++
                    }
                }
            }
            child = child.treeNext
        }
        return blocks
    }

    private fun elementBlock(child: ASTNode, elementIndex: Int): KawaBlock {
        val isHead = elementIndex == 0
        return when (mode) {
            // Body forms after the head indent +2; the head itself sits flush.
            SeqMode.SPECIAL ->
                KawaBlock(child, null, null, if (isHead) Indent.getNoneIndent() else Indent.getSpaceIndent(2))

            // Function call: head flush; args align under the first argument.
            SeqMode.CALL ->
                if (isHead) KawaBlock(child, null, null, Indent.getNoneIndent())
                else KawaBlock(child, null, seqAlignment, Indent.getSpaceIndent(2))

            // Data list: every element aligns under the first element.
            SeqMode.DATA ->
                KawaBlock(child, null, seqAlignment, Indent.getSpaceIndent(2))
        }
    }

    override fun getChildAttributes(newChildIndex: Int): ChildAttributes {
        if (!isSequence) return ChildAttributes(Indent.getNoneIndent(), null)
        return when (mode) {
            SeqMode.SPECIAL -> ChildAttributes(Indent.getSpaceIndent(2), null)
            SeqMode.CALL, SeqMode.DATA -> ChildAttributes(Indent.getSpaceIndent(2), seqAlignment)
        }
    }

    private fun computeMode(): SeqMode {
        if (node.elementType == KawaTypes.VECTOR) return SeqMode.DATA
        val head = firstElement() ?: return SeqMode.DATA
        val headText = headSymbolText(head) ?: return SeqMode.DATA // head is a list/quote -> data
        return if (headText in KawaForms.BODY_INDENT_FORMS) SeqMode.SPECIAL else SeqMode.CALL
    }

    private fun firstElement(): ASTNode? {
        var child = node.firstChildNode
        while (child != null) {
            if (isSignificant(child) && !isDelimiter(child) && !isComment(child)) return child
            child = child.treeNext
        }
        return null
    }

    /** Drill FORM -> ATOM -> SYMBOL (robust if Grammar-Kit collapses a layer). */
    private fun headSymbolText(element: ASTNode): String? {
        var n: ASTNode? = element
        if (n?.elementType == KawaTypes.FORM) n = n.firstChildNode
        if (n?.elementType == KawaTypes.ATOM) n = n.firstChildNode
        return if (n?.elementType == KawaTypes.SYMBOL) n.text else null
    }

    private fun isSignificant(child: ASTNode): Boolean =
        child.elementType != TokenType.WHITE_SPACE && child.textLength > 0

    private fun isDelimiter(child: ASTNode): Boolean = when (child.elementType) {
        KawaTypes.LPAREN, KawaTypes.RPAREN,
        KawaTypes.LBRACKET, KawaTypes.RBRACKET,
        KawaTypes.HASH_LPAREN -> true
        else -> false
    }

    private fun isComment(child: ASTNode): Boolean =
        child.elementType == KawaTypes.LINE_COMMENT || child.elementType == KawaTypes.BLOCK_COMMENT

    companion object {
        /**
         * false (default) = uniform 2-space indent: every nested form indents +2,
         *   no alignment (Emacs `lisp-indent-offset` / fixed-offset style).
         * true = align call arguments / data elements under the first one
         *   (default Emacs `scheme-mode`). Flip this to restore that behavior.
         */
        private const val ALIGN_ARGUMENTS = true
    }
}
