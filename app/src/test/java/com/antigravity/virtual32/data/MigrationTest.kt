package com.antigravity.virtual32.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class MigrationTest {

    private lateinit var dbHelper: SupportSQLiteOpenHelper
    private lateinit var db: SupportSQLiteDatabase
    private val dbName = "migration_test.db"

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(dbName)

        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Create v1 schema
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `batches` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            `source` TEXT NOT NULL,
                            `photoPath` TEXT,
                            `galleryUri` TEXT,
                            `status` TEXT NOT NULL,
                            `provider` TEXT NOT NULL,
                            `model` TEXT NOT NULL,
                            `promptName` TEXT NOT NULL,
                            `promptHash` TEXT NOT NULL,
                            `latencyMs` INTEGER NOT NULL,
                            `rawResponse` TEXT,
                            `superseded` INTEGER NOT NULL
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `answers` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `batchId` INTEGER NOT NULL,
                            `q` INTEGER NOT NULL,
                            `choice` TEXT NOT NULL,
                            `conf` TEXT NOT NULL,
                            `edited` INTEGER NOT NULL,
                            `note` TEXT,
                            FOREIGN KEY(`batchId`) REFERENCES `batches`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                    """.trimIndent())

                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_answers_batchId` ON `answers` (`batchId`)")

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `cycle_state` (
                            `id` INTEGER NOT NULL,
                            `activeBatchIds` TEXT NOT NULL,
                            `cursor` INTEGER NOT NULL,
                            PRIMARY KEY(`id`)
                        )
                    """.trimIndent())
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    // No-op in initial creation
                }
            })
            .build()

        dbHelper = FrameworkSQLiteOpenHelperFactory().create(config)
        db = dbHelper.writableDatabase
    }

    @After
    fun tearDown() {
        db.close()
        dbHelper.close()
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(dbName)
    }

    @Test
    fun testMigration1To2PreservesDataAndAddsColumns() {
        // 1. Insert v1 batch and answer
        db.execSQL("""
            INSERT INTO batches (id, createdAt, source, photoPath, galleryUri, status, provider, model, promptName, promptHash, latencyMs, rawResponse, superseded)
            VALUES (1, 1000, 'SIM', '/path/p1.jpg', 'content://media/1', 'ok', 'GEMINI', 'gemini-flash', 'default', 'hash1', 500, '{"answers":[]}', 0)
        """.trimIndent())

        db.execSQL("""
            INSERT INTO answers (id, batchId, q, choice, conf, edited, note)
            VALUES (1, 1, 1, 'B', 'high', 0, NULL)
        """.trimIndent())

        // 2. Apply MIGRATION_1_2
        AppDatabase.MIGRATION_1_2.migrate(db)

        // 3. Verify existing row in batches has the new default values and columns
        val cursorBatch = db.query("SELECT id, pageCount, photoPaths, galleryUris, warnings FROM batches WHERE id = 1")
        assertTrue(cursorBatch.moveToFirst())
        assertEquals(1, cursorBatch.getInt(cursorBatch.getColumnIndexOrThrow("id")))
        assertEquals(1, cursorBatch.getInt(cursorBatch.getColumnIndexOrThrow("pageCount")))
        assertNull(cursorBatch.getString(cursorBatch.getColumnIndexOrThrow("photoPaths")))
        assertNull(cursorBatch.getString(cursorBatch.getColumnIndexOrThrow("galleryUris")))
        assertNull(cursorBatch.getString(cursorBatch.getColumnIndexOrThrow("warnings")))
        cursorBatch.close()

        // 4. Verify existing row in answers has page default = 1
        val cursorAnswer = db.query("SELECT id, batchId, q, choice, page FROM answers WHERE id = 1")
        assertTrue(cursorAnswer.moveToFirst())
        assertEquals(1, cursorAnswer.getInt(cursorAnswer.getColumnIndexOrThrow("id")))
        assertEquals(1, cursorAnswer.getInt(cursorAnswer.getColumnIndexOrThrow("batchId")))
        assertEquals(1, cursorAnswer.getInt(cursorAnswer.getColumnIndexOrThrow("q")))
        assertEquals("B", cursorAnswer.getString(cursorAnswer.getColumnIndexOrThrow("choice")))
        assertEquals(1, cursorAnswer.getInt(cursorAnswer.getColumnIndexOrThrow("page")))
        cursorAnswer.close()

        // 5. Test inserting new v2 records with multi-page fields
        db.execSQL("""
            INSERT INTO batches (id, createdAt, source, photoPath, galleryUri, status, provider, model, promptName, promptHash, latencyMs, rawResponse, superseded, pageCount, photoPaths, galleryUris, warnings)
            VALUES (2, 2000, 'SESSION', '/path/p1.jpg', 'content://media/1', 'ok', 'CLAUDE', 'claude-haiku', 'default', 'hash2', 1200, '{}', 0, 3, '["/path/p1.jpg","/path/p2.jpg","/path/p3.jpg"]', '["uri1","uri2","uri3"]', '["Q3 missing"]')
        """.trimIndent())

        db.execSQL("""
            INSERT INTO answers (id, batchId, q, choice, conf, edited, note, page)
            VALUES (2, 2, 4, 'C', 'high', 0, NULL, 2)
        """.trimIndent())

        val cursorNewBatch = db.query("SELECT pageCount, photoPaths, galleryUris, warnings FROM batches WHERE id = 2")
        assertTrue(cursorNewBatch.moveToFirst())
        assertEquals(3, cursorNewBatch.getInt(0))
        assertEquals("[\"/path/p1.jpg\",\"/path/p2.jpg\",\"/path/p3.jpg\"]", cursorNewBatch.getString(1))
        assertEquals("[\"uri1\",\"uri2\",\"uri3\"]", cursorNewBatch.getString(2))
        assertEquals("[\"Q3 missing\"]", cursorNewBatch.getString(3))
        cursorNewBatch.close()

        val cursorNewAnswer = db.query("SELECT q, page FROM answers WHERE id = 2")
        assertTrue(cursorNewAnswer.moveToFirst())
        assertEquals(4, cursorNewAnswer.getInt(0))
        assertEquals(2, cursorNewAnswer.getInt(1))
        cursorNewAnswer.close()
    }
}
