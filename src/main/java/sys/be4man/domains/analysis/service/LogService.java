// 작성자 : 조윤상
package sys.be4man.domains.analysis.service;

import sys.be4man.domains.analysis.dto.response.JenkinsWebhooksResponseDto;

public interface LogService {

    /**
     * Jenkins 서버에서 특정 빌드의 콘솔 로그(TEXT)를 가져온다.
     * * @param jobName Jenkins Job 이름
     * @param buildNumber 빌드 번호
     * @return 콘솔 로그 전체 내용 (String)
     */
    String fetchConsoleLog(String jobName, String buildNumber);

    void fetchAndSaveLogAsync(JenkinsWebhooksResponseDto jenkinsData);

}
