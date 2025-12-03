package org.menagerie.puppet_master

import kotlinx.serialization.Serializable

@Serializable
data class SpecialEffectsManager(
    val effects: List<SpecialEffect> = emptyList(),
    val activeEffectIndex: Int = -1
) {

    fun addEffect(): SpecialEffectsManager {
        val newEffect = SpecialEffect()
        val newEffects = effects + newEffect
        return this.copy(effects = newEffects, activeEffectIndex = newEffects.lastIndex)
    }

    fun deleteEffect(index: Int): SpecialEffectsManager {
        if (index !in effects.indices) return this

        val newEffects = effects.toMutableList()
        newEffects.removeAt(index)

        var newActiveEffectIndex = activeEffectIndex
        if (newActiveEffectIndex >= index) {
            newActiveEffectIndex--
        }
        if (newActiveEffectIndex < 0 && newEffects.isNotEmpty()) {
            newActiveEffectIndex = 0
        } else if (newEffects.isEmpty()) {
            newActiveEffectIndex = -1
        }

        return this.copy(effects = newEffects, activeEffectIndex = newActiveEffectIndex)
    }

    fun updateEffect(index: Int, effect: SpecialEffect): SpecialEffectsManager {
        if (index !in effects.indices) return this
        val newEffects = effects.toMutableList()
        newEffects[index] = effect
        return copy(effects = newEffects)
    }

    fun nextEffect(): SpecialEffectsManager {
        if (effects.isEmpty()) return this
        val newActiveEffectIndex = (activeEffectIndex + 1) % effects.size
        return this.copy(activeEffectIndex = newActiveEffectIndex)
    }

    fun previousEffect(): SpecialEffectsManager {
        if (effects.isEmpty()) return this
        val newActiveEffectIndex = if (activeEffectIndex - 1 < 0) {
            effects.lastIndex
        } else {
            activeEffectIndex - 1
        }
        return this.copy(activeEffectIndex = newActiveEffectIndex)
    }

    fun getActiveEffect(): SpecialEffect? {
        return effects.getOrNull(activeEffectIndex)
    }

    fun isNameUnique(name: String, ignoreIndex: Int = -1): Boolean {
        return effects.withIndex().none { (i, effect) -> effect.name == name && i != ignoreIndex }
    }
}
