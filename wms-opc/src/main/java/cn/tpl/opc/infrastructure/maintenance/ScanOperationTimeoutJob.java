package cn.tpl.opc.infrastructure.maintenance;
import cn.tpl.opc.application.scan.ScanOperationLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.Date;
@Component
@Slf4j
public class ScanOperationTimeoutJob {
    private final ScanOperationLogService logs;
    public ScanOperationTimeoutJob(ScanOperationLogService logs){this.logs=logs;}
    @Scheduled(fixedDelay=60000,initialDelay=60000)
    public void reconcile(){
        try {int count=logs.expirePending(new Date(System.currentTimeMillis()-300000));if(count>0)log.warn("Marked {} unfinished scan operations as unknown",count);}
        catch(Exception e){log.error("Failed to reconcile unfinished scan operation logs",e);}
    }
}
