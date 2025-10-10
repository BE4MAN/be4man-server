package sys.be4man.global;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
@Tag(name = "헬스 체크", description = "서버 상태 확인 API입니다.")
public class HealthController {

    @Value("${server.port}")
    private String serverPort;

    @GetMapping()
    @Operation(summary = "서버 상태 확인", description = "서버가 정상적으로 실행 중인지 확인합니다.")
    public String check() {
        return "✅ server listening on port " + serverPort;
    }
}
