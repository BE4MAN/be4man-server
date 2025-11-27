// 작성자 : 이원석
package sys.be4man.global.exception.type;

/**
 * 예외 타입 인터페이스 모든 도메인별 예외 타입은 이 인터페이스를 구현해야 합니다.
 */
public interface ExceptionType {

    /**
     * 기계가 읽을 수 있는 에러 코드 (enum 이름)
     *
     * @return enum 이름 (예: "ACCOUNT_NOT_FOUND")
     */
    String getName();

    /**
     * 사람이 읽을 수 있는 에러 메시지
     *
     * @return 한글 메시지 (예: "계정을 찾을 수 없습니다")
     */
    String getMessage();

}
