// 작성자 : 조윤상
package sys.be4man.domains.analysis.repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class JenkinsLogCache {

    public static class LogBuffer {
        private final StringBuilder content = new StringBuilder();
        private int lastOffset = 0;
        private boolean completed = false;

        public synchronized void append(String chunk, int newOffset) {
            content.append(chunk);
            lastOffset = newOffset;
        }

        public synchronized String getContent() {
            return content.toString();
        }

        public synchronized int getLastOffset() {
            return lastOffset;
        }

        public synchronized void markCompleted() {
            completed = true;
        }

        public synchronized boolean isCompleted() {
            return completed;
        }
    }

    private final Map<String, LogBuffer> cache = new ConcurrentHashMap<>();

    public LogBuffer getOrCreate(String key) {
        return cache.computeIfAbsent(key, k -> new LogBuffer());
    }

    public LogBuffer get(String key) {
        return cache.get(key);
    }

    public void remove(String key) {
        cache.remove(key);
    }
}
