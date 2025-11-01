package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.common.CreditStatus;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.model.Company;
import com.carbonx.marketcarbon.model.User;
import com.carbonx.marketcarbon.repository.CarbonCreditRepository;
import com.carbonx.marketcarbon.repository.CompanyRepository;
import com.carbonx.marketcarbon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;


@Service
@RequiredArgsConstructor
public class GeminiAiService {

    @Value("${ai.gemini.api-key:}")
    private String apiKey;
    @Value("${ai.gemini.model:gemini-1.5-flash}")
    private String model;
    @Value("${ai.gemini.timeout-ms:60000}")
    private int timeoutMs;
    @Value("${ai.gemini.enabled:true}")
    private boolean enabled;

    private final UserRepository userRepo;
    private final CompanyRepository companyRepo;
    private final CarbonCreditRepository creditRepo;

    private OkHttpClient http;
    private OkHttpClient http() {
        if (http == null) {
            http = new OkHttpClient.Builder()
                    .callTimeout(Duration.ofMillis(timeoutMs))
                    .connectTimeout(Duration.ofSeconds(10))
                    .readTimeout(Duration.ofSeconds(60))
                    .build();
        }
        return http;
    }

    public String answer(String question) throws IOException {
        if (!enabled) return "Casia AI đang tạm tắt. Vui lòng thử lại sau.";
        if (apiKey == null || apiKey.isBlank()) throw new IllegalStateException("Thiếu cấu hình ai.gemini.api-key");

        Long companyId = currentCompanyId();
        Company company = companyRepo.findById(companyId)
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));

        //  Nếu chỉ là chào hỏi → trả lời ngay, KHÔNG show số liệu, KHÔNG gọi Gemini
        if (isGreeting(question)) {
            return "Chào " + safe(company.getCompanyName())
                    + "! Mình là Casia AI. Bạn muốn xem tồn kho, batch hay theo dự án/vintage nào?";
        }

        // Chỉ load số liệu khi user thật sự hỏi thông tin
        long available = creditRepo.sumAmountByCompany_IdAndStatus(companyId, CreditStatus.AVAILABLE);
        long sold      = creditRepo.sumAmountByCompany_IdAndStatus(companyId, CreditStatus.SOLD);
        long retired   = creditRepo.sumAmountByCompany_IdAndStatus(companyId, CreditStatus.RETIRED);

        String system = ""
                + "Bạn là Casia AI — trợ lý ảo của nền tảng Casia Carbon Market.\n"
                + "- Trả lời TIẾNG VIỆT, ngắn gọn, thân thiện, thực tế.\n"
                + "- Chỉ trình bày số liệu khi người dùng HỎI THÔNG TIN. Không tự ý cung cấp số liệu khi họ chỉ chào hỏi.\n"
                + "- Ưu tiên dùng dữ liệu trong phần DỮ LIỆU bên dưới; không bịa.\n"
                + "- Nếu câu hỏi là khái niệm chung (vd: 'tín chỉ carbon là gì'), giải thích ngắn gọn, không đưa số ngoài context.\n";

        // Context đơn giản, nối chuỗi (không dùng % format)
        String context = ""
                + "CÔNG TY\n"
                + "- ID: " + company.getId() + "\n"
                + "- Tên: " + safe(company.getCompanyName()) + "\n"
                + "TỒN KHO HIỆN TẠI (chỉ dùng khi user hỏi)\n"
                + "- AVAILABLE: " + available + "\n"
                + "- SOLD: " + sold + "\n"
                + "- RETIRED: " + retired + "\n\n"
                + "THỜI ĐIỂM (UTC)\n"
                + "- " + OffsetDateTime.now(ZoneOffset.UTC) + "\n";

        String finalPrompt = system
                + "\n---\nDỮ LIỆU:\n" + context
                + "\nCÂU HỎI: " + question + "\n"
                + "\nHƯỚNG DẪN TRẢ LỜI:\n"
                + "- Chỉ nêu số liệu (AVAILABLE/SOLD/RETIRED/…) khi CÂU HỎI yêu cầu thông tin đó.\n"
                + "- Nếu không có trong context, nêu rõ không có dữ liệu.\n";

        return callGemini(finalPrompt);
    }

    private String callGemini(String prompt) throws IOException {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + model + ":generateContent?key=" + apiKey;

        JSONObject req = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject content = new JSONObject();
        JSONArray parts = new JSONArray();
        parts.put(new JSONObject().put("text", prompt));
        content.put("parts", parts);
        contents.put(content);
        req.put("contents", contents);

        RequestBody body = RequestBody.create(req.toString(),
                MediaType.get("application/json; charset=utf-8"));
        Request httpReq = new Request.Builder().url(url).post(body).build();

        try (Response resp = http().newCall(httpReq).execute()) {
            if (!resp.isSuccessful()) return "Xin lỗi, Casia AI đang bận (HTTP " + resp.code() + ").";
            String json = resp.body() != null ? resp.body().string() : "{}";
            JSONObject obj = new JSONObject(json);
            JSONArray candidates = obj.optJSONArray("candidates");
            if (candidates != null && candidates.length() > 0) {
                return candidates.getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .optString("text", "Xin lỗi, hiện chưa có câu trả lời phù hợp.");
            }
            return "Xin lỗi, hiện chưa có câu trả lời phù hợp.";
        }
    }

    private Long currentCompanyId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepo.findByEmail(email);
        if (user == null) throw new AppException(ErrorCode.UNAUTHORIZED);
        return companyRepo.findByUserId(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND))
                .getId();
    }

    private static String safe(String s) {
        return Objects.toString(s, "Không rõ");
    }

    private static boolean isGreeting(String q) {
        if (q == null) return false;
        String s = q.trim().toLowerCase(Locale.ROOT);
        return s.equals("hi") || s.equals("hello")
                || s.startsWith("xin chào") || s.startsWith("chào")
                || s.equals("hey") || s.equals("alo");
    }
}