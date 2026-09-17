package cn.tpl.opc.controller;

import cn.tpl.opc.commons.dto.ResultDTO;
import cn.tpl.opc.commons.dto.result.PLCAddrDTO;
import cn.tpl.opc.commons.dto.result.PageData;
import cn.tpl.opc.commons.scheme.request.QueryPLCAddrPageScheme;
import cn.tpl.opc.commons.scheme.request.SavePLCAddrScheme;
import cn.tpl.opc.service.IPLCAddrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

@Tag(name = "PLC地址", description = "PLC地址配置接口")
@Slf4j
@RestController
@RequestMapping("/plcAddr")
public class PLCAddrController {
    @Resource
    private IPLCAddrService service;

    @Operation(summary = "分页查询PLC地址配置")
    @GetMapping("/page")
    public ResultDTO<PageData<PLCAddrDTO>> page(QueryPLCAddrPageScheme scheme) {
        try {
            return ResultDTO.success(service.listByPage(scheme));
        } catch (Exception e) {
            return ResultDTO.exception(e);
        }
    }

    @Operation(summary = "查询PLC地址配置详情")
    @GetMapping("/{id}")
    public ResultDTO<PLCAddrDTO> findById(@Parameter(description = "PLC地址ID") @PathVariable Long id) {
        try {
            return ResultDTO.success(service.findById(id));
        } catch (Exception e) {
            return ResultDTO.exception(e);
        }
    }

    @Operation(summary = "新增或编辑PLC地址配置")
    @PostMapping
    public ResultDTO<Boolean> save(@Valid @RequestBody SavePLCAddrScheme scheme) {
        try {
            return service.save(scheme) ? ResultDTO.success(true) : ResultDTO.failure("保存失败！");
        } catch (Exception e) {
            return ResultDTO.exception(e);
        }
    }

    @Operation(summary = "删除PLC地址配置")
    @DeleteMapping("/{id}")
    public ResultDTO<Boolean> delete(@Parameter(description = "PLC地址ID") @PathVariable Long id) {
        try {
            return service.deleteById(id) ? ResultDTO.success(true) : ResultDTO.failure("删除失败！");
        } catch (Exception e) {
            return ResultDTO.exception(e);
        }
    }
}
