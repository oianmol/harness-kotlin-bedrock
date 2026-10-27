package io.github.oianmol.harness

import io.github.oianmol.harness.tools.KbTools
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KbToolsTest {
    private val read = KbTools.readFile(exampleKb)
    private val search = KbTools.search(exampleKb)
    private val list = KbTools.listFiles(exampleKb)

    @Test
    fun `read tool returns file contents`() {
        val result = read.run(mapOf("path" to "decisions/quickhttp.md"))
        assertFalse(result.isError)
        assertTrue("library: io.example:quickhttp" in result.content)
    }

    @Test
    fun `path traversal comes back as a tool error, not an exception`() {
        val result = read.run(mapOf("path" to "../build.gradle.kts"))
        assertTrue(result.isError)
        assertTrue("escapes" in result.content)
    }

    @Test
    fun `missing parameters are reported to the model`() {
        val result = read.run(emptyMap())
        assertTrue(result.isError)
    }

    @Test
    fun `search finds lines across the knowledge base`() {
        val result = search.run(mapOf("query" to "R8"))
        assertFalse(result.isError)
        assertTrue(result.content.lines().any { it.startsWith("decisions/fastjson-kt.md:") })
    }

    @Test
    fun `list tool lists a directory`() {
        val result = list.run(mapOf("directory" to "decisions"))
        assertEquals(9, result.content.lines().size)
    }
}
