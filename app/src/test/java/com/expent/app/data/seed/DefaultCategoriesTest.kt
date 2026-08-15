package com.expent.app.data.seed

import com.expent.app.ui.components.CategoryIcons
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultCategoriesTest {

    @Test
    fun `default categories are not empty`() {
        assertTrue(DefaultCategories.all.isNotEmpty())
    }

    @Test
    fun `default categories have unique names per type`() {
        val keys = DefaultCategories.all.map { "${it.type}:${it.name}" }
        assertEquals("duplicate category names", keys.size, keys.toSet().size)
    }

    @Test
    fun `every default category icon resolves`() {
        DefaultCategories.all.forEach { category ->
            assertTrue(
                "Unresolvable icon '${category.iconName}' for ${category.name}",
                CategoryIcons.supports(category.iconName)
            )
        }
    }

    @Test
    fun `default categories include both expense and income types`() {
        val types = DefaultCategories.all.map { it.type }.toSet()
        assertEquals(2, types.size)
        assertFalse(DefaultCategories.all.any { it.name.isBlank() })
    }
}
