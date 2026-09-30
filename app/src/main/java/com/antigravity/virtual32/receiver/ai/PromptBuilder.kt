package com.antigravity.virtual32.receiver.ai

object PromptBuilder {
    val DEFAULT_INSTRUCTION = "read every MCQ on the page, use the printed question numbers, choose the single best option."
    
    val LOCKED_CONTRACT = """
Return ONLY JSON: {"status":"ok"|"unclear","answers":[{"q":<int>,"choice":"A"-"E","conf":"high"|"low"}],"reason":"<short, only when unclear>"}. Use status unclear if the text is blurry, cropped, dark, unreadable, or contains no multiple-choice questions. Never guess.
""".trim()

    val LOCKED_CONTRACT_WITH_REASONING = """
Return ONLY JSON: {"status":"ok"|"unclear","answers":[{"q":<int>,"choice":"A"-"E","conf":"high"|"low","reasoning":"<at most 15 words>"}],"reason":"<short, only when unclear>"}. Use status unclear if the text is blurry, cropped, dark, unreadable, or contains no multiple-choice questions. Never guess.
""".trim()

    fun build(userInstruction: String, includeReasoning: Boolean = false, isRetry: Boolean = false): String {
        val instruction = if (userInstruction.isBlank()) DEFAULT_INSTRUCTION else userInstruction
        val contract = if (includeReasoning) LOCKED_CONTRACT_WITH_REASONING else LOCKED_CONTRACT
        var prompt = "$instruction\n\n$contract"
        if (isRetry) {
            prompt += "\n\nReturn valid JSON only. Previous output failed to parse."
        }
        return prompt
    }
}
