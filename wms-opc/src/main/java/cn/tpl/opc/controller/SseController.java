package cn.tpl.opc.controller;

import cn.tpl.opc.commons.dto.ResultDTO;
import cn.tpl.opc.service.ISseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/4/21
 */
@Tag(name = "服务器消息", description = "服务器消息订阅接口")
@Slf4j
@RestController
@RequestMapping("/sse")
public class SseController {
    @Resource
    private ISseService sseService;

    @Operation(summary = "订阅当前所有设备状态信息")
    @GetMapping("/devicesStatus/{clientId}")
    public ResultDTO<Boolean> devicesStatus(@PathVariable("clientId") String clientId) {
        try {
            log.info("sse设备状态订阅，clientId：{}", clientId);
            return ResultDTO.success(sseService.subscribeDevicesStatus(clientId));
        } catch (Exception e) {
            return ResultDTO.exception(e);
        }
    }
}
