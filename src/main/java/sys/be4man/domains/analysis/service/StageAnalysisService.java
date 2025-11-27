// 작성자 : 조윤상
package sys.be4man.domains.analysis.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sys.be4man.domains.analysis.model.entity.StageRun;
import sys.be4man.domains.analysis.model.type.ProblemType;
import sys.be4man.domains.analysis.repository.StageRunRepository;
import sys.be4man.domains.analysis.service.llm.LlmClient;
import sys.be4man.domains.analysis.service.llm.LlmClient.AnalysisResult;

@Slf4j
@Service
@RequiredArgsConstructor
public class StageAnalysisService {

    private final StageRunRepository stageRunRepository;
    private final LlmClient llmClient;

    // 한 번에 LLM에 보낼 로그 바이트 상한(모델에 맞춰 조절)
    private static final int MAX_PROMPT_BYTES = 50_000; // 대략 50KB

    // 필요 시 public으로 바꿔 컨트롤러/서비스에서 호출
    @Transactional
    public void analyzeFailedStages(List<StageRun> targets) {

        for (StageRun stage : targets) {
            try {
                String prompt = buildPrompt(stage.getStageName(), stage.getLog());
                AnalysisResult result = llmClient.summarizeAndSuggest(prompt);

                stage.updateAnalysis(result.summary(), result.solution(),
                        ProblemType.fromStringType(result.type()));
                // @Transactional로 영속 상태라 flush 시점에 반영됨
                log.info("[StageAnalysis] stageRunId={} 요약/해결책 업데이트 완료.", stage.getId());
            } catch (Exception e) {
                log.warn("[StageAnalysis] stageRunId={} 분석 실패: {}", stage.getId(), e.getMessage(),
                        e);
                // 실패는 전체 배치 중단 없이 다음 건 진행
            }
        }
    }

    private String buildPrompt(String stageName, String log) {

        return """
                You are a senior DevOps engineer. Read the Jenkins *stage* console log and:
                1) Point out the most likely root cause in  2 or 3 sentences.
                2) Propose How to fix the root cause in 2 or 3 sentences.
                3) Please classify the failure type into one of the following 5 types.
                    Build and Packaging Failures,
                    Automation test failed,
                    Deployment and execution errors,
                    Jenkins environment and configuration errors,
                    Others
                4) Answer above questions only Korean and clearly split three answers.
                5) Don't generate text except for answers like emphasis.
                6) Do not use bolding (**...**) anywhere in the response, even for emphasis or headings.
                            
                Stage Name: %s

                ---- Console Log  ----
                %s
                            
                            
                Please provide the response in the following JSON format only.
                {
                    "summary": 'problem summary content',
                    "solution": 'problem solution content',
                    "type": 'problem type content'
                }
                """.formatted(stageName, log);
    }

}