package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.common.CreditStatus;
import com.carbonx.marketcarbon.config.AiVertexConfig;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.model.Company;
import com.carbonx.marketcarbon.model.EmissionReport;
import com.carbonx.marketcarbon.model.Project;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.repository.*;
import com.carbonx.marketcarbon.service.credit.formula.CreditComputationResult;
import com.carbonx.marketcarbon.service.credit.formula.CreditFormula;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiAiService {

    private final UserRepository userRepo;
    private final CompanyRepository companyRepo;
    private final CarbonCreditRepository creditRepo;
    private final EmissionReportRepository emissionReportRepo;
    private final CreditFormula creditFormula;
    private final PriceAnalyticsService priceAnalytics;

    private final AiVertexConfig cfg;
    private final WebClient vertexWebClient;

    // ================= MAIN ENTRY =================
    public String answer(String question) {
        if (!cfg.isEnabled()) {
            return "Casia AI đang tạm tắt. Vui lòng thử lại sau.";
        }

        long start = System.currentTimeMillis();
        Long companyId = currentCompanyId();
        Company company = companyRepo.findById(companyId)
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));

        // ===== INTENT HANDLERS =====
        if (isGreeting(question)) {
            return "Chào " + safe(company.getCompanyName())
                    + "! Mình là Casia AI. Bạn muốn xem tồn kho, batch hay theo dự án/vintage nào?";
        }
        if (isFormulaQuestion(question)) {
            return handleFormulaExplain(companyId);
        }
        if (isPriceQuestion(question)) {
            return handlePriceQA(companyId, question);
        }

        // ===== NGỮ CẢNH DỮ LIỆU CHUNG =====
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

TỒN KHO HIỆN TẠI:
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

    // ================= INTENT CHECKERS =================

    private boolean isFormulaQuestion(String q) {
        if (q == null) return false;
        String s = q.toLowerCase(Locale.ROOT);
        return s.contains("công thức") || s.contains("cach tinh") || s.contains("cách tính")
                || s.contains("formula") || s.contains("tính tín chỉ") || s.contains("tinh tin chi")
                || s.contains("phương pháp") || s.contains("phuong phap");
    }

    private boolean isPriceQuestion(String q) {
        if (q == null) return false;
        String s = q.toLowerCase(Locale.ROOT);
        return s.contains("giá") || s.contains("gia")
                || s.contains("price") || s.contains("trung bình") || s.contains("trung binh")
                || s.contains("cao nhất") || s.contains("cao nhat")
                || s.contains("thấp nhất") || s.contains("thap nhat")
                || s.contains("highest") || s.contains("lowest") || s.contains("average");
    }

    // ================= HANDLERS =================

    private String handleFormulaExplain(Long companyId) {
        Optional<EmissionReport> opt = emissionReportRepo.findTopBySeller_IdOrderByCreatedAtDesc(companyId);
        if (opt.isEmpty() || opt.get().getProject() == null) {
            return """
Công thức tính tín chỉ (mặc định của dự án):

1) Ưu tiên lấy CO₂ (kg) từ báo cáo: totalCo2; nếu không có thì CO₂ (kg) = totalEnergy (kWh) × EmissionFactor (kg/kWh).
2) Quy đổi sang tCO₂e: t = CO₂(kg) / 1000 (giữ 6 chữ số để tính tiếp).
3) Áp dụng các hệ số trừ của dự án: multiplier = 1 - buffer - uncertainty - leakage.
4) tCO₂e sau điều chỉnh: net = t × multiplier (làm tròn xuống 3 chữ số).
5) Tín chỉ nhận = floor(net); phần dư = net - floor(net).

Bạn có thể gửi “tính thử theo báo cáo gần nhất” để mình minh họa bằng số cụ thể.
""";
        }

        EmissionReport report = opt.get();
        Project project = report.getProject();

        CreditComputationResult r = creditFormula.compute(report, project);
        String ef = fmtBD(project.getEmissionFactorKgPerKwh());
        String bf = pct(project.getBufferReservePct());
        String uc = pct(project.getUncertaintyPct());
        String lk = pct(project.getLeakagePct());

        String totalEnergy = fmtBD(report.getTotalEnergy());
        String totalCo2 = fmtBD(report.getTotalCo2());
        String netTStr = fmtBD(r.getTotalTco2e());
        int credits = r.getCreditsCount();
        String residual = fmtBD(r.getResidualTco2e());

        return String.format("""
Công thức của dự án (đang áp dụng) + minh họa báo cáo gần nhất:

1) CO₂(kg):
   - Nếu có totalCo2 trong báo cáo: dùng trực tiếp (hiện: %s kg).
   - Nếu không có: CO₂(kg) = totalEnergy × EF. Ở đây totalEnergy = %s kWh, EF = %s kg/kWh.
2) tCO₂e = CO₂(kg) / 1000 (giữ 6 chữ số).
3) Hệ số trừ dự án:
   - Buffer: %s | Uncertainty: %s | Leakage: %s
   => multiplier = max(0, 1 - buffer - uncertainty - leakage).
4) tCO₂e sau điều chỉnh (làm tròn xuống 3 chữ số): %s tCO₂e.
5) Tín chỉ nhận = floor(net) = %d; phần dư = %s.

Bạn có thể hỏi: “giá trung bình 30 ngày”, “giá cao nhất 7 ngày”, “giá thấp nhất 3 tháng”,…
""",
                totalCo2, totalEnergy, ef, bf, uc, lk, netTStr, credits, residual);
    }

    private String handlePriceQA(Long companyId, String q) {
        TimeRange tr = parseRange(q);
        PriceAnalyticsService.PriceStats st = priceAnalytics.statsForCompany(companyId, tr.from(), tr.to());

        if (st.avg() == null && st.min() == null && st.max() == null) {
            return "Chưa có giao dịch hoàn tất trong " + tr.label() + " để thống kê giá.";
        }

        String avg = st.avg() != null ? money(st.avg()) : "—";
        String min = st.min() != null ? money(st.min()) : "—";
        String max = st.max() != null ? money(st.max()) : "—";

        String s = q.toLowerCase(Locale.ROOT);
        if (s.contains("trung bình") || s.contains("trung binh") || s.contains("average"))
            return "Giá trung bình " + tr.label() + ": " + avg + ".";
        if (s.contains("cao nhất") || s.contains("cao nhat") || s.contains("highest") || s.contains("max"))
            return "Giá cao nhất " + tr.label() + ": " + max + ".";
        if (s.contains("thấp nhất") || s.contains("thap nhat") || s.contains("lowest") || s.contains("min"))
            return "Giá thấp nhất " + tr.label() + ": " + min + ".";
        return "Thống kê giá " + tr.label() + ": Trung bình " + avg + " | Thấp nhất " + min + " | Cao nhất " + max + ".";
    }

    // ================= UTILS =================

    private String callGeminiVertex(String prompt) {
        String path = String.format(
                "/v1/projects/%s/locations/%s/publishers/google/models/%s:generateContent",
                cfg.getProjectId(), cfg.getLocation(), cfg.getModel()
        );
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("temperature", 0.3, "maxOutputTokens", 1024)
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
                    .block(Duration.ofSeconds(60));

            String text = extractText(raw);
            if (text.isBlank()) return "Xin lỗi, Casia AI chưa có câu trả lời phù hợp.";
            return text.trim();
        } catch (Exception ex) {
            log.error("[Casia AI] Vertex Gemini error: {}", ex.getMessage(), ex);
            return "Xin lỗi, Casia AI đang bận: " + ex.getMessage();
        }
    }

    @SuppressWarnings("unchecked")
    private static String extractText(Object raw) {
        if (!(raw instanceof Map)) return "";
        Map<String, Object> map = (Map<String, Object>) raw;
        Object candidates = map.get("candidates");
        if (!(candidates instanceof List) || ((List<?>) candidates).isEmpty()) return "";
        Object content = ((Map<?, ?>) ((List<?>) candidates).get(0)).get("content");
        if (!(content instanceof Map)) return "";
        Object parts = ((Map<?, ?>) content).get("parts");
        if (!(parts instanceof List) || ((List<?>) parts).isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Object p : (List<?>) parts) {
            if (p instanceof Map && ((Map<?, ?>) p).get("text") != null)
                sb.append(((Map<?, ?>) p).get("text")).append("\n");
        }
        return sb.toString();
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
        return s.equals("hi") || s.equals("hello") || s.startsWith("xin chào") || s.startsWith("chào")
                || s.equals("hey") || s.equals("alo");
    }

    private static String safe(String s) {
        return Objects.toString(s, "Không rõ");
    }

    private record TimeRange(LocalDateTime from, LocalDateTime to, String label) {}

    private TimeRange parseRange(String q) {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        LocalDateTime to = now.toLocalDateTime();
        LocalDateTime from = now.minusDays(90).toLocalDateTime();
        String label = "90 ngày gần đây";
        if (q == null) return new TimeRange(from, to, label);
        String s = q.toLowerCase(Locale.ROOT);

        var d = java.util.regex.Pattern.compile("(\\d{1,3})\\s*(ngày|day|days|d)").matcher(s);
        if (d.find()) {
            int n = Integer.parseInt(d.group(1));
            return new TimeRange(now.minusDays(n).toLocalDateTime(), to, n + " ngày gần đây");
        }

        var m = java.util.regex.Pattern.compile("(\\d{1,2})\\s*(tháng|month|months|m)").matcher(s);
        if (m.find()) {
            int n = Integer.parseInt(m.group(1));
            return new TimeRange(now.minusMonths(n).toLocalDateTime(), to, n + " tháng gần đây");
        }
        return new TimeRange(from, to, label);
    }

    // ================= FORMATTERS (NULL-SAFE) =================

    private static String money(BigDecimal v) {
        if (v == null) return "—";
        try {
            DecimalFormat df = new DecimalFormat("#,##0.########");
            return df.format(v) + " /tín chỉ";
        } catch (Exception e) {
            return "—";
        }
    }

    private static String pct(BigDecimal v) {
        if (v == null) return "0%";
        try {
            BigDecimal safe = Optional.ofNullable(v).orElse(BigDecimal.ZERO);
            BigDecimal percent = safe.multiply(BigDecimal.valueOf(100));
            return percent.stripTrailingZeros().toPlainString() + "%";
        } catch (Exception e) {
            return "0%";
        }
    }

    private static String fmtBD(BigDecimal v) {
        if (v == null) return "—";
        try {
            if (BigDecimal.ZERO.compareTo(v) == 0) return "0";
            return v.stripTrailingZeros().toPlainString();
        } catch (Exception e) {
            return "—";
        }
    }
}
