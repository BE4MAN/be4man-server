// 작성자 : 조윤상
package sys.be4man.domains.statistics.repository.projection;

public record ProjectSuccessCount(Long projectId, String projectName, Long success, Long failed) {

}

