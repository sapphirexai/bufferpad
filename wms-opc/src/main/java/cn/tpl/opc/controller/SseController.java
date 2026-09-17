package cn.tpl.opc.controller;

import cn.tpl.opc.service.ISseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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

    @Operation(summary = "订阅当前所有设备状态信息，当设备状态发生改变时，会收到对应设备的消息")
    @GetMapping(value = "/devicesStatus/{clientId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter devicesStatus(@Parameter(description = "客户端Id，与产线号一致") @PathVariable("clientId") String clientId) {
        try {
            log.info("sse设备状态订阅，clientId：{}", clientId);
            return sseService.subscribeDevicesStatus(clientId);
        } catch (Exception e) {
            log.error("devicesStatus, subscribe SSE failed, clientId => {}", clientId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "SSE subscribe failed", e);
        }
    }
}
