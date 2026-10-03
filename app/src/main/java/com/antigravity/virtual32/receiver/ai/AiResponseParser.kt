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
            
            val unreadableArray = json["unreadable_photos"]?.jsonArray
            val unreadablePhotos = unreadableArray?.mapNotNull { it.jsonPrimitive.intOrNull } ?: emptyList()

            val warnings = mutableListOf<String>()
            for (p in unreadablePhotos.sorted()) {
                warnings.add("Photo $p unreadable")
            }

            val answersArray = json["answers"]?.jsonArray
            if (answersArray == null) {
                if (status == "ok") return RawAiResult("unclear", emptyList(), reason ?: "No answers array", unreadablePhotos = unreadablePhotos, warnings = warnings)
                return RawAiResult(status, emptyList(), reason, unreadablePhotos = unreadablePhotos, warnings = warnings)
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
                val page = obj["page"]?.jsonPrimitive?.intOrNull ?: 1
                
                parsedAnswers.add(RawAnswer(q, choice, normalizedConf, reasoning, page))
            }

            // Dedupe by q: keep answer with conf high, else the later photo
            val deduplicated = parsedAnswers.groupBy { it.q }.map { (_, answers) ->
                answers.reduce { best, current ->
                    if (current.conf == "high" && best.conf != "high") {
                        current
                    } else if (current.conf != "high" && best.conf == "high") {
                        best
                    } else {
                        // Same confidence: keep later photo (higher page), or later in list if same page
                        if (current.page >= best.page) current else best
                    }
                }
            }

            // Sort by q
            val sortedAnswers = deduplicated.sortedBy { it.q }
            
            // Detect gaps between min and max q
            if (sortedAnswers.isNotEmpty()) {
                val minQ = sortedAnswers.first().q
                val maxQ = sortedAnswers.last().q
                val presentQs = sortedAnswers.map { it.q }.toSet()
                for (q in minQ..maxQ) {
                    if (q !in presentQs) {
                        warnings.add("Q$q missing")
                    }
                }
            }

            if (status == "ok" && sortedAnswers.isEmpty()) {
                return RawAiResult("unclear", emptyList(), "ok with zero valid answers", unreadablePhotos = unreadablePhotos, warnings = warnings)
            }
            
            return RawAiResult(status, sortedAnswers, reason, unreadablePhotos = unreadablePhotos, warnings = warnings)

        } catch (e: Exception) {
            return RawAiResult("error", isParseError = true, reason = "Failed to parse: ${e.message}")
        }
    }
}
