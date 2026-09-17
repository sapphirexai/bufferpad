package cn.tpl.opc.application.scan;

import cn.tpl.opc.auth.AuthPrincipal;
import cn.tpl.opc.commons.dto.result.OperationEventDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;

/** A durable operation summary. Row locks serialize independent scan and PLC callbacks. */
@Service
@lombok.extern.slf4j.Slf4j
public class ScanOperationLogService {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final ObjectMapper json = new ObjectMapper();

    public ScanOperationLogService(JdbcTemplate jdbc, PlatformTransactionManager manager) {
        this.jdbc=jdbc;
        transactions=new TransactionTemplate(manager);
        transactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactions.setTimeout(15);
    }

    public boolean begin(ScanCommand command) {
        try {
            return Boolean.TRUE.equals(transactions.execute(tx -> {
                ObjectNode detail=json.createObjectNode();
                detail.put("scanCode","PENDING").put("scanMessage","等待扫码处理");
                detail.put("plcCode","PENDING").put("plcMessage","等待业务结果");
                detail.put("readCode","SKIPPED").put("readMessage","无需回读");
                String actor="";
                var auth=SecurityContextHolder.getContext().getAuthentication();
                if (command.getScannerId()==null && auth!=null && auth.getPrincipal() instanceof AuthPrincipal p) actor=p.getUsername();
                insert(command.getOperationId(),command.getQrCode(),command.getScannerId()==null?"MANUAL_SCAN":"AUTO_SCAN",
                        command.getWorkLine(),actor,command.getScannerId(),detail);
                return true;
            }));
        } catch (DuplicateKeyException duplicate) { return false; }
    }

    public void trackTransaction(String operationId) {
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
            new org.springframework.transaction.support.TransactionSynchronization() {
                @Override public void afterCompletion(int status) {
                    if(status==STATUS_COMMITTED)return;
                    try {
                        if(status==STATUS_ROLLED_BACK) {
                            OperationEventDTO failure=OperationEventDTO.of(cn.tpl.opc.commons.dto.enums.OperationEventCode.SCAN_COUNT_FAILED,null);
                            failure.setOperationId(operationId);failure.setMessage("业务事务已回滚，计数和使用明细未保存，PLC指令未提交");accept(failure);
                        } else uncertain(operationId,"业务事务提交结果未知，请核实计数及PLC状态");
                    } catch(Exception e){log.error("Could not record transaction completion for scan {}",operationId,e);}
                }
            });
    }

    public void accept(OperationEventDTO event) {
        if (event==null || event.getOperationId()==null || event.getCode()==null) return;
        if (!(event.getCode().startsWith("SCAN_") || event.getCode().startsWith("PLC_") || event.getCode().equals("CUSHION_MAX_REACHED"))) return;
        transactions.executeWithoutResult(tx -> {
            List<Map<String,Object>> rows=lock(event.getOperationId());
            if (rows.isEmpty()) {
                // Legacy independent PLC operations have no scan entry, but still get a correlated summary.
                ObjectNode detail=json.createObjectNode();
                detail.put("scanCode","NOT_APPLICABLE").put("scanMessage","独立PLC操作");
                detail.put("plcCode","NOT_APPLICABLE").put("plcMessage","无需通知");
                detail.put("readCode","SKIPPED").put("readMessage","无需回读");
                jdbc.update("INSERT INTO scan_log(operation_id,qr_code,operation_type,status,msg,msg_type,detail_json,updated_date) VALUES(?,?,'PLC_OPERATION','PROCESSING','处理中',0,?,CURRENT_TIMESTAMP(3)) ON DUPLICATE KEY UPDATE operation_id=operation_id",
                        event.getOperationId(),safe(event.getQrCode(),255),detail.toString());
                rows=lock(event.getOperationId());
            }
            Map<String,Object> row=rows.get(0);ObjectNode detail=parse(row);
            String code=event.getCode();
            String message=safe(event.getMessage(),1500);
            if (event.getUsedCount()!=null) {
                if(code.equals("SCAN_COUNTED"))message="计数成功";
                if(code.equals("SCAN_REPEATED"))message="两小时内重复扫码，本次未增加使用次数";
                if(code.equals("CUSHION_MAX_REACHED") && event.getMaxUseCount()!=null)
                    message=event.getUsedCount()>event.getMaxUseCount()?"已超过寿命上限":"已达到寿命上限";
            }
            if(code.equals("PLC_NOTIFY_SUCCEEDED"))message="写入成功";
            if (event.getErrorCode()!=null) message += "，错误码="+event.getErrorCode();
            if (event.getTechnicalDetail()!=null && !event.getTechnicalDetail().isBlank()) message += "，原因="+safe(event.getTechnicalDetail(),500);
            if (event.getAddress()!=null) message += "，地址="+safe(event.getAddress(),128);
            if (event.getWriteValue()!=null) message += "，写入值="+event.getWriteValue();
            if (code.startsWith("SCAN_") || code.equals("CUSHION_MAX_REACHED")) {
                stage(detail,"scan",code,message);
                if (event.getUsedCount()!=null) detail.put("usedCount",event.getUsedCount());
                if (event.getMaxUseCount()!=null) detail.put("maxUseCount",event.getMaxUseCount());
                if (code.equals("SCAN_COUNT_FAILED")) {
                    stage(detail,"plc","SKIPPED","业务保存失败，未发送PLC指令");
                    stage(detail,"read","SKIPPED","未执行回读");
                }
            } else if (code.startsWith("PLC_READ_")) {
                stage(detail,"read",code,message);
            } else {
                stage(detail,"plc",code,message);
                if (detail.path("plcCode").asText().equals(code) && (code.equals("PLC_NOTIFY_PENDING") || code.equals("PLC_NOTIFY_SUCCEEDED"))
                        && Boolean.TRUE.equals(event.getReadExpected()) && detail.path("readCode").asText().equals("SKIPPED"))
                    stage(detail,"read","PENDING","等待开口数回读");
                if (!code.equals("PLC_NOTIFY_PENDING") && !code.equals("PLC_NOTIFY_SUCCEEDED"))
                    stage(detail,"read","SKIPPED","PLC通知未完成，未执行回读");
            }
            // Freeze identities at their first known value, including for manual scans resolved to a scanner.
            if (row.get("scanner_id")==null && event.getScannerId()!=null) {
                row.put("scanner_id",event.getScannerId());row.put("scanner_snapshot",device(event.getScannerId()));
            }
            if (row.get("plc_id")==null && event.getPlcId()!=null) {
                row.put("plc_id",event.getPlcId());row.put("plc_snapshot",device(event.getPlcId()));
            }
            if (row.get("work_line")==null) row.put("work_line",event.getWorkLine());
            save(row,detail);
        });
    }

    public void uncertain(String operationId,String reason) {
        transactions.executeWithoutResult(tx -> {
            List<Map<String,Object>> rows=lock(operationId);
            if (rows.isEmpty()) return;
            Map<String,Object> row=rows.get(0);ObjectNode detail=parse(row);
            detail.put("uncertain",safe(reason,500));save(row,detail);
        });
    }

    public int expirePending(Date cutoff) {
        List<String> ids=jdbc.queryForList("SELECT operation_id FROM scan_log WHERE status='PROCESSING' AND updated_date<? ORDER BY updated_date,id LIMIT 1000",String.class,cutoff);
        int expired=0;
        for(String id:ids) {
            Boolean changed=transactions.execute(tx -> {
                List<Map<String,Object>> rows=jdbc.queryForList("SELECT * FROM scan_log WHERE operation_id=? AND status='PROCESSING' AND updated_date<? FOR UPDATE",id,cutoff);
                if(rows.isEmpty())return false;
                Map<String,Object> row=rows.get(0);
                ObjectNode detail=parse(row);detail.put("uncertain","超过5分钟未完成，可能发生超时或服务中断，结果需核实");save(row,detail);return true;
            });
            if(Boolean.TRUE.equals(changed))expired++;
        }
        return expired;
    }

    private void insert(String id,String qr,String type,Integer workLine,String actor,Long scannerId,ObjectNode detail) {
        jdbc.update("INSERT INTO scan_log(operation_id,qr_code,operation_type,status,work_line,operator_name,scanner_id,scanner_snapshot,msg,msg_type,detail_json,updated_date) VALUES(?,?,?,'PROCESSING',?,?,?,?,?,0,?,CURRENT_TIMESTAMP(3))",
                id,safe(qr,255),type,workLine,safe(actor,64),scannerId,device(scannerId),"处理中",detail.toString());
        Map<String,Object> row=lock(id).get(0);save(row,detail);
    }

    private List<Map<String,Object>> lock(String id) {
        return jdbc.queryForList("SELECT * FROM scan_log WHERE operation_id=? FOR UPDATE",id);
    }
    private ObjectNode parse(Map<String,Object> row) {
        try {return (ObjectNode)json.readTree((String)row.get("detail_json"));}
        catch(Exception e){throw new IllegalStateException("Invalid scan operation summary",e);}
    }
    private void stage(ObjectNode detail,String stage,String code,String message) {
        String previous=detail.path(stage+"Code").asText();
        if ((code.equals("PENDING") || code.equals("PLC_NOTIFY_PENDING"))
                && !(previous.isEmpty() || previous.equals("PENDING") || previous.equals("SKIPPED") || previous.equals("NOT_APPLICABLE"))) return;
        // A duplicate successful callback must not hide an already recorded failure in that stage.
        if ((code.endsWith("SUCCEEDED")) && (previous.endsWith("FAILED") || previous.endsWith("REJECTED"))) return;
        detail.put(stage+"Code",code).put(stage+"Message",safe(message,2000));
    }

    private void save(Map<String,Object> row,ObjectNode detail) {
        String scan=detail.path("scanCode").asText(),plc=detail.path("plcCode").asText(),read=detail.path("readCode").asText();
        boolean pending=scan.equals("PENDING") || plc.equals("PENDING") || plc.equals("PLC_NOTIFY_PENDING") || read.equals("PENDING");
        boolean failed=scan.equals("SCAN_COUNT_FAILED") || scan.equals("SCAN_NO_READ") || plc.equals("PLC_OFFLINE") || plc.equals("PLC_WRITE_FAILED") || plc.equals("PLC_WRITE_REJECTED");
        boolean warning=scan.equals("SCAN_REPEATED") || scan.equals("CUSHION_MAX_REACHED") || plc.equals("PLC_ADDRESS_NOT_CONFIGURED") || plc.equals("PLC_TARGET_NOT_RESOLVED") || read.equals("PLC_READ_FAILED") || read.equals("PLC_READ_ADDRESS_NOT_CONFIGURED");
        String state=pending?(detail.has("uncertain")?"UNKNOWN":"PROCESSING"):(failed?"FAILED":warning?"WARNING":"SUCCESS");
        if (!pending && detail.has("uncertain")) detail.remove("uncertain");
        String type=Objects.toString(row.get("operation_type"),"");
        String source=type.equals("MANUAL_SCAN")?"手动扫码":type.equals("AUTO_SCAN")?"自动扫码":"独立PLC操作";
        String msg=source+"｜操作="+row.get("operation_id")+"｜状态="+label(state)
                +"｜产线="+nonempty(row.get("work_line"),"未确定");
        if(type.equals("MANUAL_SCAN"))msg+="｜操作用户="+nonempty(row.get("operator_name"),"未记录");
        msg+="｜扫码/计数="+detail.path("scanMessage").asText();
        if(detail.has("usedCount"))msg+="，当前次数="+detail.path("usedCount").asText();
        if(detail.has("maxUseCount"))msg+="，寿命上限="+detail.path("maxUseCount").asText();
        // QR code and device identities are stored in dedicated columns, not repeated in the summary.
        msg+="｜PLC通知="+detail.path("plcMessage").asText()+"｜开口数="+detail.path("readMessage").asText();
        if(detail.has("uncertain"))msg+="｜"+detail.path("uncertain").asText();
        String result=scan.equals("NOT_APPLICABLE")?(read.equals("SKIPPED")?plc:read):scan;
        jdbc.update("UPDATE scan_log SET msg=?,msg_type=?,status=?,result_code=?,scanner_id=?,plc_id=?,scanner_snapshot=?,plc_snapshot=?,work_line=?,detail_json=?,updated_date=CURRENT_TIMESTAMP(3) WHERE id=?",
                safe(msg,8000),(failed||warning||state.equals("UNKNOWN"))?1:0,state,result,row.get("scanner_id"),row.get("plc_id"),row.get("scanner_snapshot"),row.get("plc_snapshot"),row.get("work_line"),detail.toString(),row.get("id"));
    }

    private String device(Long id) {
        if(id==null)return "";
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT id,name,ip,port,position,install_seq,work_line FROM device_info WHERE id=?",id);
        if(rows.isEmpty())return "设备[ID="+id+"，信息缺失]";
        Map<String,Object> d=rows.get(0);String position=Objects.toString(d.get("position"),"");
        if(d.get("install_seq")!=null) {
            List<String> names=jdbc.queryForList("SELECT name FROM device_install_position WHERE id=?",String.class,d.get("install_seq"));
            if(!names.isEmpty())position=names.get(0);
        }
        return safe(nonempty(d.get("name"),"未命名")+"[ID="+id+"，IP="+nonempty(d.get("ip"),"未知")+":"+nonempty(d.get("port"),"未知")+"，位置="+nonempty(position,"未知")+"]",1024);
    }
    private static String nonempty(Object value,String fallback){String s=Objects.toString(value,"");return s.isBlank()?fallback:s;}
    private static String safe(String value,int max){if(value==null)return "";String s=value.replaceAll("[\\p{Cntrl}]+"," ");return s.length()>max?s.substring(0,max-1)+"…":s;}
    private static String label(String value){return switch(value){case "PROCESSING"->"处理中";case "SUCCESS"->"成功";case "WARNING"->"提示";case "FAILED"->"失败";default->"结果未知";};}
}
