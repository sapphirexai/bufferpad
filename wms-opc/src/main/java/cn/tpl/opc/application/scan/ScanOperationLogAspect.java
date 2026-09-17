package cn.tpl.opc.application.scan;

import cn.tpl.opc.commons.dto.ResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE+100)
@Slf4j
public class ScanOperationLogAspect {
    private final ScanOperationLogService logs;
    public ScanOperationLogAspect(ScanOperationLogService logs){this.logs=logs;}

    @Around("(execution(* cn.tpl.opc.application.scan.ScanApplicationService.handleScan(..)) || execution(* cn.tpl.opc.application.scan.ScanApplicationService.handleScanCodeFailed(..))) && args(command)")
    public Object around(ProceedingJoinPoint invocation,ScanCommand command) throws Throwable {
        if(!logs.begin(command)) {
            log.warn("Duplicate scan operation ignored, operationId={}",command.getOperationId());
            return invocation.getSignature().getName().equals("handleScan")?ResultDTO.failure("该操作已提交，请查询日志，不要重复提交"):null;
        }
        try {return invocation.proceed();}
        catch(Throwable error) {
            try {logs.uncertain(command.getOperationId(),"操作异常，结果需核实（"+error.getClass().getSimpleName()+"）");}
            catch(Exception loggingError){log.error("Could not finalize failed scan operation {}",command.getOperationId(),loggingError);}
            throw error;
        }
    }
}
