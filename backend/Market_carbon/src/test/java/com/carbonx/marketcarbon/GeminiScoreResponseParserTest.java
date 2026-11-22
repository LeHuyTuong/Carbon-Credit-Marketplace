package com.carbonx.marketcarbon;

import com.carbonx.marketcarbon.service.ai.GeminiScoreResponseParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests cho phần parse/sanitize JSON score response tách từ GeminiAiScoringService. */
class GeminiScoreResponseParserTest {

    private final GeminiScoreResponseParser parser = new GeminiScoreResponseParser();

    @Test
    @DisplayName("JSON hợp lệ: parse đúng toàn bộ field")
    void tryParseJson_validJson_parsesAllFields() {
        String text = """
                {"score": 7.5, "riskLevel": "MEDIUM", "fraudLikelihood": 0.3,
                 "version": "v2.5", "notes": "Overview ..."}
                """;

        Map<String, Object> parsed = parser.tryParseJson(text);

        assertThat(parsed).isNotNull();
        assertThat(((Number) parsed.get("score")).doubleValue()).isEqualTo(7.5);
        assertThat(parsed.get("riskLevel")).isEqualTo("MEDIUM");
        assertThat(((Number) parsed.get("fraudLikelihood")).doubleValue()).isEqualTo(0.3);
        assertThat(parsed.get("version")).isEqualTo("v2.5");
        assertThat(parsed.get("notes")).isEqualTo("Overview ...");
    }

    @Test
    @DisplayName("JSON nằm trong markdown code fence: bỏ wrapper rồi parse")
    void tryParseJson_markdownCodeFence_parsesInnerJson() {
        String text = "```json\n{\"score\": 8.25, \"riskLevel\": \"LOW\", \"fraudLikelihood\": 0.1}\n```";

        Map<String, Object> parsed = parser.tryParseJson(text);

        assertThat(((Number) parsed.get("score")).doubleValue()).isEqualTo(8.25);
        assertThat(parsed.get("riskLevel")).isEqualTo("LOW");
        assertThat(((Number) parsed.get("fraudLikelihood")).doubleValue()).isEqualTo(0.1);
    }

    @Test
    @DisplayName("JSON lỗi/null: trả về map rỗng thay vì ném exception")
    void tryParseJson_nullBlankOrGarbage_returnsEmptyMap() {
        assertThat(parser.tryParseJson(null)).isEmpty();
        assertThat(parser.tryParseJson("   \n  ")).isEmpty();
        assertThat(parser.tryParseJson("not a json object at all")).isEmpty();
    }

    @Test
    @DisplayName("JSON thiếu closing brace: fallback regex vẫn lấy được field")
    void tryParseJson_incompleteJson_regexFallback() {
        String text = "{\"score\": 6.25, \"riskLevel\": \"HIGH\", \"fraudLikelihood\": 0.9";

        Map<String, Object> parsed = parser.tryParseJson(text);

        assertThat(((Number) parsed.get("score")).doubleValue()).isEqualTo(6.25);
        assertThat(parsed.get("riskLevel")).isEqualTo("HIGH");
        assertThat(((Number) parsed.get("fraudLikelihood")).doubleValue()).isEqualTo(0.9);
    }

    @Test
    @DisplayName("extractText: lấy text trong envelope Vertex và strip markdown")
    void extractText_vertexEnvelope_stripsMarkdown() {
        Object raw = Map.of(
                "candidates", List.of(Map.of(
                        "content", Map.of(
                                "parts", List.of(Map.of(
                                        "text", "```json\n{\"score\": 9}\n```"
                                ))
                        )
                ))
        );

        assertThat(parser.extractText(raw)).isEqualTo("{\"score\": 9}");
    }

    @Test
    @DisplayName("extractText: raw sai cấu trúc hoặc thiếu text trả về '{}'")
    void extractText_malformedEnvelope_returnsEmptyObject() {
        assertThat(parser.extractText(null)).isEqualTo("{}");
        assertThat(parser.extractText("not a map")).isEqualTo("{}");
        assertThat(parser.extractText(Map.of("candidates", List.of()))).isEqualTo("{}");
        assertThat(parser.extractText(Map.of(
                "candidates", List.of(Map.of("content", Map.of("parts", List.of(Map.of()))))
        ))).isEqualTo("{}");
    }
}
