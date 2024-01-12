package cn.tpl.opc.controller;

import cn.tpl.opc.commons.dto.ResultDTO;
import cn.tpl.opc.commons.dto.result.DeviceInfoDTO;
import cn.tpl.opc.commons.dto.result.PageData;
import cn.tpl.opc.commons.scheme.base.BasePageScheme;
import cn.tpl.opc.netty.ConnectionMgr;
import cn.tpl.opc.service.IDeviceInfoService;
import cn.tpl.opc.service.INettyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

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

    @Resource
    private ConnectionMgr connectionMgr;

    @Operation(summary = "分页查询设备列表，用于设备配置相关")
    @GetMapping("/devicesPage")
    public ResultDTO<PageData<DeviceInfoDTO>> devicesPage(@Parameter(description = "详情查看<a href=\"#model-BasePageScheme\"> BasePageScheme") BasePageScheme scheme) {
        try {
            log.debug("devicesPage，scheme：{}", scheme);
            return ResultDTO.success(deviceInfoService.listByPage(scheme));
        } catch (Exception e) {
            return ResultDTO.exception(e);
        }
    }

    @Operation(summary = "连接所有设备（扫码器和PLC），并返回当前所有设备状态信息")
    @GetMapping("/deviceConnections/{workLine}")
    public ResultDTO<List<DeviceInfoDTO>> deviceConnections(@Parameter(description = "生产线，为0则连接所有产线") @PathVariable Integer workLine) {
        try {
            return nettyService.connectDevices(workLine);
        } catch (Exception e) {
            return ResultDTO.exception(e);
        }
    }

    @Operation(summary = "获取当前设备状态信息")
    @GetMapping("/devicesStatus/{workLine}")
    public ResultDTO<List<DeviceInfoDTO>> devicesStatus(@Parameter(description = "生产线，为0则获取所有产线") @PathVariable Integer workLine) {
        try {
            return ResultDTO.success(nettyService.getDevicesStatus(workLine));
        } catch (Exception e) {
            return ResultDTO.exception(e);
        }
    }

    @Operation(summary = "连接所有扫码器，并返回当前所有扫码器状态信息")
    @GetMapping("/scannerConnections")
    public ResultDTO<List<DeviceInfoDTO>> scannerConnections() {
        try {
            return nettyService.connectScanners();
        } catch (Exception e) {
            return ResultDTO.exception(e);
        }
    }

    @Operation(summary = "通过设备ID删除指定设备的连接")
    @DeleteMapping("/connection/{id}")
    public ResultDTO<?> removeConnection(
            @Parameter(description = "设备ID")
            @PathVariable("id") Long id) {
        try {
            log.info("removeConnection, removing connection, id => {}", id);

            if (null == id)
                return ResultDTO.failure("设备ID不能为空！");

            DeviceInfoDTO deviceInfoDTO = deviceInfoService.findById(id);
            if (null == deviceInfoDTO) return ResultDTO.failure("无此设备！");

            String ip = deviceInfoDTO.getIp();
            Integer port = deviceInfoDTO.getPort();
            log.info("removeConnection, removing connection, ip => {}:{}", ip, port);
            connectionMgr.removeConnection(deviceInfoDTO.getIp(), deviceInfoDTO.getPort());
            log.info("removeConnection, removing connection => success");
            return ResultDTO.success();
        } catch (Exception e) {
            return ResultDTO.exception(e);
        }
    }

    @Operation(summary = "通过设备ID删除指定设备数据和对应连接")
    @DeleteMapping("{id}")
    public ResultDTO<?> deleteDevice(
            @Parameter(description = "设备ID")
            @PathVariable("id") Long id) {
        try {
            log.info("deleteDevice, deleting deviceInfo, id => {}", id);

            if (null == id)
                return ResultDTO.failure("设备ID不能为空！");

            DeviceInfoDTO deviceInfoDTO = deviceInfoService.findById(id);
            if (null == deviceInfoDTO) return ResultDTO.failure("无此设备！");

            boolean result = deviceInfoService.deleteById(id);
            if (result) {
                String ip = deviceInfoDTO.getIp();
                Integer port = deviceInfoDTO.getPort();
                log.info("deleteDevice, removing connection, ip => {}:{}", ip, port);
                connectionMgr.removeConnection(deviceInfoDTO.getIp(), deviceInfoDTO.getPort());
                log.info("deleteDevice, removing connection => success");
            }

            return result ? ResultDTO.success() : ResultDTO.failure("删除失败！");
        } catch (Exception e) {
            return ResultDTO.exception(e);
        }
    }

}