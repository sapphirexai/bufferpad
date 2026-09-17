package cn.tpl.opc.infrastructure.maintenance;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ScanLogRetentionService {
    private final DataSource dataSource;
    public ScanLogRetentionService(DataSource dataSource) { this.dataSource = dataSource; }
    public record CleanupResult(long deleted, int batches, String outcome) {}

    public CleanupResult cleanup(LocalDateTime cutoff, int batchSize, int maxSeconds, int pauseMillis) {
        if (cutoff == null || batchSize < 1 || batchSize > 10000 || maxSeconds < 1 || pauseMillis < 0)
            throw new IllegalArgumentException("Invalid scan log retention parameters");
        long started = System.nanoTime(), deleted = 0;
        int batches = 0;
        try (Connection c = dataSource.getConnection()) {
            boolean auto = c.getAutoCommit(), locked = false;
            Integer oldLockWait = null;
            try {
                c.setAutoCommit(true);
                try (Statement s = c.createStatement()) {
                    s.setQueryTimeout(10);
                    try (ResultSet rs = s.executeQuery("SELECT GET_LOCK(CONCAT('bp.scanLog.cleanup:',MD5(DATABASE())),0)")) {
                        locked = rs.next() && rs.getInt(1) == 1;
                    }
                    if (!locked) return new CleanupResult(0, 0, "SKIPPED_LOCKED");
                    try (ResultSet rs = s.executeQuery("SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='scan_log' AND seq_in_index=1 AND column_name='created_date' LIMIT 1")) {
                        if (!rs.next()) {
                            log.error("scan_log cleanup skipped: install the created_date index migration first");
                            return new CleanupResult(0, 0, "SKIPPED_MISSING_INDEX");
                        }
                    }
                    try (ResultSet rs = s.executeQuery("SELECT @@SESSION.innodb_lock_wait_timeout")) { rs.next(); oldLockWait = rs.getInt(1); }
                    s.execute("SET SESSION innodb_lock_wait_timeout=5");
                }
                try (PreparedStatement s = c.prepareStatement("DELETE FROM scan_log WHERE created_date < ? ORDER BY created_date,id LIMIT ?")) {
                    s.setQueryTimeout(10);
                    // DATETIME stores local wall time; use the configured zone's literal cutoff.
                    s.setString(1, cutoff.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    s.setInt(2, batchSize);
                    while (TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - started) < maxSeconds) {
                        if (Thread.currentThread().isInterrupted()) return new CleanupResult(deleted, batches, "INTERRUPTED");
                        int count = s.executeUpdate();
                        deleted += count; batches++;
                        if (count < batchSize) return new CleanupResult(deleted, batches, "COMPLETE");
                        if (pauseMillis > 0) {
                            try { Thread.sleep(pauseMillis); }
                            catch (InterruptedException e) { Thread.currentThread().interrupt(); return new CleanupResult(deleted, batches, "INTERRUPTED"); }
                        }
                    }
                    return new CleanupResult(deleted, batches, "TIME_BUDGET_REACHED");
                }
            } finally {
                try (Statement s = c.createStatement()) {
                    s.setQueryTimeout(10);
                    try {
                        if (oldLockWait != null) s.execute("SET SESSION innodb_lock_wait_timeout=" + oldLockWait);
                    } finally {
                        if (locked) {
                            // SELECT is supported by the application's Druid WallFilter; DO is not.
                            try (ResultSet rs=s.executeQuery("SELECT RELEASE_LOCK(CONCAT('bp.scanLog.cleanup:',MD5(DATABASE())))")) {
                                if (!rs.next() || rs.getInt(1)!=1) throw new SQLException("Could not release scan log cleanup lock");
                            }
                        }
                    }
                } finally { c.setAutoCommit(auto); }
            }
        } catch (SQLException e) {
            log.error("scan_log cleanup failed, cutoff={}, committedDeleted={}, batches={}", cutoff, deleted, batches, e);
            throw new IllegalStateException("扫描日志清理失败", e);
        }
    }
}
