package sys.be4man.domains.analysis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import sys.be4man.domains.analysis.service.JenkinsConsoleStreamingService;

@RestController
@RequestMapping("/api/jenkins")
@RequiredArgsConstructor
public class JenkinsLogStreamController {

    private final JenkinsConsoleStreamingService streamingService;

    @GetMapping(value = "/log-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam Long deploymentId) {
        return streamingService.subscribe(deploymentId);
    }
}
