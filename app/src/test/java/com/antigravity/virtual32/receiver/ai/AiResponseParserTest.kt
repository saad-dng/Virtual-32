package com.antigravity.virtual32.receiver.ai

import org.junit.Assert.*
import org.junit.Test

class AiResponseParserTest {

    @Test
    fun testParse_cleanJson() {
        val input = """{"status":"ok","answers":[{"q":1,"choice":"A","conf":"high"}]}"""
        val res = AiResponseParser.parse(input)
        assertEquals("ok", res.status)
        assertEquals(1, res.answers.size)
        assertEquals(1, res.answers[0].q)
        assertEquals("A", res.answers[0].choice)
    }

    @Test
    fun testParse_fenced() {
        val input = "```json\n{\"status\":\"ok\",\"answers\":[{\"q\":2,\"choice\":\"C\",\"conf\":\"low\"}]}\n```"
        val res = AiResponseParser.parse(input)
        assertEquals("ok", res.status)
        assertEquals(1, res.answers.size)
        assertEquals(2, res.answers[0].q)
        assertEquals("C", res.answers[0].choice)
    }

    @Test
    fun testParse_proseWrapped() {
        val input = "Here is the result:\n{\"status\":\"ok\",\"answers\":[{\"q\":3,\"choice\":\"B\",\"conf\":\"high\"}]}\nHave a nice day!"
        val res = AiResponseParser.parse(input)
        assertEquals("ok", res.status)
        assertEquals(1, res.answers.size)
    }

    @Test
    fun testParse_lowercaseChoice() {
        val input = """{"status":"ok","answers":[{"q":1,"choice":"e","conf":"high"}]}"""
        val res = AiResponseParser.parse(input)
        assertEquals("E", res.answers[0].choice)
    }

    @Test
    fun testParse_invalidChoice() {
        val input = """{"status":"ok","answers":[{"q":1,"choice":"X","conf":"high"}]}"""
        val res = AiResponseParser.parse(input)
        assertEquals("unclear", res.status) // Because valid answers became 0
        assertEquals(0, res.answers.size)
    }

    @Test
    fun testParse_missingFields() {
        val input = """{"status":"ok","answers":[{"choice":"A"}]}"""
        val res = AiResponseParser.parse(input)
        assertEquals("unclear", res.status)
    }

    @Test
    fun testParse_duplicatedQ() {
        val input = """{"status":"ok","answers":[{"q":1,"choice":"A","conf":"high"},{"q":1,"choice":"B","conf":"low"}]}"""
        val res = AiResponseParser.parse(input)
        assertEquals(1, res.answers.size)
        assertEquals("A", res.answers[0].choice)
    }

    @Test
    fun testParse_outOfOrder() {
        val input = """{"status":"ok","answers":[{"q":2,"choice":"B","conf":"high"},{"q":1,"choice":"A","conf":"high"}]}"""
        val res = AiResponseParser.parse(input)
        assertEquals(2, res.answers.size)
        assertEquals(1, res.answers[0].q)
        assertEquals(2, res.answers[1].q)
    }

    @Test
    fun testParse_garbage() {
        val input = "This is not json"
        val res = AiResponseParser.parse(input)
        assertEquals("error", res.status)
        assertTrue(res.isParseError)
    }

    @Test
    fun testParse_noAnswersOk() {
        val input = """{"status":"ok","answers":[]}"""
        val res = AiResponseParser.parse(input)
        assertEquals("unclear", res.status)
    }

    @Test
    fun testParse_unclearWithReason() {
        val input = """{"status":"unclear","reason":"Blurry image"}"""
        val res = AiResponseParser.parse(input)
        assertEquals("unclear", res.status)
        assertEquals("Blurry image", res.reason)
    }
    
    @Test
    fun testParse_malformedJson() {
        val input = """{"status":"ok", "answers": [{"q":1"""
        val res = AiResponseParser.parse(input)
        assertEquals("error", res.status)
        assertTrue(res.isParseError)
    }

    @Test
    fun testParse_qIsString() {
        val input = """{"status":"ok","answers":[{"q":"5","choice":"C","conf":"high"}]}"""
        val res = AiResponseParser.parse(input)
        assertEquals(5, res.answers[0].q)
    }

    @Test
    fun testParse_extraKeysIgnored() {
        val input = """{"status":"ok","extra":"foo","answers":[{"q":1,"choice":"A","conf":"high","extra2":123}]}"""
        val res = AiResponseParser.parse(input)
        assertEquals(1, res.answers.size)
    }

    @Test
    fun testParse_missingStatusDefaultsToUnclear() {
        val input = """{"answers":[]}"""
        val res = AiResponseParser.parse(input)
        assertEquals("unclear", res.status)
    }
}
