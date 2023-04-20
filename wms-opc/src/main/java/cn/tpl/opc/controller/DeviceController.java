package cn.tpl.opc.controller;

import cn.tpl.opc.commons.dto.ResultDTO;
import cn.tpl.opc.commons.dto.result.DeviceInfoDTO;
import cn.tpl.opc.commons.dto.result.PageData;
import cn.tpl.opc.commons.scheme.base.BasePageScheme;
import cn.tpl.opc.service.IDeviceInfoService;
import cn.tpl.opc.service.INettyService;
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
 * Time: 2023/4/19
 */
@Tag(name = "设备", description = "设备相关接口")
@Slf4j
@RestController
@RequestMapping("/device")
public class DeviceController {
    @Resource
    private INettyService nettyService;

    @Resource
    private IDeviceInfoService deviceInfoService;

    @Operation(summary = "分页查询设备列表")
    @GetMapping("/devicesPage")
    public ResultDTO<PageData<DeviceInfoDTO>> devicesPage(@Parameter(description = "详情查看<a href=\"#model-BasePageScheme\"> BasePageScheme") BasePageScheme scheme) {
        try {
            log.debug("devicesPage，scheme：{}", scheme);
            return ResultDTO.success(deviceInfoService.listByPage(scheme));
        } catch (Exception e) {
            return ResultDTO.exception(e);
        }
    }

}