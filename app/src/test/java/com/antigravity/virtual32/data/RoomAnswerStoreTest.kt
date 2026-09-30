package com.antigravity.virtual32.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.antigravity.virtual32.receiver.ai.RawAnswer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomAnswerStoreTest {
    private lateinit var db: AppDatabase
    private lateinit var store: RoomAnswerStore
    
    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        store = RoomAnswerStore(db.answerDao())
    }
    
    @After
    fun closeDb() {
        db.close()
    }
    
    @Test
    fun testEmpty() = runBlocking {
        val next = store.next()
        assertFalse(next.ok)
        assertEquals("empty", next.reason)
    }
    
    @Test
    fun testReplaceAndNext() = runBlocking {
        val answers = listOf(
            RawAnswer(1, "A", "high"),
            RawAnswer(2, "B", "low")
        )
        val batch = Batch(source = "SIM", photoPath = null, galleryUri = null, status = "ok", provider = "GEMINI", model = "m", promptName = "p", promptHash = "h", latencyMs = 0, rawResponse = null)
        
        store.applyBatch(batch, answers, "REPLACE")
        
        assertEquals(0, store.cursor)
        
        val res1 = store.next()
        assertTrue(res1.ok)
        assertEquals(1, res1.q)
        assertEquals("A", res1.choice)
        assertEquals(1, res1.blinks)
        
        assertEquals(1, store.cursor)
        
        val res2 = store.next()
        assertTrue(res2.ok)
        assertEquals(2, res2.q)
        assertEquals("B", res2.choice)
        assertEquals(2, res2.blinks)
        
        val res3 = store.next()
        assertFalse(res3.ok)
        assertEquals("wrap", res3.reason)
        assertEquals(true, res3.end)
        
        assertEquals(0, store.cursor) // Wrapped
    }

    @Test
    fun testAppend() = runBlocking {
        val batch = Batch(source = "SIM", photoPath = null, galleryUri = null, status = "ok", provider = "GEMINI", model = "m", promptName = "p", promptHash = "h", latencyMs = 0, rawResponse = null)
        store.applyBatch(batch, listOf(RawAnswer(1, "A", "high")), "REPLACE")
        store.applyBatch(batch.copy(id = 0), listOf(RawAnswer(2, "B", "high"), RawAnswer(1, "C", "high")), "APPEND")
        
        val active = store.activeAnswers().first()
        assertEquals(2, active.size)
        // Q1 should be overwritten by the newer batch with choice C
        assertEquals("C", active[0].choice)
        assertEquals(1, active[0].q)
        assertEquals("B", active[1].choice)
    }

    @Test
    fun testRepeat() = runBlocking {
        val batch = Batch(source = "SIM", photoPath = null, galleryUri = null, status = "ok", provider = "GEMINI", model = "m", promptName = "p", promptHash = "h", latencyMs = 0, rawResponse = null)
        store.applyBatch(batch, listOf(RawAnswer(1, "A", "high"), RawAnswer(2, "B", "low")), "REPLACE")
        
        store.next() // cursor becomes 1
        val r = store.repeat()
        assertTrue(r.ok)
        assertEquals(1, r.q) // Repeats the one just played
        
        store.next() // cursor becomes 2
        val r2 = store.repeat()
        assertTrue(r2.ok)
        assertEquals(2, r2.q)
    }

    @Test
    fun testEdit() = runBlocking {
        val batch = Batch(source = "SIM", photoPath = null, galleryUri = null, status = "ok", provider = "GEMINI", model = "m", promptName = "p", promptHash = "h", latencyMs = 0, rawResponse = null)
        store.applyBatch(batch, listOf(RawAnswer(1, "A", "high")), "REPLACE")
        
        val active = store.activeAnswers().first()
        val id = active[0].id
        
        store.editAnswer(id, "D")
        val updated = store.activeAnswers().first()
        assertEquals("D", updated[0].choice)
        assertTrue(updated[0].edited)
    }

    @Test
    fun testCursorPersistence() = runBlocking {
        val batch = Batch(source = "SIM", photoPath = null, galleryUri = null, status = "ok", provider = "GEMINI", model = "m", promptName = "p", promptHash = "h", latencyMs = 0, rawResponse = null)
        store.applyBatch(batch, listOf(RawAnswer(1, "A", "high"), RawAnswer(2, "B", "low")), "REPLACE")
        
        store.next()
        assertEquals(1, store.cursor)
        
        // Recreate store (simulating process restart)
        val newStore = RoomAnswerStore(db.answerDao())
        // Initialize flow
        newStore.activeAnswers().first()
        assertEquals(1, newStore.cursor)
    }

    @Test
    fun testRetention() = runBlocking {
        // Insert 105 batches
        for (i in 1..105) {
            val batch = Batch(source = "SIM", photoPath = null, galleryUri = null, status = "ok", provider = "GEMINI", model = "m", promptName = "p", promptHash = "h", latencyMs = 0, rawResponse = null)
            store.applyBatch(batch, listOf(RawAnswer(1, "A", "high")), "APPEND")
        }
        
        val c = db.query("SELECT COUNT(*) FROM batches", null)
        c.moveToFirst()
        val count = c.getInt(0)
        c.close()
        
        // Should only retain the last 100
        assertEquals(100, count)
    }
}
