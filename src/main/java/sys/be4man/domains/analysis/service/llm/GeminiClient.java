package sys.be4man.domains.analysis.service.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.types.GenerateContentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.google.genai.Client;
import sys.be4man.domains.analysis.service.llm.LlmClient;

@Component
@RequiredArgsConstructor
public class GeminiClient implements LlmClient {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Override
    public AnalysisResult summarizeAndSuggest(String prompt) {
        Client client = Client.builder().apiKey(geminiApiKey).build();

        GenerateContentResponse response =
                client.models.generateContent(
                        "gemini-2.5-flash",
                        prompt,
                        null);

        String jsonResponse = response.text();

        ObjectMapper objectMapper = new ObjectMapper();

        try {
            // JSON 문자열을 AnalysisResult 클래스의 인스턴스로 변환
            // 인코딩 문제(유니코드 이스케이프 등)는 Jackson이 자동으로 처리합니다.
            AnalysisResult analysisResult = objectMapper.readValue(jsonResponse,
                    AnalysisResult.class);

            // AnalysisResult 객체를 반환
            return analysisResult;

        } catch (Exception e) {
            // JSON 파싱 실패 시 예외 처리
            System.err.println("JSON 파싱 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            // 오류 발생 시 빈 객체 또는 적절한 오류 객체를 반환하도록 처리
            return new AnalysisResult("JSON 파싱 실패", "로그를 확인하고 DTO와 JSON 응답 구조를 검토하세요.");
        }
    }
}