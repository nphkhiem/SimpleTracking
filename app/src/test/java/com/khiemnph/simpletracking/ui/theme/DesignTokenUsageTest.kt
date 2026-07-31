package com.khiemnph.simpletracking.ui.theme

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Two of the brief's success criteria, enforced rather than reviewed.
 *
 * Colours must come from `?attr/` semantic roles, and type from the named scale. Both are the kind
 * of rule that holds right up until someone in a hurry writes a literal, and the module-boundary
 * work already showed that a documented-but-unchecked rule drifts.
 */
class DesignTokenUsageTest {

    private fun resourceFiles(vararg directories: String): List<File> =
        directories.flatMap { directory ->
            File("src/main/res/$directory").walkTopDown().filter { it.isFile && it.extension == "xml" }
        }

    private val uiResources get() = resourceFiles("layout", "layout-land", "drawable")

    @Test
    fun `no layout or drawable hardcodes a colour resource`() {
        // `@android:color/transparent` is the framework's own and has no semantic role to use
        // instead, so it is allowed.
        val offenders = uiResources.flatMap { file ->
            Regex("""@color/(\w+)""").findAll(file.readText())
                .map { "${file.name}: @color/${it.groupValues[1]}" }
        }

        assertEquals(
            "Use a ?attr/ semantic role so the colour follows the theme, including dark mode",
            emptyList<String>(),
            offenders,
        )
    }

    @Test
    fun `no layout sets a text size inline`() {
        val offenders = resourceFiles("layout", "layout-land").flatMap { file ->
            Regex("""android:textSize="([^"]+)"""").findAll(file.readText())
                .map { "${file.name}: textSize=${it.groupValues[1]}" }
        }

        assertEquals(
            "Use a TextAppearance from type.xml so the scale stays consistent and scalable",
            emptyList<String>(),
            offenders,
        )
    }

    @Test
    fun `the legacy palette is gone`() {
        val colours = File("src/main/res/values/colors.xml").readText()
        val leftovers = listOf("history_surface", "history_ink", "history_border", "history_accent")
            .filter { colours.contains("\"$it\"") }

        assertEquals("superseded by md_theme roles", emptyList<String>(), leftovers)
    }

    @Test
    fun `the check is actually reading resource files`() {
        assertTrue("expected resources to scan", uiResources.size > 5)
    }
}
