package cn.tpl.opc.controller;

import cn.tpl.opc.commons.dto.ResultDTO;
import cn.tpl.opc.commons.dto.result.DeviceInstallPositionDTO;
import cn.tpl.opc.commons.dto.result.PageData;
import cn.tpl.opc.commons.scheme.request.QueryDeviceInstallPositionPageScheme;
import cn.tpl.opc.commons.scheme.request.SaveDeviceInstallPositionScheme;
import cn.tpl.opc.service.IDeviceInstallPositionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

@Tag(name = "设备安装位置", description = "设备安装位置接口")
@Slf4j
@RestController
@RequestMapping("/deviceInstallPositions")
public class DeviceInstallPositionController {
    @Resource
    private IDeviceInstallPositionService service;

    @Operation(summary = "分页查询设备安装位置")
    @GetMapping("/page")
    public ResultDTO<PageData<DeviceInstallPositionDTO>> page(QueryDeviceInstallPositionPageScheme scheme) {
        try {
            return ResultDTO.success(service.listByPage(scheme));
        } catch (Exception e) {
            return ResultDTO.exception(e);
        }
    }

    @Operation(summary = "查询所有设备安装位置")
    @GetMapping
    public ResultDTO<List<DeviceInstallPositionDTO>> list() {
        try {
            return ResultDTO.success(service.list());
        } catch (Exception e) {
            return ResultDTO.exception(e);
        }
    }

    @Operation(summary = "查询设备安装位置详情")
    @GetMapping("/{id}")
    public ResultDTO<DeviceInstallPositionDTO> findById(@Parameter(description = "安装位置ID") @PathVariable Long id) {
        try {
            return ResultDTO.success(service.findById(id));
        } catch (Exception e) {
            return ResultDTO.exception(e);
        }
    }

    @Operation(summary = "新增或编辑设备安装位置")
    @PostMapping
    public ResultDTO<Boolean> save(@Valid @RequestBody SaveDeviceInstallPositionScheme scheme) {
        try {
            return service.save(scheme) ? ResultDTO.success(true) : ResultDTO.failure("保存失败！");
        } catch (Exception e) {
            return ResultDTO.exception(e);
        }
    }

    @Operation(summary = "删除设备安装位置")
    @DeleteMapping("/{id}")
    public ResultDTO<Boolean> delete(@Parameter(description = "安装位置ID") @PathVariable Long id) {
        try {
            return service.deleteById(id) ? ResultDTO.success(true) : ResultDTO.failure("删除失败！");
        } catch (Exception e) {
            return ResultDTO.exception(e);
        }
    }
}
