package com.universalremote.data

import com.universalremote.core.model.ThemePreference
import com.universalremote.domain.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class HapticPreferencesTest {

    private class FakePreferencesRepository : PreferencesRepository {
        private val themeFlow = MutableStateFlow(ThemePreference.SYSTEM)
        private val hapticFlow = MutableStateFlow(true)
        private val animFlow = MutableStateFlow(false)

        override val theme: Flow<ThemePreference> = themeFlow
        override val hapticEnabled: Flow<Boolean> = hapticFlow
        override val reduceAnimations: Flow<Boolean> = animFlow

        override suspend fun setTheme(theme: ThemePreference) { themeFlow.value = theme }
        override suspend fun setHapticEnabled(enabled: Boolean) { hapticFlow.value = enabled }
        override suspend fun setReduceAnimations(reduced: Boolean) { animFlow.value = reduced }
    }

    @Test
    fun defaultHapticPreferencesAreEnabled() = runTest {
        val repo = FakePreferencesRepository()
        assertTrue(repo.hapticEnabled.first())
        assertFalse(repo.reduceAnimations.first())
    }

    @Test
    fun setHapticEnabledUpdatesPreferenceFlow() = runTest {
        val repo = FakePreferencesRepository()
        repo.setHapticEnabled(false)
        assertFalse(repo.hapticEnabled.first())

        repo.setReduceAnimations(true)
        assertTrue(repo.reduceAnimations.first())
    }
}
