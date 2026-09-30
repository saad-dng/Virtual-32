package com.antigravity.virtual32.receiver.ai

object PromptBuilder {
    val DEFAULT_INSTRUCTION = "read every MCQ on the page, use the printed question numbers, choose the single best option."
    
    val LOCKED_CONTRACT = """
Return ONLY JSON: {"status":"ok"|"unclear","answers":[{"q":<int>,"choice":"A"-"E","conf":"high"|"low"}],"reason":"<short, only when unclear>"}. Use status unclear if the text is blurry, cropped, dark, unreadable, or contains no multiple-choice questions. Never guess.
""".trim()

    fun build(userInstruction: String, isRetry: Boolean = false): String {
        val instruction = if (userInstruction.isBlank()) DEFAULT_INSTRUCTION else userInstruction
        var prompt = "$instruction\n\n$LOCKED_CONTRACT"
        if (isRetry) {
            prompt += "\n\nReturn valid JSON only. Previous output failed to parse."
        }
        return prompt
    }
}
