package cn.tpl.opc.controller;

import cn.tpl.opc.commons.dto.ResultDTO;
import cn.tpl.opc.commons.dto.result.OpcConfigDTO;
import cn.tpl.opc.commons.dto.result.PageData;
import cn.tpl.opc.commons.scheme.base.BasePageScheme;
import cn.tpl.opc.commons.scheme.request.SaveOpcConfigScheme;
import cn.tpl.opc.service.IOpcConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

@Tag(name = "OPC全局配置", description = "OPC全局配置接口")
@Slf4j
@RestController
@RequestMapping("/opcConfig")
public class OpcConfigController {
    @Resource
    private IOpcConfigService opcConfigService;

    @Operation(summary = "获取缓冲垫寿命配置")
    @GetMapping
    public ResultDTO<OpcConfigDTO> getConfig() {
        try {
            return ResultDTO.success(opcConfigService.getOrInit());
        } catch (Exception e) {
            return ResultDTO.exception(e);
        }
    }

    @Operation(summary = "分页查询缓冲垫寿命配置")
    @GetMapping("/page")
    public ResultDTO<PageData<OpcConfigDTO>> page(BasePageScheme scheme) {
        try {
            return ResultDTO.success(opcConfigService.listByPage(scheme));
        } catch (Exception e) {
            return ResultDTO.exception(e);
        }
    }

    @Operation(summary = "保存缓冲垫寿命配置，固定ID为1")
    @PostMapping
    public ResultDTO<Boolean> save(@Valid @RequestBody SaveOpcConfigScheme scheme) {
        try {
            boolean result = opcConfigService.save(scheme);
            return result ? ResultDTO.success(true) : ResultDTO.failure("全局配置ID只能为1！");
        } catch (Exception e) {
            return ResultDTO.exception(e);
        }
    }

    @Operation(summary = "重置缓冲垫寿命配置为默认值500")
    @DeleteMapping("/{id}")
    public ResultDTO<Boolean> resetDefault(@Parameter(description = "固定ID，只允许为1") @PathVariable Long id) {
        try {
            boolean result = opcConfigService.resetDefault(id);
            return result ? ResultDTO.success(true) : ResultDTO.failure("全局配置ID只能为1！");
        } catch (Exception e) {
            return ResultDTO.exception(e);
        }
    }
}
