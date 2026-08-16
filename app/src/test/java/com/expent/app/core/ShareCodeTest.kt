package com.expent.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ShareCodeTest {

    private val allowed = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toSet()

    @Test
    fun `generated codes have the canonical length and a safe alphabet`() {
        repeat(100) {
            val code = ShareCode.generate()
            assertEquals(ShareCode.CODE_LENGTH, code.length)
            assertTrue("code '$code' contains a confusing character", code.all { it in allowed })
        }
    }

    @Test
    fun `generation is reproducible with a seeded random`() {
        assertEquals(ShareCode.generate(random = Random(42)), ShareCode.generate(random = Random(42)))
        assertFalse(ShareCode.generate(random = Random(42)) == ShareCode.generate(random = Random(43)))
    }

    @Test
    fun `normalize uppercases, strips junk, and truncates`() {
        assertEquals("K7M2QX", ShareCode.normalize("k7m 2qx"))
        assertEquals("K7M2QX", ShareCode.normalize("k7m-2qx!"))
        assertEquals("K7M2QX", ShareCode.normalize(" k7m2qxtoo-long "))
        assertEquals("", ShareCode.normalize(""))
    }

    @Test
    fun `generateUnique avoids codes already in use`() {
        val existing = setOf("AAAAAA", "BBBBBB", "CCCCCC")
        val code = ShareCode.generateUnique(existing, random = Random(7))
        assertFalse(code in existing)
        assertEquals(ShareCode.CODE_LENGTH, code.length)
    }
}
