package sys.be4man.global;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Value("${server.port}")
    private String serverPort;

    @GetMapping()
    public String check() {
        return "✅ server listening on port " + serverPort;
    }
}
