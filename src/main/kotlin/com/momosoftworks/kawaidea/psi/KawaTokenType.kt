package com.momosoftworks.kawaidea.psi

import com.intellij.psi.tree.IElementType
import com.momosoftworks.kawaidea.KawaLanguage
import org.jetbrains.annotations.NonNls

class KawaTokenType(@NonNls debugName: String) : IElementType(debugName, KawaLanguage) {
    override fun toString(): String = "KawaTokenType." + super.toString()
}
