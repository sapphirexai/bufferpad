package cn.tpl.opc.auth;

import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

/** Bounded, expiring in-process protection; keyed by both account and source address. */
@Component
public class LoginAttempts {
    private final Map<String, long[]> attempts = new HashMap<>();
    private static final long WINDOW = 5 * 60 * 1000L;

    public synchronized void check(String username, String address) {
        cleanup();
        checkKey("user:" + username, 5);
        checkKey("ip:" + address, 20);
    }

    public synchronized void failed(String username, String address) {
        cleanup();
        add("user:" + username);
        add("ip:" + address);
    }

    public synchronized void succeeded(String username) { attempts.remove("user:" + username); }

    private void checkKey(String key, int limit) {
        long[] entry = attempts.get(key);
        if (entry != null && entry[0] >= limit) throw new AuthException(429, "登录失败次数过多，请5分钟后重试");
    }

    private void add(String key) {
        if (attempts.size() >= 10000 && !attempts.containsKey(key)) {
            throw new AuthException(429, "登录请求过多，请稍后重试");
        }
        long[] entry = attempts.computeIfAbsent(key, ignored -> new long[]{0, System.currentTimeMillis()});
        entry[0]++;
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        attempts.values().removeIf(entry -> now - entry[1] >= WINDOW);
    }
}
