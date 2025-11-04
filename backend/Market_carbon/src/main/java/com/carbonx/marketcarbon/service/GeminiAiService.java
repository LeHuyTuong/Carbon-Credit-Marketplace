package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.common.CreditStatus;
import com.carbonx.marketcarbon.config.AiVertexConfig;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.model.Company;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.repository.CarbonCreditRepository;
import com.carbonx.marketcarbon.repository.CompanyRepository;
import com.carbonx.marketcarbon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Casia AI Chat Service — Giao tiếp Vertex AI Gemini (Vertex hosted model)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiAiService {

    private final UserRepository userRepo;
    private final CompanyRepository companyRepo;
    private final CarbonCreditRepository creditRepo;

    private final AiVertexConfig cfg;
    private final WebClient vertexWebClient;

    public String answer(String question) {
        if (!cfg.isEnabled()) {
            return "Casia AI đang tạm tắt. Vui lòng thử lại sau.";
        }

        long start = System.currentTimeMillis();
        Long companyId = currentCompanyId();
        Company company = companyRepo.findById(companyId)
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));


        if (isGreeting(question)) {
            return "Chào " + safe(company.getCompanyName())
                    + "! Mình là Casia AI. Bạn muốn xem tồn kho, batch hay theo dự án/vintage nào?";
        }

        long available = creditRepo.sumAmountByCompany_IdAndStatus(companyId, CreditStatus.AVAILABLE);
        long sold = creditRepo.sumAmountByCompany_IdAndStatus(companyId, CreditStatus.SOLD);
        long retired = creditRepo.sumAmountByCompany_IdAndStatus(companyId, CreditStatus.RETIRED);

        String systemPrompt = """
Bạn là Casia AI — trợ lý ảo của nền tảng Casia Carbon Market.
- Trả lời bằng TIẾNG VIỆT, ngắn gọn, thân thiện, thực tế.
- Chỉ trình bày số liệu khi người dùng hỏi thông tin.
- Ưu tiên dùng dữ liệu trong phần DỮ LIỆU bên dưới; không bịa.
- Nếu câu hỏi là khái niệm chung (vd: 'tín chỉ carbon là gì'), giải thích ngắn gọn, không đưa số ngoài context.
""";

        String context = String.format("""
DỮ LIỆU:
- CÔNG TY ID: %d
- TÊN CÔNG TY: %s

TỒN KHO HIỆN TẠI (chỉ dùng khi user hỏi):
- AVAILABLE: %d
- SOLD: %d
- RETIRED: %d

THỜI ĐIỂM (UTC):
- %s
""",
                company.getId(),
                safe(company.getCompanyName()),
                available, sold, retired,
                OffsetDateTime.now(ZoneOffset.UTC)
        );

        String finalPrompt = systemPrompt
                + "\n---\n"
                + context
                + "\nCÂU HỎI NGƯỜI DÙNG: " + question + "\n"
                + "\nHƯỚNG DẪN:\n"
                + "- Chỉ nêu số liệu nếu người dùng hỏi về chúng.\n"
                + "- Nếu không có trong context, trả lời rằng bạn không có dữ liệu đó.\n";

        String response = callGeminiVertex(finalPrompt);

        long took = System.currentTimeMillis() - start;
        log.info("[Casia AI] Model={} | Took {} ms | Q='{}' | Response len={}",
                cfg.getModel(), took, question, response != null ? response.length() : 0);

        return response;
    }

    private String callGeminiVertex(String prompt) {
        String path = String.format(
                "/v1/projects/%s/locations/%s/publishers/google/models/%s:generateContent",
                cfg.getProjectId(), cfg.getLocation(), cfg.getModel()
        );

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", prompt))
                )),
                "generationConfig", Map.of(
                        "temperature", 0.3,
                        "maxOutputTokens", 1024
                )
        );

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = vertexWebClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, resp ->
                            resp.bodyToMono(String.class).flatMap(b -> {
                                log.error("[Vertex Gemini] {} body:\n{}", resp.statusCode(), b);
                                return Mono.error(new RuntimeException("Vertex " + resp.statusCode() + ": " + b));
                            })
                    )
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(60)); // ⏱️ timeout an toàn 60s

            String text = extractText(raw);
            if (text.isBlank()) {
                return "Xin lỗi, Casia AI chưa có câu trả lời phù hợp.";
            }

            log.debug("[Vertex Gemini raw response] {}", text);
            return text;

        } catch (Exception ex) {
            log.error("[Casia AI] Vertex Gemini error: {}", ex.getMessage(), ex);
            return "Xin lỗi, Casia AI đang bận: " + ex.getMessage();
        }
    }

    @SuppressWarnings("unchecked")
    private static String extractText(Object raw) {
        if (!(raw instanceof Map)) return "";
        Map<String, Object> map = (Map<String, Object>) raw;
        Object candidatesObj = map.get("candidates");
        if (!(candidatesObj instanceof List) || ((List<?>) candidatesObj).isEmpty()) return "";

        Object contentObj = ((Map<?, ?>) ((List<?>) candidatesObj).get(0)).get("content");
        if (!(contentObj instanceof Map)) return "";
        Object partsObj = ((Map<?, ?>) contentObj).get("parts");
        if (!(partsObj instanceof List) || ((List<?>) partsObj).isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        for (Object part : (List<?>) partsObj) {
            if (part instanceof Map && ((Map<?, ?>) part).get("text") != null) {
                sb.append(((Map<?, ?>) part).get("text")).append("\n");
            }
        }

        return sb.toString().trim();
    }

    private Long currentCompanyId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepo.findByEmail(email);
        if (user == null) throw new AppException(ErrorCode.UNAUTHORIZED);
        return companyRepo.findByUserId(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND))
                .getId();
    }

    private static boolean isGreeting(String q) {
        if (q == null) return false;
        String s = q.trim().toLowerCase(Locale.ROOT);
        return s.equals("hi") || s.equals("hello")
                || s.startsWith("xin chào") || s.startsWith("chào")
                || s.equals("hey") || s.equals("alo");
    }

    private static String safe(String s) {
        return Objects.toString(s, "Không rõ");
    }
}
