package com.antigravity.virtual32.receiver.ai

import kotlinx.serialization.json.*

object AiResponseParser {
    fun parse(rawText: String): RawAiResult {
        try {
            var text = rawText
            // Strip markdown fences if present
            if (text.contains("```")) {
                val start = text.indexOf("```json", ignoreCase = true)
                if (start != -1) {
                    text = text.substring(start + 7)
                } else {
                    val s2 = text.indexOf("```")
                    if (s2 != -1) text = text.substring(s2 + 3)
                }
                val end = text.lastIndexOf("```")
                if (end != -1 && end > 0) {
                    text = text.substring(0, end)
                }
            }

            // Extract first balanced JSON object
            val firstBrace = text.indexOf('{')
            val lastBrace = text.lastIndexOf('}')
            if (firstBrace == -1 || lastBrace == -1 || lastBrace < firstBrace) {
                return RawAiResult("error", isParseError = true, reason = "No JSON object found")
            }
            text = text.substring(firstBrace, lastBrace + 1)

            val json = Json.parseToJsonElement(text).jsonObject
            val status = json["status"]?.jsonPrimitive?.content ?: "unclear"
            val reason = json["reason"]?.jsonPrimitive?.content
            
            val answersArray = json["answers"]?.jsonArray
            if (answersArray == null) {
                if (status == "ok") return RawAiResult("unclear", emptyList(), reason ?: "No answers array")
                return RawAiResult(status, emptyList(), reason)
            }

            val parsedAnswers = mutableListOf<RawAnswer>()
            for (item in answersArray) {
                val obj = item.jsonObject
                val qStr = obj["q"]?.jsonPrimitive?.content ?: continue
                val q = qStr.toIntOrNull() ?: continue
                var choice = obj["choice"]?.jsonPrimitive?.content?.uppercase() ?: continue
                if (choice.length > 1) choice = choice.substring(0, 1)
                if (choice !in listOf("A", "B", "C", "D", "E")) continue
                
                val conf = obj["conf"]?.jsonPrimitive?.content?.lowercase() ?: "low"
                val normalizedConf = if (conf == "high") "high" else "low"
                val reasoning = obj["reasoning"]?.jsonPrimitive?.content
                
                parsedAnswers.add(RawAnswer(q, choice, normalizedConf, reasoning))
            }

            // Dedupe and sort by q
            val deduplicated = parsedAnswers.distinctBy { it.q }.sortedBy { it.q }
            
            if (status == "ok" && deduplicated.isEmpty()) {
                return RawAiResult("unclear", emptyList(), "ok with zero valid answers")
            }
            
            return RawAiResult(status, deduplicated, reason)

        } catch (e: Exception) {
            return RawAiResult("error", isParseError = true, reason = "Failed to parse: ${e.message}")
        }
    }
}
