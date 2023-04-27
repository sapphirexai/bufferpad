package cn.tpl.opc.controller;

import cn.tpl.opc.commons.dto.ResultDTO;
import cn.tpl.opc.commons.dto.result.CushionInfoDTO;
import cn.tpl.opc.commons.dto.result.PageData;
import cn.tpl.opc.commons.scheme.base.BasePageScheme;
import cn.tpl.opc.service.ICushionInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

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
    public ResultDTO<PageData<CushionInfoDTO>> cushionsPage(@Parameter(description = "详情查看<a href=\"#model-BasePageScheme\"> BasePageScheme") BasePageScheme scheme) {
        try {
            log.debug("cushionsPage，scheme：{}", scheme);
            return ResultDTO.success(cushionInfoService.listByPage(scheme));
        } catch (Exception e) {
            return ResultDTO.exception(e);
        }
    }

    @Operation(summary = "手动输入缓冲垫二维码")
    @PostMapping("/manualCushionInfo/{workLine}/{qrCode}")
    public ResultDTO<CushionInfoDTO> manualCushionInfo(
            @Parameter(description = "产线")
            @PathVariable("workLine") Integer workLine,
            @Parameter(description = "二维码")
            @PathVariable("qrCode") String qrCode) {
        try {
            log.debug("manualCushionInfo，workLine：{}，qrCode：{}", workLine, qrCode);

            if (null == workLine)
                return ResultDTO.failure("产线不能为空！");

            if (StringUtils.isEmpty(qrCode))
                return ResultDTO.failure("缓冲垫编码不能为空！");
            return cushionInfoService.onQrCodeReceived(workLine,qrCode);
        } catch (Exception e) {
            return ResultDTO.exception(e);
        }
    }
}