package dev.haseeb.handi.ui.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EditorViewModelTest {

    @Test fun `blank input normalizes to null (field is optional)`() {
        assertNull(EditorViewModel.normalizedSourceUrl(""))
        assertNull(EditorViewModel.normalizedSourceUrl("   "))
    }

    @Test fun `trims surrounding whitespace`() {
        assertEquals("https://example.com/recipe", EditorViewModel.normalizedSourceUrl("  https://example.com/recipe  "))
    }

    @Test fun `leaves a url that already has a scheme untouched`() {
        assertEquals("https://example.com/recipe", EditorViewModel.normalizedSourceUrl("https://example.com/recipe"))
        assertEquals("http://example.com/recipe", EditorViewModel.normalizedSourceUrl("http://example.com/recipe"))
    }

    @Test fun `prefixes https onto a bare domain so it's always openable`() {
        assertEquals("https://example.com/recipe", EditorViewModel.normalizedSourceUrl("example.com/recipe"))
        assertEquals("https://www.youtube.com/watch?v=abc", EditorViewModel.normalizedSourceUrl("www.youtube.com/watch?v=abc"))
    }
}
