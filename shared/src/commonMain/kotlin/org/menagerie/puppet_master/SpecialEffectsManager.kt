package org.menagerie.puppet_master

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Manages a collection of [SpecialEffect]s and the currently active effect.
 *
 * This class is immutable. All functions that modify the state return a new instance of the manager.
 *
 * @property effects The list of available special effects.
 * @property activeEffectIndex The index of the currently active effect in the [effects] list.
 * @property newEffect A temporary [SpecialEffect] that is being created.
 */
@Serializable
data class SpecialEffectsManager(
    val effects: List<SpecialEffect> = emptyList(),
    val activeEffectIndex: Int = -1,
    @Transient val newEffect: SpecialEffect? = null
) {

    /**
     * `true` if a new effect is currently being created.
     */
    @Transient
    val isCreatingEffect: Boolean = newEffect != null

    /**
     * Starts the process of creating a new special effect.
     *
     * @return A new [SpecialEffectsManager] instance with a new [SpecialEffect] being created.
     */
    fun startCreatingEffect(): SpecialEffectsManager {
        if (isCreatingEffect) return this
        return copy(newEffect = SpecialEffect(), activeEffectIndex = -1)
    }

    /**
     * Saves the new effect that is currently being created.
     *
     * @return A new [SpecialEffectsManager] instance with the new effect added to the list of effects.
     */
    fun saveNewEffect(): SpecialEffectsManager {
        if (newEffect == null) return this
        val newEffects = effects + newEffect
        return copy(effects = newEffects, activeEffectIndex = newEffects.lastIndex, newEffect = null)
    }

    /**
     * Discards the new effect that is currently being created.
     *
     * @return A new [SpecialEffectsManager] instance with the new effect discarded.
     */
    fun discardNewEffect(): SpecialEffectsManager {
        return copy(newEffect = null, activeEffectIndex = if (effects.isNotEmpty()) 0 else -1)
    }

    /**
     * Updates the new effect that is currently being created.
     *
     * @param effect The updated [SpecialEffect].
     * @return A new [SpecialEffectsManager] instance with the new effect updated.
     */
    fun updateNewEffect(effect: SpecialEffect): SpecialEffectsManager {
        return copy(newEffect = effect)
    }

    /**
     * Deletes an effect from the list of effects.
     *
     * @param index The index of the effect to delete.
     * @return A new [SpecialEffectsManager] instance with the effect removed.
     */
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

    /**
     * Updates an effect in the list of effects.
     *
     * @param index The index of the effect to update.
     * @param effect The updated [SpecialEffect].
     * @return A new [SpecialEffectsManager] instance with the effect updated.
     */
    fun updateEffect(index: Int, effect: SpecialEffect): SpecialEffectsManager {
        if (index !in effects.indices || isCreatingEffect) return this
        val newEffects = effects.toMutableList()
        newEffects[index] = effect
        return copy(effects = newEffects)
    }

    /**
     * Switches to the next effect in the list.
     *
     * @return A new [SpecialEffectsManager] instance with the next effect as the active effect.
     */
    fun nextEffect(): SpecialEffectsManager {
        if (effects.isEmpty() || isCreatingEffect) return this
        val newActiveEffectIndex = (activeEffectIndex + 1) % effects.size
        return this.copy(activeEffectIndex = newActiveEffectIndex)
    }

    /**
     * Switches to the previous effect in the list.
     *
     * @return A new [SpecialEffectsManager] instance with the previous effect as the active effect.
     */
    fun previousEffect(): SpecialEffectsManager {
        if (effects.isEmpty() || isCreatingEffect) return this
        val newActiveEffectIndex = if (activeEffectIndex - 1 < 0) {
            effects.lastIndex
        } else {
            activeEffectIndex - 1
        }
        return this.copy(activeEffectIndex = newActiveEffectIndex)
    }

    /**
     * Gets the currently active effect.
     *
     * If a new effect is being created, it returns the new effect. Otherwise, it returns the effect
     * at the [activeEffectIndex].
     *
     * @return The active [SpecialEffect], or `null` if there is no active effect.
     */
    fun getActiveEffect(): SpecialEffect? {
        return newEffect ?: effects.getOrNull(activeEffectIndex)
    }

    /**
     * Checks if a name is unique among the effects.
     *
     * @param name The name to check.
     * @param ignoreIndex The index of an effect to ignore when checking for uniqueness. This is
     * useful when updating an existing effect.
     * @return `true` if the name is unique, `false` otherwise.
     */
    fun isNameUnique(name: String, ignoreIndex: Int = -1): Boolean {
        if (isCreatingEffect) {
            return effects.none { it.name == name }
        }
        return effects.withIndex().none { (i, effect) -> effect.name == name && i != ignoreIndex }
    }
}
