package sys.be4man.global.exception.handler;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import sys.be4man.global.exception.BaseException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 4xx 커스텀 예외
     */
    @ExceptionHandler(BaseException.class)
    public ProblemDetail handleBaseException(final BaseException e) {
        log.warn("[{}] : {}", e.getClass(), e.getMessage());

        return ProblemDetail.forStatusAndDetail(e.getHttpStatus(), e.getMessage());
    }

    /**
     * 5xx 서버 에러. 예외처리되지 않은 케이스
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleException(final Exception e) {
        log.error("[{}] : {}", e.getClass(), e.getMessage());
        e.printStackTrace();

        return ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                e.getMessage()
        );
    }

    /**
     * 400 클라이언트 예외
     */
    // 요청 검증 실패 예외 (@Valid 어노테이션 실패. @RequestBody)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValidException(final MethodArgumentNotValidException e) {
        final List<FieldError> errors = e.getBindingResult().getFieldErrors();
        log.warn("[{}] {}", e.getClass(), errors);

        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                errors.toString()
        );
    }

    // 요청 파라미터 검증 실패 예외 (@Validated 어노테이션 실패. @RequestParam, @PathVariable)
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail constraintViolationException(final ConstraintViolationException e) {
        final Set<ConstraintViolation<?>> errors = e.getConstraintViolations();
        log.warn("[{}] {}", e.getClass(), errors);

        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                errors.toString()
        );
    }

    // 요청 파라미터 타입 변환 실패 (String을 Integer로 변환 실패 등)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleMethodArgumentTypeMismatchException(final MethodArgumentTypeMismatchException e) {
        log.warn("[{}] {}", e.getClass(), e.getMessage());

        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                e.getRequiredType() + " 타입으로 변환할 수 없는 요청입니다."
        );
    }

    // 필수 요청 파라미터 누락 예외 (@RequestParam required=true 파라미터 누락)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingServletRequestParameterException(
            final MissingServletRequestParameterException e
    ) {
        log.warn("[{}] {}", e.getClass(), e.getMessage());

        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                e.getParameterName() + "파라미터가 필요합니다."
        );
    }

}
