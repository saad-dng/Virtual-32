package com.antigravity.virtual32.receiver.ai

object PromptBuilder {
    val DEFAULT_INSTRUCTION = "read every MCQ on the page, use the printed question numbers, choose the single best option."
    
    fun defaultMultiPhotoInstruction(n: Int): String =
        "These $n photos are consecutive parts of ONE question paper, in order from the first photo to the last. Read them together as one continuous document. A question may be cut off at the edge of one photo and continue in the next. Neighbouring photos may overlap, so the same question can appear twice: answer each question number once, using the most complete view. Use the printed question numbers."

    val LOCKED_CONTRACT = """
Return ONLY JSON: {"status":"ok"|"unclear","answers":[{"q":<int>,"choice":"A"-"E","conf":"high"|"low"}],"reason":"<short, only when unclear>"}. Use status unclear if the text is blurry, cropped, dark, unreadable, or contains no multiple-choice questions. Never guess.
""".trim()

    val LOCKED_CONTRACT_WITH_REASONING = """
Return ONLY JSON: {"status":"ok"|"unclear","answers":[{"q":<int>,"choice":"A"-"E","conf":"high"|"low","reasoning":"<at most 15 words>"}],"reason":"<short, only when unclear>"}. Use status unclear if the text is blurry, cropped, dark, unreadable, or contains no multiple-choice questions. Never guess.
""".trim()

    val LOCKED_CONTRACT_MULTI = """
Return ONLY JSON: {"status":"ok"|"unclear","answers":[{"q":<int>,"choice":"A"-"E","conf":"high"|"low","page":<photo number it was found on>}],"unreadable_photos":[<ints>],"reason":"<short, only when unclear>"}. Use status unclear only if no question is readable in any photo. Never guess.
""".trim()

    val LOCKED_CONTRACT_MULTI_WITH_REASONING = """
Return ONLY JSON: {"status":"ok"|"unclear","answers":[{"q":<int>,"choice":"A"-"E","conf":"high"|"low","reasoning":"<at most 15 words>","page":<photo number it was found on>}],"unreadable_photos":[<ints>],"reason":"<short, only when unclear>"}. Use status unclear only if no question is readable in any photo. Never guess.
""".trim()

    fun build(
        userInstruction: String,
        includeReasoning: Boolean = false,
        isRetry: Boolean = false,
        photoCount: Int = 1,
        multiPhotoInstruction: String? = null
    ): String {
        val instruction = if (userInstruction.isBlank()) DEFAULT_INSTRUCTION else userInstruction
        val contract = if (photoCount > 1) {
            if (includeReasoning) LOCKED_CONTRACT_MULTI_WITH_REASONING else LOCKED_CONTRACT_MULTI
        } else {
            if (includeReasoning) LOCKED_CONTRACT_WITH_REASONING else LOCKED_CONTRACT
        }

        val fullInstruction = if (photoCount > 1) {
            val multiPrefix = if (!multiPhotoInstruction.isNullOrBlank()) {
                multiPhotoInstruction.replace("{N}", photoCount.toString()).replace(" N ", " $photoCount ")
            } else {
                defaultMultiPhotoInstruction(photoCount)
            }
            "$multiPrefix\n\n$instruction"
        } else {
            instruction
        }

        var prompt = "$fullInstruction\n\n$contract"
        if (isRetry) {
            prompt += "\n\nReturn valid JSON only. Previous output failed to parse."
        }
        return prompt
    }
}
