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

    // ==============================================
    //                MAIN ENTRY
    // ==============================================
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

        if (isHowSystemWork(question)) {
            return handleHowSystemWork();
        }

        if (isMarketPriceQuestion(question)) {
            return handleMarketPriceQA(question);
        }

        if (isPriceQuestion(question)) {
            return handlePriceQA(companyId, question);
        }

        if (isVolumeQuestion(question)) {
            return handleVolumeQA();
        }

        // ===== NGỮ CẢNH DỮ LIỆU CHUNG =====
        long available = creditRepo.sumAmountByCompany_IdAndStatus(companyId, CreditStatus.AVAILABLE);
        long sold = creditRepo.sumAmountByCompany_IdAndStatus(companyId, CreditStatus.SOLD);
        long retired = creditRepo.sumAmountByCompany_IdAndStatus(companyId, CreditStatus.RETIRED);

        String systemPrompt = """
Bạn là Casia AI — trợ lý ảo của nền tảng Casia Carbon Market.

NHIỆM VỤ CHÍNH:
- Trả lời rõ ràng, đầy đủ, dễ hiểu.
- Ưu tiên sử dụng dữ liệu thật được cung cấp trong CONTEXT.
- Không bịa số liệu, không tạo dữ liệu thị trường nếu không có trong context.
- Khi người dùng hỏi khái niệm (ví dụ: tín chỉ carbon là gì), trả lời chi tiết, dễ hiểu.
- Khi người dùng hỏi về giá cả, số lượng, tồn kho hoặc số liệu → dùng đúng dữ liệu context hoặc kết quả tính toán từ backend.
- Không trả lời ngắn gọn trừ khi câu hỏi yêu cầu.
- Luôn giữ giọng văn thân thiện, chuyên nghiệp, giống trợ lý cá nhân.

QUY TẮC KHI TRẢ LỜI:
1. Nếu câu hỏi liên quan đến số liệu mà không có dữ liệu trong context → nêu rõ rằng bạn không có dữ liệu đó.
2. Nếu câu hỏi mơ hồ → yêu cầu người dùng nói rõ hơn.
3. Khi người dùng muốn giải thích quy trình/hệ thống → trình bày theo dạng từng bước chi tiết.
4. Khi trả lời về giá thị trường hoặc giá công ty → chỉ dùng kết quả backend trả về.
5. Tránh lặp lại nguyên câu hỏi của người dùng.
6. Không chèn ký hiệu lạ, không markdown quá phức tạp — giữ bố cục gọn gàng.

MỤC TIÊU:
- Hỗ trợ doanh nghiệp hiểu rõ tín chỉ carbon của họ.
- Giải thích cách hoạt động của hệ thống Casia Carbon Market.
- Cung cấp thông tin minh bạch, tin cậy và thực tế.
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

    // ==============================================
    //                INTENT DETECTION
    // ==============================================

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

    private boolean isMarketPriceQuestion(String q) {
        if (q == null) return false;
        String s = q.toLowerCase();
        return s.contains("giá sàn")
                || s.contains("giá thị trường")
                || s.contains("giá toàn sàn")
                || s.contains("giá chung")
                || s.contains("market price")
                || s.contains("giá cả thị trường");
    }

    private boolean isVolumeQuestion(String q) {
        if (q == null) return false;
        String s = q.toLowerCase();
        return s.contains("lượng tín chỉ")
                || s.contains("khối lượng tín chỉ")
                || s.contains("volume tín chỉ")
                || s.contains("đang giao dịch")
                || s.contains("trên sàn");
    }

    private boolean isHowSystemWork(String q) {
        if (q == null) return false;
        String s = q.toLowerCase();
        return s.contains("hoạt động như thế nào")
                || s.contains("hoat dong nhu the nao")
                || s.contains("cách hoạt động")
                || s.contains("cach hoat dong")
                || s.contains("cơ chế")
                || s.contains("co che")
                || s.contains("marketcarbon hoạt động")
                || s.contains("marketcarbon hoat dong")
                || s.contains("how it works")
                || s.contains("how does");
    }


    // ==============================================
    //              HANDLER: FORMULA
    // ==============================================

    private String handleFormulaExplain(Long companyId) {
        Optional<EmissionReport> opt = emissionReportRepo
                .findTopBySeller_IdOrderByCreatedAtDesc(companyId);

        if (opt.isEmpty() || opt.get().getProject() == null) {
            return """
Công thức tính tín chỉ (mặc định của dự án):

1) Ưu tiên lấy CO₂ (kg) từ báo cáo: totalCo2; nếu không có thì CO₂ (kg) = totalEnergy (kWh) × EmissionFactor (kg/kWh).
2) Quy đổi sang tCO₂e: t = CO₂(kg) / 1000.
3) Áp dụng các hệ số trừ: buffer, uncertainty, leakage.
4) tCO₂e sau điều chỉnh = t × multiplier.
5) Tín chỉ nhận = floor(net); phần dư = net - floor(net).
""";
        }

        EmissionReport report = opt.get();
        Project project = report.getProject();

        CreditComputationResult r = creditFormula.compute(report, project);

        return String.format("""
Công thức tính tín chỉ + minh họa báo cáo gần nhất:

1) CO₂(kg): %s
2) totalEnergy: %s kWh
3) EF: %s kg/kWh
4) tCO₂e sau điều chỉnh: %s
5) Tín chỉ nhận: %d; phần dư: %s
""",
                fmtBD(report.getTotalCo2()),
                fmtBD(report.getTotalEnergy()),
                fmtBD(project.getEmissionFactorKgPerKwh()),
                fmtBD(r.getTotalTco2e()),
                r.getCreditsCount(),
                fmtBD(r.getResidualTco2e())
        );
    }

    // ==============================================
    //              HANDLER: COMPANY PRICE
    // ==============================================
    private String handlePriceQA(Long companyId, String q) {
        TimeRange tr = parseRange(q);
        var st = priceAnalytics.statsForCompany(companyId, tr.from(), tr.to());

        if (st.avg() == null && st.min() == null && st.max() == null) {
            return "Chưa có giao dịch hoàn tất trong " + tr.label() + ".";
        }

        String avg = money(st.avg());
        String min = money(st.min());
        String max = money(st.max());

        String s = q.toLowerCase();
        if (s.contains("trung bình")) return "Giá trung bình " + tr.label() + ": " + avg + ".";
        if (s.contains("cao nhất")) return "Giá cao nhất " + tr.label() + ": " + max + ".";
        if (s.contains("thấp nhất")) return "Giá thấp nhất " + tr.label() + ": " + min + ".";

        return "Thống kê giá " + tr.label()
                + ": Trung bình " + avg
                + " | Thấp nhất " + min
                + " | Cao nhất " + max + ".";
    }

    // ==============================================
    //         HANDLER: MARKET PRICE (NEW)
    // ==============================================
    private String handleMarketPriceQA(String q) {
        TimeRange tr = parseRange(q);
        var st = priceAnalytics.statsForMarket(tr.from(), tr.to());

        if (st.avg() == null && st.min() == null && st.max() == null) {
            return "Chưa có giao dịch toàn thị trường trong " + tr.label() + ".";
        }

        String avg = money(st.avg());
        String min = money(st.min());
        String max = money(st.max());

        String s = q.toLowerCase();

        if (s.contains("trung bình"))
            return "Giá trung bình toàn thị trường " + tr.label() + ": " + avg + ".";

        if (s.contains("cao nhất"))
            return "Giá cao nhất toàn thị trường " + tr.label() + ": " + max + ".";

        if (s.contains("thấp nhất"))
            return "Giá thấp nhất toàn thị trường " + tr.label() + ": " + min + ".";

        return "Thống kê giá toàn thị trường " + tr.label()
                + ": Trung bình " + avg
                + " | Thấp nhất " + min
                + " | Cao nhất " + max + ".";
    }

    // ==============================================
    //    HANDLER: MARKET VOLUME RANKING (NEW)
    // ==============================================
    private String handleVolumeQA() {
        List<Company> all = companyRepo.findAll();
        if (all.isEmpty()) {
            return "Hiện chưa có dữ liệu công ty nào trên sàn.";
        }

        List<VolumeStat> stats = new ArrayList<>();
        for (Company c : all) {
            long total = creditRepo.sumAmountByCompany_Id(c.getId());
            stats.add(new VolumeStat(c.getCompanyName(), total));
        }

        stats.sort(Comparator.comparingLong(v -> v.volume));

        VolumeStat lowest = stats.get(0);
        VolumeStat highest = stats.get(stats.size() - 1);

        return """
Trên sàn hiện tại:
- Công ty có **lượng tín chỉ thấp nhất**: %s.
- Công ty có **lượng tín chỉ cao nhất**: %s.
""".formatted(
                lowest.name,
                highest.name
        );
    }

    private static class VolumeStat {
        String name;
        long volume;

        VolumeStat(String n, long v) {
            this.name = n;
            this.volume = v;
        }
    }

    private String handleHowSystemWork() {
        return """
Hệ thống Casia Carbon Market hoạt động như một sàn giao dịch tín chỉ carbon:

1) **Dự án tạo tín chỉ**
   - Doanh nghiệp/dự án gửi báo cáo giảm phát thải.
   - Casia AI và CVA thẩm định, tính toán tín chỉ.

2) **Niêm yết bán tín chỉ**
   - Tín chỉ đủ điều kiện được đưa lên Marketplace.
   - Người bán đặt giá, số lượng, vintage, dự án nguồn gốc.

3) **Giao dịch**
   - Người mua đặt lệnh.
   - Hệ thống tự động khớp lệnh và tạo giao dịch.

4) **Thanh toán**
   - Casia tính phí nền tảng.
   - Tiền được chuyển về tài khoản người bán.

5) **Theo dõi & báo cáo**
   - Doanh nghiệp xem tồn kho, giao dịch, tín chỉ đã bán hoặc đã retire.
   - Casia cung cấp dữ liệu minh bạch, truy xuất nguồn gốc.

Nếu bạn muốn giải thích chi tiết từng bước, hãy nói: “mô tả chi tiết”.
""";
    }


    // ==============================================
    //                  VERTEX AI CALL
    // ==============================================
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
                    .block(Duration.ofSeconds(60));

            String text = extractText(raw);
            if (text.isBlank()) return "Xin lỗi, Casia AI chưa có câu trả lời phù hợp.";
            return text.trim();

        } catch (Exception ex) {
            log.error("[Casia AI] Vertex Gemini error: {}", ex.getMessage(), ex);
            return "Xin lỗi, Casia AI đang bận: " + ex.getMessage();
        }
    }

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

    // ==============================================
    //                  HELPERS
    // ==============================================
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
        String s = q.trim().toLowerCase();
        return s.equals("hi") || s.equals("hello")
                || s.startsWith("xin chào") || s.startsWith("chào")
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

        String s = q.toLowerCase();

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
            BigDecimal percent = v.multiply(BigDecimal.valueOf(100));
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
