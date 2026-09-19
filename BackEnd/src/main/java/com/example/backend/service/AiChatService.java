package com.example.backend.service;

import com.example.backend.entity.AiKienThuc;
import com.example.backend.repository.AiKienThucRepository;
import com.example.backend.repository.SanPhamRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiChatService {

    @Value("${ai.ollama.url:http://localhost:11434}")
    private String ollamaUrl;

    @Value("${ai.ollama.model:llama3.2}")
    private String model;

    @Value("${ai.ollama.enabled:true}")
    private boolean enabled;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("http://localhost:11434")
            .build();

    private final AiKienThucRepository aiKienThucRepository;
    private final SanPhamRepository sanPhamRepository;

    public AiChatService(AiKienThucRepository aiKienThucRepository,
                          SanPhamRepository sanPhamRepository) {
        this.aiKienThucRepository = aiKienThucRepository;
        this.sanPhamRepository = sanPhamRepository;
    }

    /**
     * Tin nhắn chào mừng
     */
    public String getWelcomeMessage() {
        return "Xin chào! 👋 Mình là trợ lý AI của **SAOClub**. " +
                "Mình có thể giúp bạn:\n\n" +
                "🔹 Tìm hiểu về sản phẩm laptop, giá cả\n" +
                "🔹 Chính sách đổi trả, bảo hành\n" +
                "🔹 Hướng dẫn mua hàng, thanh toán\n" +
                "🔹 Các câu hỏi thường gặp\n\n" +
                "Bạn cần hỗ trợ gì hôm nay? Nếu cần tư vấn từ nhân viên, " +
                "bạn có thể gõ **\"chuyển nhân viên\"** nhé!";
    }

    /**
     * Trả lời câu hỏi
     */
    public String traLoi(String cauHoi, Long cuocTroChuyenId) {
        if (!enabled) {
            return fallbackResponse();
        }

        try {
            // 1. Tìm kiến thức liên quan
            String context = layContext(cauHoi);

            // 2. Gọi Ollama
            String prompt = buildPrompt(cauHoi, context);
            String traLoi = goiOllama(prompt);

            return traLoi;
        } catch (Exception e) {
            return fallbackResponse();
        }
    }

    /**
     * Lấy context từ cơ sở kiến thức
     */
    private String layContext(String cauHoi) {
        StringBuilder context = new StringBuilder();

        // Tìm kiếm theo keyword
        List<AiKienThuc> kienThuc = aiKienThucRepository.searchByKeyword(cauHoi);

        if (!kienThuc.isEmpty()) {
            context.append("## Thông tin từ cơ sở kiến thức:\n\n");
            for (AiKienThuc k : kienThuc) {
                context.append("**").append(k.getTieuDe()).append("**\n");
                context.append(k.getNoiDung()).append("\n\n");
            }
        } else {
            // Fallback: lấy FAQ chung
            List<AiKienThuc> faq = aiKienThucRepository.findGeneralKnowledge();
            if (!faq.isEmpty()) {
                context.append("## Câu hỏi thường gặp:\n\n");
                for (AiKienThuc k : faq) {
                    context.append("**").append(k.getTieuDe()).append("**\n");
                    context.append(k.getNoiDung()).append("\n\n");
                }
            }
        }

        return context.toString();
    }

    /**
     * Build prompt cho Ollama
     */
    private String buildPrompt(String cauHoi, String context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Bạn là trợ lý AI của cửa hàng laptop SAOClub. ");
        prompt.append("Nhiệm vụ của bạn:\n");
        prompt.append("1. Trả lời câu hỏi về sản phẩm, giá cả, chính sách\n");
        prompt.append("2. Gợi ý sản phẩm phù hợp nếu khách hỏi\n");
        prompt.append("3. Hướng dẫn mua hàng, thanh toán, vận chuyển\n");
        prompt.append("4. Nếu không biết câu trả lời chính xác, hãy:\n");
        prompt.append("   - Xin lỗi và nói rõ mình không có thông tin đó\n");
        prompt.append("   - Gợi ý khách liên hệ nhân viên bằng cách gõ 'chuyển nhân viên'\n\n");

        if (!context.isBlank()) {
            prompt.append("Dựa trên thông tin sau:\n").append(context).append("\n");
        }

        prompt.append("Câu hỏi từ khách: ").append(cauHoi).append("\n\n");
        prompt.append("Hãy trả lời ngắn gọn, thân thiện (dưới 300 từ). Sử dụng emoji phù hợp nếu cần.");

        return prompt.toString();
    }

    /**
     * Gọi Ollama API
     */
    private String goiOllama(String prompt) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("prompt", prompt);
        requestBody.put("stream", false);
        requestBody.put("options", Map.of(
                "temperature", 0.7,
                "num_predict", 500
        ));

        Map<String, Object> response = webClient.post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response != null && response.containsKey("response")) {
            return formatResponse((String) response.get("response"));
        }

        return fallbackResponse();
    }

    /**
     * Format response từ Ollama — loại bỏ dấu <> nếu có
     */
    private String formatResponse(String raw) {
        if (raw == null) return fallbackResponse();

        // Loại bỏ các tag không mong muốn
        raw = raw.trim();

        // Giới hạn độ dài
        if (raw.length() > 1500) {
            raw = raw.substring(0, 1500) + "...";
        }

        return raw;
    }

    /**
     * Fallback khi Ollama không hoạt động
     */
    private String fallbackResponse() {
        return "Xin lỗi, hiện tại mình chưa thể trả lời câu hỏi của bạn ngay lúc này. 😅\n\n" +
                "Bạn có thể:\n" +
                "📞 Gọi hotline: 1900.xxxx\n" +
                "💬 Gõ **\"chuyển nhân viên\"** để được nhân viên hỗ trợ trực tiếp\n" +
                "📧 Email: contact@saoclub.com";
    }

    /**
     * Kiểm tra Ollama có đang chạy không
     */
    public boolean isOllamaAvailable() {
        if (!enabled) return false;
        try {
            Map<String, Object> response = webClient.get()
                    .uri("/api/tags")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            return response != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Lấy danh sách models available
     */
    public List<String> getAvailableModels() {
        try {
            Map<String, Object> response = webClient.get()
                    .uri("/api/tags")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("models")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> models = (List<Map<String, Object>>) response.get("models");
                return models.stream()
                        .map(m -> (String) m.get("name"))
                        .collect(Collectors.toList());
            }
        } catch (Exception ignored) {
        }
        return Collections.emptyList();
    }
}
