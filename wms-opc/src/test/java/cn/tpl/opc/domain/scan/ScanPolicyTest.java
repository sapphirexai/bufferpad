package cn.tpl.opc.domain.scan;

import cn.tpl.opc.commons.constant.Constants;
import cn.tpl.opc.entity.CushionInfoEntity;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;

public class ScanPolicyTest {
    private final ScanPolicy scanPolicy = new ScanPolicy();

    @Test
    public void repeatedWithinEffectiveInterval() {
        long now = System.currentTimeMillis();
        Date lastScanDate = new Date(now - Constants.SCANNER_EFFECTIVE_INTERVAL_MILLIS + 1);

        Assert.assertTrue(scanPolicy.isRepeatedWithinEffectiveInterval(lastScanDate, now));
    }

    @Test
    public void notRepeatedWhenIntervalReached() {
        long now = System.currentTimeMillis();
        Date lastScanDate = new Date(now - Constants.SCANNER_EFFECTIVE_INTERVAL_MILLIS);

        Assert.assertFalse(scanPolicy.isRepeatedWithinEffectiveInterval(lastScanDate, now));
    }

    @Test
    public void maxReachedWhenUsedCountEqualsMaxUseCount() {
        CushionInfoEntity entity = new CushionInfoEntity();
        entity.setMaxUseCount(500);
        entity.setUsedCount(500);

        Assert.assertTrue(scanPolicy.isMaxReached(entity));
    }

    @Test
    public void maxNotReachedWhenUsedCountLowerThanMaxUseCount() {
        CushionInfoEntity entity = new CushionInfoEntity();
        entity.setMaxUseCount(500);
        entity.setUsedCount(499);

        Assert.assertFalse(scanPolicy.isMaxReached(entity));
    }
}
