package sys.be4man.global.exception.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import sys.be4man.global.dto.response.ErrorResponse;
import sys.be4man.global.exception.BaseException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 4xx 커스텀 예외
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(
            final BaseException e,
            final HttpServletRequest request
    ) {
        ErrorResponse errorResponse = ErrorResponse.from(
                e.getExceptionType().getName(),
                e.getHttpStatus().value(),
                e.getExceptionType().getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(e.getHttpStatus())
                .body(errorResponse);
    }

    /**
     * 5xx 서버 에러. 예외처리되지 않은 케이스
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            final Exception e,
            final HttpServletRequest request
    ) {
        log.error("[{}] : {}", e.getClass(), e.getMessage());
        e.printStackTrace();

        ErrorResponse errorResponse = ErrorResponse.from(
                "INTERNAL_SERVER_ERROR",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "서버 내부 오류가 발생했습니다",
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }

    /**
     * 400 클라이언트 예외
     */
    // 요청 검증 실패 예외 (@Valid 어노테이션 실패. @RequestBody)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            final MethodArgumentNotValidException e,
            final HttpServletRequest request
    ) {
        final List<FieldError> errors = e.getBindingResult().getFieldErrors();
        log.warn("[{}] {}", e.getClass(), errors);

        String errorMessage = errors.stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ErrorResponse errorResponse = ErrorResponse.from(
                "VALIDATION_FAILED",
                HttpStatus.BAD_REQUEST.value(),
                errorMessage,
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    // 요청 파라미터 검증 실패 예외 (@Validated 어노테이션 실패. @RequestParam, @PathVariable)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> constraintViolationException(
            final ConstraintViolationException e,
            final HttpServletRequest request
    ) {
        final Set<ConstraintViolation<?>> errors = e.getConstraintViolations();
        log.warn("[{}] {}", e.getClass(), errors);

        String errorMessage = errors.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));

        ErrorResponse errorResponse = ErrorResponse.from(
                "CONSTRAINT_VIOLATION",
                HttpStatus.BAD_REQUEST.value(),
                errorMessage,
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    // 요청 파라미터 타입 변환 실패 (String을 Integer로 변환 실패 등)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            final MethodArgumentTypeMismatchException e,
            final HttpServletRequest request
    ) {
        log.warn("[{}] {}", e.getClass(), e.getMessage());

        ErrorResponse errorResponse = ErrorResponse.from(
                "TYPE_MISMATCH",
                HttpStatus.BAD_REQUEST.value(),
                e.getRequiredType() + " 타입으로 변환할 수 없는 요청입니다.",
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    // 필수 요청 파라미터 누락 예외 (@RequestParam required=true 파라미터 누락)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
            final MissingServletRequestParameterException e,
            final HttpServletRequest request
    ) {
        log.warn("[{}] {}", e.getClass(), e.getMessage());

        ErrorResponse errorResponse = ErrorResponse.from(
                "MISSING_PARAMETER",
                HttpStatus.BAD_REQUEST.value(),
                e.getParameterName() + " 파라미터가 필요합니다.",
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

}
