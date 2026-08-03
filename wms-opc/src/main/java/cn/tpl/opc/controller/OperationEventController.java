package cn.tpl.opc.controller;

import cn.tpl.opc.commons.dto.ResultDTO;
import cn.tpl.opc.commons.dto.result.OperationEventDTO;
import cn.tpl.opc.service.IOperationEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@Tag(name = "运行事件", description = "扫码、计数和PLC通知运行事件")
@RestController
@RequestMapping("/operationEvents")
public class OperationEventController {
    @Resource
    private IOperationEventService operationEventService;

    @Operation(summary = "查询最近运行事件")
    @GetMapping("/recent")
    public ResultDTO<List<OperationEventDTO>> recent(
            @RequestParam(required = false) Integer workLine,
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        return ResultDTO.success(operationEventService.listRecent(workLine, limit));
    }
}
