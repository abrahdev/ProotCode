package dev.prootcode.app.ui.viewmodels

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WizardViewModelTest {
    private val viewModel = WizardViewModel()

    @Test
    fun `next clamps at the install step`() {
        repeat(10) { viewModel.next() }

        assertEquals(WizardViewModel.LAST_STEP, viewModel.uiState.value.step)
    }

    @Test
    fun `back clamps at the first step`() {
        repeat(5) { viewModel.back() }

        assertEquals(0, viewModel.uiState.value.step)
    }

    @Test
    fun `selecting a distro resets the version`() {
        viewModel.selectVersion(2)
        viewModel.selectDistro(1)

        assertEquals(1, viewModel.uiState.value.distroIndex)
        assertEquals(0, viewModel.uiState.value.versionIndex)
    }

    @Test
    fun `tools toggle on and off`() {
        assertTrue(viewModel.uiState.value.tools["git"] == true)

        viewModel.toggleTool("git")
        assertFalse(viewModel.uiState.value.tools["git"] == true)

        viewModel.toggleTool("git")
        assertTrue(viewModel.uiState.value.tools["git"] == true)
    }

    @Test
    fun `mounts are unique`() {
        viewModel.addMount("/a")
        viewModel.addMount("/a")
        viewModel.addMount("/b")

        assertEquals(listOf("/a", "/b"), viewModel.uiState.value.extraMounts)
    }

    @Test
    fun `reset restores defaults`() {
        viewModel.setName("machine")
        viewModel.setIcon("server")
        viewModel.next()

        viewModel.reset()

        assertEquals(WizardState(), viewModel.uiState.value)
    }

    @Test
    fun `name is truncated to forty characters`() {
        viewModel.setName("x".repeat(80))

        assertEquals(40, viewModel.uiState.value.name.length)
    }
}
