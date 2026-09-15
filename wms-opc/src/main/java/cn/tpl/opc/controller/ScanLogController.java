package cn.tpl.opc.controller;

import cn.tpl.opc.commons.dto.ResultDTO;
import cn.tpl.opc.commons.dto.result.ScanLogDTO;
import cn.tpl.opc.commons.dto.result.PageData;
import cn.tpl.opc.commons.scheme.request.QueryScanLogScheme;
import cn.tpl.opc.service.IScanLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2024/11/8
 */
@Tag(name = "扫码日志", description = "扫码日志相关接口")
@Slf4j
@RestController
@RequestMapping("/scanLogs")
public class ScanLogController {
    @Resource
    private IScanLogService scanLogService;

    @Operation(summary = "分页查询日志，默认每页20条，最多200条")
    @GetMapping
    public ResultDTO<PageData<ScanLogDTO>> listByPage(
            @Parameter(description = "详情查看<a href=\"#model-QueryScanLogScheme\"> QueryScanLogScheme")
            QueryScanLogScheme scheme) {
        try {
            log.debug("scanLogs，scheme：{}", scheme);
            return ResultDTO.success(scanLogService.listByPage(scheme));
        } catch (Exception e) {
            return ResultDTO.exception(e);
        }
    }
}
