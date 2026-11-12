package com.kichikan.negah.domain.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ClassDayTest {
    @Test
    fun daysAreOrderedSaturdayThroughFriday() {
        assertEquals(
            listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه"),
            ClassDay.entries.sortedBy { it.order }.map { it.label },
        )
    }

    @Test
    fun orderMapsToTheExpectedDayAndRejectsUnknownValues() {
        assertEquals(ClassDay.SATURDAY, ClassDay.fromOrder(0))
        assertEquals(ClassDay.FRIDAY, ClassDay.fromOrder(6))
        assertNull(ClassDay.fromOrder(7))
    }
}
