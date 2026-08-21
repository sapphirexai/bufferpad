package cn.tpl.opc.controller;

import cn.tpl.opc.commons.dto.ResultDTO;
import cn.tpl.opc.commons.dto.enums.DeviceTypeEnum;
import cn.tpl.opc.commons.dto.enums.PlcAddrTypeEnum;
import cn.tpl.opc.commons.dto.result.DeviceInfoDTO;
import cn.tpl.opc.commons.dto.result.DeviceOptionDTO;
import cn.tpl.opc.commons.dto.result.OptionDTO;
import cn.tpl.opc.entity.DeviceInfoEntity;
import cn.tpl.opc.service.IDeviceInstallPositionService;
import cn.tpl.opc.service.IDeviceInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "下拉选项", description = "通用下拉选项接口")
@RestController
@RequestMapping("/options")
public class OptionController {
    @Resource
    private IDeviceInfoService deviceInfoService;
    @Resource
    private IDeviceInstallPositionService installPositionService;

    @Operation(summary = "设备类型选项")
    @GetMapping("/deviceTypes")
    public ResultDTO<List<OptionDTO<Integer>>> deviceTypes() {
        return ResultDTO.success(DeviceTypeEnum.options());
    }

    @Operation(summary = "PLC地址类型选项")
    @GetMapping("/plcAddrTypes")
    public ResultDTO<List<OptionDTO<Integer>>> plcAddrTypes() {
        return ResultDTO.success(PlcAddrTypeEnum.options());
    }

    @Operation(summary = "设备安装位置选项")
    @GetMapping("/deviceInstallPositions")
    public ResultDTO<List<OptionDTO<Long>>> installPositions() {
        return ResultDTO.success(installPositionService.list().stream()
                .map(item -> new OptionDTO<>(item.getId(), item.getName()))
                .collect(Collectors.toList()));
    }

    @Operation(summary = "PLC设备选项")
    @GetMapping("/plcDevices")
    public ResultDTO<List<DeviceOptionDTO>> plcDevices() {
        return ResultDTO.success(deviceInfoService.list().stream()
                .filter(item -> DeviceTypeEnum.isPlc(item.getType()))
                .map(item -> {
                    DeviceTypeEnum type = DeviceTypeEnum.of(item.getType());
                    String typeLabel = type == null ? "PLC" : type.getLabel();
                    return new DeviceOptionDTO(item.getId(), item.getName() + " - " + typeLabel, item.getType());
                })
                .collect(Collectors.toList()));
    }

    @Operation(summary = "扫码器设备选项")
    @GetMapping("/scannerDevices")
    public ResultDTO<List<OptionDTO<Long>>> scannerDevices() {
        return ResultDTO.success(deviceInfoService.list().stream()
                .filter(item -> DeviceTypeEnum.isScanner(item.getType()))
                .map(item -> {
                    String positionName = installPositionService.getNameById(item.getInstallSeq());
                    String label = item.getName() + (positionName == null ? "" : " - " + positionName);
                    return new OptionDTO<>(item.getId(), label);
                })
                .collect(Collectors.toList()));
    }
}
