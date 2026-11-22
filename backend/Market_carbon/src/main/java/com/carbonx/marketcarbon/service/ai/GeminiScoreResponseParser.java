package com.carbonx.marketcarbon.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts and parses the JSON score payload returned by Vertex Gemini.
 *
 * <p>Keeps the tolerant parsing of the original inline implementation:
 * markdown wrappers are stripped, unbalanced braces are repaired and a
 * regex fallback recovers individual fields from incomplete JSON.
 */
@Component
@Slf4j
public class GeminiScoreResponseParser {

    private static final Pattern JSON_BLOCK = Pattern.compile("\\{[\\s\\S]*\\}");
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern SCORE_PATTERN = Pattern.compile("\"score\"\\s*:\\s*([0-9.]+)");
    private static final Pattern RISK_PATTERN = Pattern.compile("\"riskLevel\"\\s*:\\s*\"(\\w+)\"");
    private static final Pattern FRAUD_PATTERN = Pattern.compile("\"fraudLikelihood\"\\s*:\\s*([0-9.]+)");

    @SuppressWarnings("unchecked")
    public String extractText(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            return "{}";
        }

        Object candidatesObj = map.get("candidates");
        if (!(candidatesObj instanceof List<?> candidates) || candidates.isEmpty()) {
            return "{}";
        }

        Object contentObj = ((Map<?, ?>) candidates.get(0)).get("content");
        if (!(contentObj instanceof Map<?, ?> content)) {
            return "{}";
        }

        Object partsObj = content.get("parts");
        if (!(partsObj instanceof List<?> parts) || parts.isEmpty()) {
            return "{}";
        }

        Object text = ((Map<?, ?>) parts.get(0)).get("text");
        if (text == null) {
            return "{}";
        }

        String s = text.toString().trim();
        // Remove possible markdown wrappers
        s = s.replaceAll("(?i)```json", "")
                .replaceAll("```", "")
                .replaceAll("^json", "")
                .trim();
        return s;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> tryParseJson(String text) {
        if (text == null || text.isBlank()) {
            return Map.of();
        }

        Matcher m = JSON_BLOCK.matcher(text);
        String candidate = m.find() ? m.group() : text;

        long open = candidate.chars().filter(ch -> ch == '{').count();
        long close = candidate.chars().filter(ch -> ch == '}').count();
        if (open > close) {
            candidate += "}".repeat((int) (open - close));
        }

        try {
            return MAPPER.readValue(candidate, Map.class);
        } catch (Exception e) {
            Map<String, Object> fallback = new HashMap<>();
            try {
                Matcher scoreM = SCORE_PATTERN.matcher(candidate);
                if (scoreM.find()) {
                    fallback.put("score", Double.parseDouble(scoreM.group(1)));
                }

                Matcher riskM = RISK_PATTERN.matcher(candidate);
                if (riskM.find()) {
                    fallback.put("riskLevel", riskM.group(1));
                }

                Matcher fraudM = FRAUD_PATTERN.matcher(candidate);
                if (fraudM.find()) {
                    fallback.put("fraudLikelihood", Double.parseDouble(fraudM.group(1)));
                }
            } catch (Exception ignored) {
            }
            log.warn("[AI] Fallback parser used for incomplete JSON:\n{}", candidate);
            return fallback;
        }
    }
}
