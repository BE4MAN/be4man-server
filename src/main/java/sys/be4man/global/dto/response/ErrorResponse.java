package sys.be4man.global.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 표준화된 에러 응답 DTO
 * <p>
 * 클라이언트가 에러를 기계적으로 구분할 수 있도록 code 필드를 포함합니다.
 *
 * @param code      기계가 읽을 수 있는 에러 코드 (예: "ACCOUNT_NOT_FOUND", "INVALID_ACCESS_TOKEN")
 * @param status    HTTP 상태 코드 (예: 404, 401)
 * @param message   사람이 읽을 수 있는 에러 메시지 (예: "계정을 찾을 수 없습니다")
 * @param timestamp 에러 발생 시각
 * @param path      요청 API URI 경로
 */
public record ErrorResponse(
        String code,
        int status,
        String message,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp,
        String path
) {

    /**
     * BaseException으로부터 ErrorResponse 생성
     */
    public static ErrorResponse from(
            String code,
            int status,
            String message,
            String path
    ) {
        return new ErrorResponse(
                code,
                status,
                message,
                LocalDateTime.now(),
                path
        );
    }
}

