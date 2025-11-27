// 작성자 : 조윤상
package sys.be4man.domains.analysis.repository;


import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class JenkinsLogEmitterRegistry {

    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter addEmitter(String key) {
        SseEmitter emitter = new SseEmitter(0L);

        emitters.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>())
                .add(emitter);

        emitter.onCompletion(() -> removeEmitter(key, emitter));
        emitter.onTimeout(() -> removeEmitter(key, emitter));
        emitter.onError(e -> removeEmitter(key, emitter));

        return emitter;
    }

    public void removeEmitter(String key, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(key);
        if (list == null) return;
        list.remove(emitter);
        if (list.isEmpty()) {
            emitters.remove(key);
        }
    }

    public boolean hasEmitters(String key) {
        List<SseEmitter> list = emitters.get(key);
        return list != null && !list.isEmpty();
    }

    public void sendLog(String key, String logChunk) {
        List<SseEmitter> list = emitters.get(key);
        if (list == null) return;

        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event()
                        .name("log")
                        .data(logChunk));
            } catch (IOException e) {
                emitter.complete();
                removeEmitter(key, emitter);
            }
        }
    }

    public void sendComplete(String key, String result) {
        List<SseEmitter> list = emitters.get(key);
        if (list == null) return;

        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event()
                        .name("complete")
                        .data(result));
                emitter.complete();
            } catch (IOException e) {
                emitter.complete();
            }
        }
        emitters.remove(key);
    }
}
