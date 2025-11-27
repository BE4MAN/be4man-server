// 작성자 : 조윤상
package sys.be4man.domains.statistics.repository.projection;

public record MonthBucket(
        Integer month,
        Long deployments,
        Long success,
        Long failed
) {}