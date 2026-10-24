package com.example

import com.example.ui.components.NavDestination
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationDestinationTest {
    @Test
    fun navDestinations_hasExactlyFiveTabsInRequiredOrder() {
        val destinations = NavDestination.entries
        assertEquals(5, destinations.size)
        assertEquals("Home", destinations[0].label)
        assertEquals("Syllabus", destinations[1].label)
        assertEquals("Focus", destinations[2].label)
        assertEquals("Calendar", destinations[3].label)
        assertEquals("Tools", destinations[4].label)
    }
}
