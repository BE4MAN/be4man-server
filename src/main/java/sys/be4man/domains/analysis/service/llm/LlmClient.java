package sys.be4man.domains.analysis.service.llm;

public interface LlmClient {
    record AnalysisResult(String summary, String solution) {}
    AnalysisResult summarizeAndSuggest(String prompt);
}