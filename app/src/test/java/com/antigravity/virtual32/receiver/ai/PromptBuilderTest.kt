package com.antigravity.virtual32.receiver.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptBuilderTest {

    @Test
    fun `build appends locked contract by default`() {
        val instruction = "Read the page"
        val prompt = PromptBuilder.build(instruction, includeReasoning = false, isRetry = false)
        
        assertTrue(prompt.startsWith("Read the page\n\n"))
        assertTrue(prompt.contains("Return ONLY JSON:"))
        assertTrue(prompt.contains("\"status\":\"ok\"|\"unclear\""))
        assertTrue(prompt.contains("\"answers\":[{\"q\":<int>,\"choice\":\"A\"-\"E\",\"conf\":\"high\"|\"low\"}]"))
    }

    @Test
    fun `build appends reasoning locked contract when reasoning enabled`() {
        val instruction = "Read the page"
        val prompt = PromptBuilder.build(instruction, includeReasoning = true, isRetry = false)
        
        assertTrue(prompt.startsWith("Read the page\n\n"))
        assertTrue(prompt.contains("Return ONLY JSON:"))
        assertTrue(prompt.contains("\"status\":\"ok\"|\"unclear\""))
        assertTrue(prompt.contains("\"answers\":[{\"q\":<int>,\"choice\":\"A\"-\"E\",\"conf\":\"high\"|\"low\",\"reasoning\":\"<at most 15 words>\"}]"))
    }

    @Test
    fun `build uses default instruction if input is blank`() {
        val prompt = PromptBuilder.build("", includeReasoning = false, isRetry = false)
        assertTrue(prompt.startsWith(PromptBuilder.DEFAULT_INSTRUCTION))
    }

    @Test
    fun `build appends retry note if isRetry is true`() {
        val instruction = "Read the page"
        val prompt = PromptBuilder.build(instruction, includeReasoning = false, isRetry = true)
        assertTrue(prompt.endsWith("Return valid JSON only. Previous output failed to parse."))
    }
}
