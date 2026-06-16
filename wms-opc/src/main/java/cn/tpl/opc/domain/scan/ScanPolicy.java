package cn.tpl.opc.domain.scan;

import cn.tpl.opc.commons.constant.Constants;
import cn.tpl.opc.entity.CushionInfoEntity;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class ScanPolicy {
    public boolean isManualScan(Long scannerId) {
        return scannerId == null;
    }

    public boolean isRepeatedWithinEffectiveInterval(Date lastScanDate, long currentTimeMillis) {
        if (lastScanDate == null) return false;
        return currentTimeMillis - lastScanDate.getTime() < Constants.SCANNER_EFFECTIVE_INTERVAL_MILLIS;
    }

    public boolean isMaxReached(CushionInfoEntity cushionInfoEntity) {
        if (cushionInfoEntity == null || cushionInfoEntity.getMaxUseCount() == null || cushionInfoEntity.getUsedCount() == null) {
            return false;
        }
        return cushionInfoEntity.getMaxUseCount() <= cushionInfoEntity.getUsedCount();
    }
}
