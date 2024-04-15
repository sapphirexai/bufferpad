package cn.tpl.opc.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.tpl.opc.commons.constant.Params;
import cn.tpl.opc.commons.dto.ResultDTO;
import cn.tpl.opc.commons.dto.result.CushionDetailDTO;
import cn.tpl.opc.commons.dto.result.CushionInfoDTO;
import cn.tpl.opc.commons.dto.result.ExportCushionInfoDTO;
import cn.tpl.opc.commons.dto.result.PageData;
import cn.tpl.opc.commons.scheme.request.ModifyCushionInfoScheme;
import cn.tpl.opc.commons.scheme.request.QueryCushionDetailPageScheme;
import cn.tpl.opc.commons.scheme.request.QueryCushionInfoPageScheme;
import cn.tpl.opc.service.ICushionInfoService;
import cn.tpl.opc.util.EasyExcelUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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
    public ResultDTO<PageData<CushionInfoDTO>> cushionsPage(
            @Parameter(description = "详情查看<a href=\"#model-QueryCushionInfoPageScheme\"> QueryCushionInfoPageScheme")
            QueryCushionInfoPageScheme scheme) {
        try {
            log.debug("cushionsPage，scheme：{}", scheme);
            return ResultDTO.success(cushionInfoService.listByPage(scheme));
        } catch (Exception e) {
            return ResultDTO.exception(e);
        }
    }

    @Operation(summary = "通过二维码模糊查询缓冲垫列表")
    @GetMapping("/cushions/{qrCode}")
    public ResultDTO<List<CushionInfoDTO>> listByQrCode(@Parameter(description = "二维码")
                                                        @PathVariable String qrCode) {
        try {
            log.info("listByQrCode，qrCode：{}", qrCode);

            if (StringUtils.isEmpty(qrCode))
                return ResultDTO.failure("缓冲垫编码不能为空！");

            return ResultDTO.success(cushionInfoService.listByQrCode(qrCode));
        } catch (Exception e) {
            return ResultDTO.exception(e);
        }
    }

    @Operation(summary = "通过二维码查询对应缓冲垫明细")
    @GetMapping("/detailsPage")
    public ResultDTO<PageData<CushionDetailDTO>> listDetailsPageByQrCode(
            @Parameter(description = "详情查看<a href=\"#model-QueryCushionDetailPageScheme\"> QueryCushionDetailPageScheme")
            QueryCushionDetailPageScheme scheme) {
        try {
            log.debug("detailsPage，scheme：{}", scheme);
            return ResultDTO.success(cushionInfoService.listDetailsByPage(scheme));
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
            log.info("manualCushionInfo, workLine：{}, qrCode: {}", workLine, qrCode);

            if (null == workLine)
                return ResultDTO.failure("产线不能为空！");

            if (StringUtils.isEmpty(qrCode))
                return ResultDTO.failure("缓冲垫编码不能为空！");
            return cushionInfoService.onQrCodeReceived(workLine, null, qrCode);
        } catch (Exception e) {
            return ResultDTO.exception(e);
        }
    }

    @Operation(summary = "批量修改缓冲垫信息")
    @PostMapping("/cushions")
    public ResultDTO<?> modifyCushionInfo(
            @Parameter(description = "修改缓冲垫信息协议")
            @RequestBody ModifyCushionInfoScheme scheme) {
        try {
            log.info("modifyCushionInfo, scheme => {}", scheme);
            Integer maxUseCount = scheme.getMaxUseCount();
            if (null == maxUseCount)
                return ResultDTO.failure("最大使用次数不能为空!");

            if (0 > maxUseCount)
                return ResultDTO.failure("最大使用次数不能为负!");

            List<Long> ids = scheme.getIds();
            if (CollectionUtils.isEmpty(ids))
                return ResultDTO.failure("需修改的缓冲垫ID不能为空!");

            boolean result = cushionInfoService.modifyMaxUseCountByIds(scheme);
            return result ? ResultDTO.success() : ResultDTO.failure(null);
        } catch (Exception e) {
            return ResultDTO.exception(e);
        }
    }

    @Operation(summary = "批量导出缓冲垫信息")
    @PostMapping("/cushions/excel")
    public ResultDTO<Boolean> exportCushions(
            @Parameter(hidden = true) HttpServletResponse response,
            @Parameter(description = "需要导出的缓冲垫ID列表")
            @RequestBody List<Long> ids) {
        try {
            log.info("exportCushions, ids => {}", ids);
            if (CollectionUtils.isEmpty(ids))
                return ResultDTO.failure("需导出的缓冲垫ID不能为空!");

            List<CushionInfoDTO> cushionInfos = cushionInfoService.listByIds(ids);
            if (CollectionUtils.isEmpty(cushionInfos)) return ResultDTO.failure("无对应数据!");

            List<ExportCushionInfoDTO> exportDatas = new ArrayList<>();
            for (CushionInfoDTO data : cushionInfos) {
                ExportCushionInfoDTO exportData = new ExportCushionInfoDTO();
                BeanUtil.copyProperties(data, exportData);
                Integer scannerSeq = data.getScannerSeq();
                String position = "";
                if (null != scannerSeq) {
                    if (Params.SCANNER_SEQ_KEY_1 == data.getScannerSeq())
                        position = Params.SCANNER_SEQ_VAL_1;
                    if (Params.SCANNER_SEQ_KEY_2 == data.getScannerSeq())
                        position = Params.SCANNER_SEQ_VAL_2;
                }
                exportData.setCreatedDate(DateUtil.formatDateTime(data.getCreatedDate()));
                exportData.setLastScanDate(DateUtil.formatDateTime(data.getLastScanDate()));
                exportData.setPosition(position);

                exportDatas.add(exportData);
            }
            String fileType = ".xls";
            String fileName = "Cushion_" + DateUtil.now() + fileType;
            response.setContentType(fileType + "; charset=UTF-8");
            response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
            response.setHeader("Content-Disposition", "attachment;filename=" + new String(fileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1));
            EasyExcelUtils.export(response.getOutputStream(), exportDatas, ExportCushionInfoDTO.class);
            return ResultDTO.success();
        } catch (Exception e) {
            return ResultDTO.exception(e);
        }
    }
}
