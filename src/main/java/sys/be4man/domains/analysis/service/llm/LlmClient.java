// 작성자 : 조윤상
package sys.be4man.domains.analysis.service.llm;

public interface LlmClient {
    record AnalysisResult(String summary, String solution, String type) {}
    AnalysisResult summarizeAndSuggest(String prompt);
}