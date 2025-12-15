package org.menagerie.puppet_master

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class SpecialEffectsManager(
    val effects: List<SpecialEffect> = emptyList(),
    val activeEffectIndex: Int = -1,
    @Transient val newEffect: SpecialEffect? = null
) {

    @Transient
    val isCreatingEffect: Boolean = newEffect != null

    fun startCreatingEffect(): SpecialEffectsManager {
        if (isCreatingEffect) return this
        return copy(newEffect = SpecialEffect(), activeEffectIndex = -1)
    }

    fun saveNewEffect(): SpecialEffectsManager {
        if (newEffect == null) return this
        val newEffects = effects + newEffect
        return copy(effects = newEffects, activeEffectIndex = newEffects.lastIndex, newEffect = null)
    }

    fun discardNewEffect(): SpecialEffectsManager {
        return copy(newEffect = null, activeEffectIndex = if (effects.isNotEmpty()) 0 else -1)
    }

    fun updateNewEffect(effect: SpecialEffect): SpecialEffectsManager {
        return copy(newEffect = effect)
    }

    fun deleteEffect(index: Int): SpecialEffectsManager {
        if (index !in effects.indices || isCreatingEffect) return this

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
        if (index !in effects.indices || isCreatingEffect) return this
        val newEffects = effects.toMutableList()
        newEffects[index] = effect
        return copy(effects = newEffects)
    }

    fun nextEffect(): SpecialEffectsManager {
        if (effects.isEmpty() || isCreatingEffect) return this
        val newActiveEffectIndex = (activeEffectIndex + 1) % effects.size
        return this.copy(activeEffectIndex = newActiveEffectIndex)
    }

    fun previousEffect(): SpecialEffectsManager {
        if (effects.isEmpty() || isCreatingEffect) return this
        val newActiveEffectIndex = if (activeEffectIndex - 1 < 0) {
            effects.lastIndex
        } else {
            activeEffectIndex - 1
        }
        return this.copy(activeEffectIndex = newActiveEffectIndex)
    }

    fun getActiveEffect(): SpecialEffect? {
        return newEffect ?: effects.getOrNull(activeEffectIndex)
    }

    fun isNameUnique(name: String, ignoreIndex: Int = -1): Boolean {
        if (isCreatingEffect) {
            return effects.none { it.name == name }
        }
        return effects.withIndex().none { (i, effect) -> effect.name == name && i != ignoreIndex }
    }
}
