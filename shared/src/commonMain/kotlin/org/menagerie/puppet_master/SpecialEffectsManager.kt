package org.menagerie.puppet_master

import kotlinx.serialization.Serializable

@Serializable
class SpecialEffectsManager {
    val effects = mutableListOf<SpecialEffect>()
    var activeEffectIndex = -1

    fun addEffect() {
        effects.add(SpecialEffect())
        activeEffectIndex = effects.lastIndex
    }

    fun deleteEffect(index: Int) {
        if (index in effects.indices) {
            effects.removeAt(index)
            if (activeEffectIndex >= index) {
                activeEffectIndex--
            }
            if (activeEffectIndex < 0 && effects.isNotEmpty()) {
                activeEffectIndex = 0
            } else if (effects.isEmpty()) {
                activeEffectIndex = -1
            }
        }
    }

    fun nextEffect() {
        if (effects.isNotEmpty()) {
            activeEffectIndex = (activeEffectIndex + 1) % effects.size
        }
    }

    fun previousEffect() {
        if (effects.isNotEmpty()) {
            activeEffectIndex = if (activeEffectIndex - 1 < 0) {
                effects.lastIndex
            } else {
                activeEffectIndex - 1
            }
        }
    }

    fun getActiveEffect(): SpecialEffect? {
        return effects.getOrNull(activeEffectIndex)
    }

    fun updateEffectName(index: Int, newName: String) {
        if (index in effects.indices) {
            effects[index].name = newName
        }
    }

    fun isNameUnique(name: String): Boolean {
        return effects.none { it.name == name }
    }
}
