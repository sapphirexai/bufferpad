package cn.tpl.opc.controller;

import cn.tpl.opc.commons.dto.ResultDTO;
import cn.tpl.opc.commons.dto.result.CushionInfoDTO;
import cn.tpl.opc.commons.dto.result.PageData;
import cn.tpl.opc.commons.scheme.request.QueryCushionInfoPageScheme;
import cn.tpl.opc.service.ICushionInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
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
@Tag(name = "缓冲垫", description = "缓冲垫相关接口")
@Slf4j
@RestController
@RequestMapping("/cushion")
public class CushionController {
    @Resource
    private ICushionInfoService cushionInfoService;

    @Operation(summary = "分页查询缓冲垫列表")
    @GetMapping("/cushionsPage")
    public ResultDTO<PageData<CushionInfoDTO>> cushionsPage(@Parameter(description = "具体查看Schemas#QueryCushionInfoPageScheme") QueryCushionInfoPageScheme scheme) {
        try {
            log.info("cushionsPage，scheme：{}", scheme);
            return ResultDTO.success(cushionInfoService.listByPage(scheme));
        } catch (Exception e) {
            return ResultDTO.exception(e);
        }
    }
}