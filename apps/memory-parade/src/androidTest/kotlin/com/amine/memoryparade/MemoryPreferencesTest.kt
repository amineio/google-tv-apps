package com.amine.memoryparade

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

class MemoryPreferencesTest {
    @Test fun originalRecordMigratesOnlyToClassicAndNewRecordsStaySeparate() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "record_migration_" + System.nanoTime()
        try {
            context.getSharedPreferences(name, 0).edit().putInt("best_moves", 13).putBoolean("sound", false).commit()
            val records = MemoryPreferences(context, name)
            assertEquals(13, records.best(BoardSize.CLASSIC))
            assertEquals(0, records.best(BoardSize.EASY))
            assertEquals(0, records.best(BoardSize.CHALLENGE))
            assertFalse(records.sound)
            assertEquals(3, records.record(BoardSize.EASY, 3))
            assertEquals(8, records.record(BoardSize.CHALLENGE, 8))
            assertEquals(13, records.record(BoardSize.CLASSIC, 15))
            assertEquals(9, records.record(BoardSize.CLASSIC, 9))
            records.board = BoardSize.CHALLENGE
            val reopened = MemoryPreferences(context, name)
            assertEquals(3, reopened.best(BoardSize.EASY))
            assertEquals(9, reopened.best(BoardSize.CLASSIC))
            assertEquals(8, reopened.best(BoardSize.CHALLENGE))
            assertEquals(BoardSize.CHALLENGE, reopened.board)
            assertFalse(reopened.sound)
        } finally { context.deleteSharedPreferences(name) }
    }
    @Test fun invalidLegacyRecordDoesNotBecomeARecord() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "invalid_record_" + System.nanoTime()
        try {
            context.getSharedPreferences(name, 0).edit().putInt("best_moves", 2).commit()
            assertEquals(0, MemoryPreferences(context, name).best(BoardSize.CLASSIC))
        } finally { context.deleteSharedPreferences(name) }
    }
}
